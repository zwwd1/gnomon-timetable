package cn.scvtc.campus

import android.content.Context
import androidx.room.withTransaction
import cn.scvtc.campus.core.Meeting
import cn.scvtc.campus.core.ScheduleWriteGuard
import cn.scvtc.campus.core.TeachingCalendar
import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import com.xiaomanjun.sleepdownschedule.TodayCoursesWidgetProvider
import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.feature.schedule.autorefresh.*
import com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

internal fun scvtcKey(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }

internal fun Meeting.nativeCourse(scheduleId: Int) = CourseEntity(
    name = name, teacher = teacher.ifBlank { null }, location = room.ifBlank { null },
    weekday = day, periods = (startNode..endNode).toList(), weeks = weeks.distinct().sorted(),
    weekParity = WeekParity.ALL, note = null, scheduleId = scheduleId
)

/** CAS only authenticates. All imported course data comes from verified JSON APIs. */
internal object ScvtcNativeBridge {
    internal val coordinator = Mutex()
    private var foregroundAttempt = 0L
    @Volatile var interactiveLogin = false
    private var loginJob: Deferred<JwxtStudent>? = null
    private var foregroundJob: Job? = null
    private var sessionReadJob: Job? = null
    private var initialSyncJob: Job? = null
    val loginState = kotlinx.coroutines.flow.MutableStateFlow(NativeLoginState())
    @Synchronized
    fun signIn(app: CourseScheduleApp, account: String, password: String): Deferred<JwxtStudent> {
        loginJob?.takeIf { !it.isCompleted }?.let { return it }
        require(account.matches(Regex("[0-9]{6,20}")) && password.length in 1..512) { "请输入学号和密码" }
        foregroundJob?.cancel()
        initialSyncJob?.cancel()
        sessionReadJob?.cancel()
        loginState.value = NativeLoginState(account,busy=true,message="正在连接学校并核验学生身份…")
        val job = app.applicationScope.async(start = CoroutineStart.LAZY) {
            try {
                val student = coordinator.withLock {
                    val memory = OfficialLoginMemory(app)
                    memory.rememberInput(account,password)
                    val api = JwxtApi(headers = { memory.apiHeaders(account,it) })
                    CasAuthManager(app,api).signIn(account,password) { message ->
                        loginState.value = NativeLoginState(account,busy=true,message=message)
                    }
                }
                loginState.value = NativeLoginState(account,verified=true,message="学校身份已核验，正在后台读取课表")
                student
            } catch(e: CancellationException) {
                loginState.value = NativeLoginState(account,message="已停止本次登录，原数据保留"); throw e
            } catch(e: Exception) {
                val auth=e as? JwxtAuthenticationRequired
                val extra = auth?.stage in setOf(AuthenticationStage.CHALLENGE,AuthenticationStage.NOTICE,AuthenticationStage.FORM,AuthenticationStage.CALLBACK,AuthenticationStage.SESSION)
                loginState.value = NativeLoginState(account,requiresVerification=extra,
                    message=when(auth?.stage) {
                        AuthenticationStage.CHALLENGE -> "学校显示了验证码或二次认证，请补充认证；已填信息保留"
                        AuthenticationStage.NOTICE -> "学校有账号安全提示，请在官方页面确认后继续"
                        AuthenticationStage.REJECTED -> "学校未接受账号或密码，请检查输入后重试"
                        AuthenticationStage.FORM -> "学校登录表单未准备好，请重试；也可打开学校认证页继续"
                        AuthenticationStage.CALLBACK -> "学校认证回调等待超时，请重试；原数据保留"
                        AuthenticationStage.SESSION -> "学校会话尚未建立，可补充认证；原数据保留"
                        null -> "这次登录未完成，请检查网络或稍后重试；原数据保留"
                    })
                throw e
            }
        }
        loginJob = job
        job.invokeOnCompletion { cause ->
            synchronized(this) { if(loginJob===job) loginJob=null }
            if(cause==null) startInitialSync(app,account)
        }
        job.start()
        return job
    }
    /** Finish cancelling the hidden browser before opening an interactive one. */
    suspend fun cancelSignIn() {
        val tasks = synchronized(this) { listOfNotNull(loginJob,foregroundJob,initialSyncJob,sessionReadJob).distinct() }
        tasks.forEach { it.cancel() }; tasks.forEach { it.join() }
    }
    fun completeOfficialSignIn(app:CourseScheduleApp,student:JwxtStudent) {
        loginState.value = NativeLoginState(student.account,verified=true,message="学校身份已核验，课表正在后台同步")
        startInitialSync(app,student.account)
    }
    @Synchronized
    private fun startInitialSync(app:CourseScheduleApp,account:String) {
        if(loginJob?.isActive==true)return
        initialSyncJob?.cancel()
        val job=app.applicationScope.launch(start=CoroutineStart.LAZY) {
            val result=sync(app,account,recover=false)
            if(result.success)AcademicRepository.requestRefresh(app,account)
        }
        initialSyncJob=job
        job.invokeOnCompletion{synchronized(this){if(initialSyncJob===job)initialSyncJob=null}}
        job.start()
    }
    const val SCHOOL = "scvtc"
    fun profileKey(account: String, term: String) = "profile:" + scvtcKey("$account|$term")
    fun fingerprint(course: CourseEntity) = ScvtcCourseWriter.fingerprint(course)

    /** Persist the verified account before returning to the caller. A long first
     * timetable read may be interrupted without losing the connected identity. */
    internal suspend fun rememberVerifiedProfile(app:CourseScheduleApp,account:String,calendar:Pair<cn.scvtc.campus.core.OfficialTerm,List<Int>>) {
        ScvtcLegacyMigration.run(app)
        val previous=AutoRefreshScheduleStore.load(app)?.takeIf { it.schoolId==SCHOOL && it.username==account }
        val term=calendar.first
        val currentConfig=app.repository.activeSnapshot().config
        val today=TeachingCalendar.today()
        val weeks=calendar.second.max()
        val teachingWeek=TeachingCalendar.weekAt(term.firstMonday,today).coerceIn(1,weeks)
        val id=app.database.withTransaction {
            val ledger=app.database.scvtcStateDao()
            val key=profileKey(account,term.semester)
            val existing=ledger.get(key)?.toIntOrNull()?.takeIf { candidate ->
                app.database.scheduleProfileDao().getProfiles().any { it.id==candidate }
            }
            val profileId=existing ?: app.database.scheduleProfileDao().upsertProfile(ScheduleProfileEntity(
                name="川职 ${term.semester} · ${account.takeLast(4)}")).toInt().also { ledger.put(ScvtcState(key,it.toString())) }
            if(app.database.configDao().getConfig(profileId)==null) {
                app.database.configDao().upsertConfig(currentConfig.copy(id=profileId,
                    totalWeeks=weeks,currentWeek=teachingWeek,termStartDate=term.firstMonday.toString(),autoCurrentWeek=true,
                    termState=if(today<term.firstMonday)ScheduleTermState.UPCOMING else ScheduleTermState.ACTIVE))
            }
            app.database.scheduleProfileDao().activateProfile(profileId)
            profileId
        }
        val profile=AutoRefreshScheduleProfile(SCHOOL,"四川职业技术学院","scvtc-native-v1",
            "官方教务接口",account,"",id,automatic=previous?.automatic?:true,
            frequencyMinutes=previous?.frequencyMinutes?:AutoRefreshFrequency.DailyMinutes,
            avatarPath=previous?.avatarPath,lastRefreshAt=previous?.takeIf { it.scheduleId==id }?.lastRefreshAt?:0,
            lastResult="学校身份已核验，正在读取课表")
        AutoRefreshScheduleStore.save(app,profile)
        AutoRefreshScheduleWorker.updateSchedule(app,profile)
    }

    internal suspend fun <T> readSession(block:suspend ()->T):T=coordinator.withLock {
        val owner=currentCoroutineContext().job
        synchronized(this) {
            if(loginJob?.isActive==true)throw CancellationException("主动登录正在进行")
            sessionReadJob=owner
        }
        try { block() } finally { synchronized(this) { if(sessionReadJob===owner)sessionReadJob=null } }
    }

    fun foreground(app: CourseScheduleApp) {
        if (interactiveLogin) return
        val now = System.currentTimeMillis()
        if (now - foregroundAttempt < 5 * 60_000) return
        foregroundAttempt = now
        foregroundJob=app.applicationScope.launch {
            val migration = runCatching { ScvtcLegacyMigration.run(app) }
            if (migration.isFailure) return@launch // Never synchronize over an incomplete migration.
            val profile = AutoRefreshScheduleStore.load(app)
            val memory = OfficialLoginMemory(app)
            val account = profile?.takeIf { it.schoolId == SCHOOL }?.username
                ?: memory.lastVerifiedAccount().ifBlank { memory.savedAccounts().singleOrNull().orEmpty() }.takeIf { it.isNotBlank() }
            if (account != null && profile?.automatic != false) sync(app, account)
        }
    }

    suspend fun sync(context: Context, expectedAccount: String, recover: Boolean = true): AutoRefreshOutcome = readSession {
        val app = context.applicationContext as CourseScheduleApp
        val memory = OfficialLoginMemory(app)
        val old = AutoRefreshScheduleStore.load(app)
        CampusSyncStatus.begin(app,expectedAccount,previousSuccess=old?.lastRefreshAt?:0)
        try {
            ScvtcLegacyMigration.run(app)
            CampusSyncStatus.phase(SyncStage.AUTHENTICATING,"正在认证学校账号…")
            memory.restoreSession(expectedAccount)
            val api = JwxtApi(headers = { memory.apiHeaders(expectedAccount, it) })
            var recovered = false
            val session = JwxtSessionManager(api) { account ->
                if (!recover || account.isBlank()) throw JwxtAuthenticationRequired()
                CasAuthManager(app, api).restoreJwxt(account)
                recovered = true
            }
            val (student, calendar, fetched) = session.withSession(expectedAccount) { student ->
                memory.confirmed(student.account)
                CampusSyncStatus.phase(SyncStage.READING,"正在读取真实课表…")
                val calendar = api.calendar()
                Triple(student, calendar, api.schedule(student.account, calendar.first.semester, calendar.second))
            }
            val account = student.account
            val term = calendar.first
            require(fetched.extraction.account == account && fetched.extraction.semester == term.semester)
            ScheduleWriteGuard.validate(null, fetched.extraction)
            val db = app.database
            CampusSyncStatus.phase(SyncStage.WRITING,"正在保存课表…")
            val scheduleId = db.withTransaction {
                val ledger = db.scvtcStateDao()
                val key = profileKey(account, term.semester)
                val existingId = ledger.get(key)?.toIntOrNull()
                val validId = existingId?.takeIf { id -> db.scheduleProfileDao().getProfiles().any { it.id == id } }
                val id = validId ?: db.scheduleProfileDao().upsertProfile(ScheduleProfileEntity(
                    name = "川职 ${term.semester} · ${account.takeLast(4)}"
                )).toInt().also { ledger.put(ScvtcState(key, it.toString())) }
                val oldConfig = db.configDao().getConfig(id)
                val count = calendar.second.max()
                val today = TeachingCalendar.today()
                val current = TeachingCalendar.weekAt(term.firstMonday, today).coerceIn(1, count)
                val config = oldConfig ?: ScheduleConfigEntity(id, count, current, 10,
                    termStartDate = term.firstMonday.toString(), autoCurrentWeek = true,
                    termState = if (today < term.firstMonday) ScheduleTermState.UPCOMING else ScheduleTermState.ACTIVE)
                db.configDao().upsertConfig(config.copy(totalWeeks = maxOf(config.totalWeeks, count)))
                val incoming = fetched.extraction.meetings.map { it.nativeCourse(id) }
                val maxNode = incoming.flatMap { it.periods }.maxOrNull() ?: 12
                val periods = db.configDao().getPeriods(id).associateBy { it.periodIndex }
                db.configDao().upsertPeriods((1..maxOf(maxNode, periods.keys.maxOrNull() ?: 0)).map { node ->
                    periods[node] ?: PeriodEntity(node, "", "", id) // No invented school bell times.
                })
                ScvtcCourseWriter.write(object : SchoolCourseStore {
                    override suspend fun courses(scheduleId: Int) = db.courseDao().getCourses(scheduleId)
                    override suspend fun ownership(scheduleId: Int) = ledger.get("courses:$scheduleId")
                    override suspend fun put(course: CourseEntity) = db.courseDao().insertCourse(course)
                    override suspend fun delete(id: Long) = db.courseDao().deleteCourse(id)
                    override suspend fun ownership(scheduleId: Int, value: String) = ledger.put(ScvtcState("courses:$scheduleId", value))
                }, id, fetched.extraction)
                if (old == null || old.username != account || old.scheduleId != id) db.scheduleProfileDao().activateProfile(id)
                id
            }
            memory.confirmed(account)
            val updated = AutoRefreshScheduleProfile(SCHOOL, "四川职业技术学院", "scvtc-native-v1",
                "官方教务接口", account, "", scheduleId, automatic = old?.automatic ?: true,
                frequencyMinutes = old?.frequencyMinutes ?: AutoRefreshFrequency.DailyMinutes,
                avatarPath = old?.takeIf { it.username == account }?.avatarPath,
                lastRefreshAt = System.currentTimeMillis(),
                lastResult = (if (recovered) "会话已自动恢复；" else "") + "已验证学生身份并同步 ${fetched.extraction.meetings.size} 项课程安排；本地编辑保留")
            AutoRefreshScheduleStore.save(app, updated)
            CampusSyncStatus.success(app,updated.lastRefreshAt)
            AutoRefreshScheduleWorker.updateSchedule(app, updated)
            val snapshot = app.repository.activeSnapshot()
            NotificationScheduler.refreshToday(app, snapshot.courses, snapshot.config, snapshot.periods)
            TodayCoursesWidgetProvider.refreshAll(app)
            AutoRefreshOutcome(true, updated.lastResult, updated)
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) {
                CampusSyncStatus.phase(SyncStage.CACHE,"已停止 · 使用本机缓存")
                throw e
            }
            CampusSyncStatus.failure(app)
            failure(app, "登录恢复超时；已保存的课表仍可使用", retryable = true)
        } catch (e: Exception) {
            CampusSyncStatus.failure(app,e is JwxtAuthenticationRequired)
            val message = if (e is JwxtAuthenticationRequired) "学校需要补充认证，请打开官方登录；离线课表已保留"
                else "同步未完成，离线课表已保留。请检查网络或稍后重试"
            failure(app, message, retryable = e is java.io.IOException)
        }
    }

    private fun failure(app: Context, message: String, retryable: Boolean = false): AutoRefreshOutcome {
        AutoRefreshScheduleStore.update(app) { it.copy(lastResult = message) }
        return AutoRefreshOutcome(false, message, AutoRefreshScheduleStore.load(app), retryable)
    }

    suspend fun disconnect(context: Context) {
        synchronized(this) { loginJob?.cancel() }
        coordinator.withLock {
        val memory=OfficialLoginMemory(context)
        val account=AutoRefreshScheduleStore.load(context)?.username.orEmpty().ifBlank { memory.lastVerifiedAccount() }
        if(account.isNotBlank())memory.forget(account)
        AutoRefreshScheduleWorker.updateSchedule(context, null)
        AutoRefreshScheduleStore.clear(context)
        withContext(Dispatchers.Main) {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            android.webkit.WebStorage.getInstance().deleteAllData()
        }
        }
    }

}
