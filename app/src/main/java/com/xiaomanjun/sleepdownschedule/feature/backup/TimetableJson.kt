package com.xiaomanjun.sleepdownschedule.feature.backup

import androidx.room.withTransaction
import com.xiaomanjun.sleepdownschedule.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TimetableJsonDocument(val format: String = "Gnomon Timetable", val version: Int = 1,
    val metadata: BackupSourceMetadata, val schedule: BackupSchedule)

/** Human-readable single-timetable interchange, validated through the existing backup codec. */
object TimetableJsonCodec {
    private val json = Json { encodeDefaults = true; prettyPrint = true; ignoreUnknownKeys = true }
    fun encode(document: TimetableJsonDocument): String {
        validate(document)
        return json.encodeToString(document)
    }
    fun decode(text: String): DecodedBackupArchive {
        require(text.toByteArray().size <= 12 * 1024 * 1024) { "JSON 文件超过 12 MB" }
        return validate(json.decodeFromString<TimetableJsonDocument>(text))
    }
    private fun validate(document: TimetableJsonDocument): DecodedBackupArchive {
        require(document.format == "Gnomon Timetable" && document.version == 1) { "不支持的课表 JSON 格式或版本" }
        val s = document.schedule
        require(s.periods.all { it.periodIndex > 0 } && s.courses.all { course ->
            course.periods.isNotEmpty() && course.periods.all { it > 0 } && course.weeks.isNotEmpty() && course.weeks.all { it in 1..s.config.totalWeeks }
        }) { "课表包含无效节次或上课周数" }
        val times = s.periods.map { com.xiaomanjun.sleepdownschedule.model.PeriodSchemeTimeEntity(0, it.periodIndex, it.startTime, it.endTime) }
        com.xiaomanjun.sleepdownschedule.domain.schedule.validateEditablePeriodTimes(times)?.let { throw IllegalArgumentException(it) }
        s.periodSchemes.forEach { scheme ->
            com.xiaomanjun.sleepdownschedule.domain.schedule.validateEditablePeriodTimes(scheme.times.map {
                com.xiaomanjun.sleepdownschedule.model.PeriodSchemeTimeEntity(0, it.periodIndex, it.startTime, it.endTime)
            })?.let { throw IllegalArgumentException(it) }
        }
        require(s.agentDailySessions.isEmpty() && s.agentMessages.isEmpty() && s.config.wallpaperAssetId == null) {
            "单课表 JSON 不包含助手会话或图片，请使用完整备份"
        }
        val archive = BackupArchive(document.metadata,
            BackupData(BackupFormatV1.DATA_VERSION, listOf(s)),
            BackupPreferences(BackupFormatV1.PREFERENCES_VERSION))
        val bytes = java.io.ByteArrayOutputStream().also { BackupCodec.write(it, archive) }.toByteArray()
        return BackupCodec.decode(bytes)
    }
}

class TimetableJsonService(private val app: CourseScheduleApp) {
    suspend fun export(scheduleId: Int): String = withContext(Dispatchers.IO) {
        BackupExportService(app, app.database).exportTimetable(scheduleId)
    }

    /** Append only. Old timetables, preferences and authentication are never replaced. */
    suspend fun append(archive: DecodedBackupArchive): Int = withContext(Dispatchers.IO) {
        require(archive.data.schedules.size == 1 && archive.assets.isEmpty()) { "请选择单课表 JSON" }
        val db = app.database
        db.withTransaction {
            val target = BackupRestoreService(app, db).readTargetSnapshot()
            val plan = BackupImportPlanBuilder.build(archive, newBackupRestoreOperationId(), target)
            val rows = BackupRoomRestoreMapper.map(archive, plan)
            db.scheduleProfileDao().upsertProfiles(rows.schedules.map { it.copy(isActive = target.scheduleIds.isEmpty()) })
            db.configDao().upsertConfigs(rows.configs)
            db.configDao().upsertPeriods(rows.periods)
            db.courseDao().insertCourses(rows.courses)
            db.periodSchemeDao().upsertSchemes(rows.periodSchemes)
            db.periodSchemeDao().upsertTimes(rows.periodSchemeTimes)
            rows.schedules.single().id
        }.also { com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler.requestReschedule(app) }
    }
}
