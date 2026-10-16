package com.xiaomanjun.sleepdownschedule.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.feature.importing.IcsScheduleCodec
import com.xiaomanjun.sleepdownschedule.feature.settings.*
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidDropdownPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun TimetableFilesPanel(state: AppState, backdrop: Backdrop?) {
    val context = LocalContext.current
    val app = context.applicationContext as CourseScheduleApp
    val scope = rememberCoroutineScope()
    var selectedId by remember { mutableIntStateOf(state.config.id) }
    val profiles = state.schedules
    val selected = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
    var pendingText by remember { mutableStateOf<String?>(null) }
    var pendingIcsReport by remember { mutableStateOf<String?>(null) }
    var jsonPreview by remember { mutableStateOf<DecodedBackupArchive?>(null) }
    var icsPreview by remember { mutableStateOf<ImportDraft?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun task(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true; message = null
            try { block() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { message = error.message ?: "文件操作未完成，请重试" }
            finally { busy = false }
        }
    }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingText; pendingText = null
        if (uri != null && text != null) task { withContext(Dispatchers.IO) {
            (context.contentResolver.openOutputStream(uri, "wt") ?: error("无法打开保存位置")).use { it.write(text.toByteArray()) }
        }; message = "JSON 已保存：只包含所选课表，不含图片、凭据或助手会话" }
    }
    val saveIcs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        val text = pendingText; pendingText = null
        val report = pendingIcsReport; pendingIcsReport = null
        if (uri != null && text != null) task { withContext(Dispatchers.IO) {
            (context.contentResolver.openOutputStream(uri, "wt") ?: error("无法打开保存位置")).use { it.write(text.toByteArray()) }
        }; message = "ICS 已保存。${report.orEmpty()}；日历文件不代替完整备份。" }
    }
    suspend fun read(uri: android.net.Uri): ByteArray = withContext(Dispatchers.IO) {
        (context.contentResolver.openInputStream(uri) ?: error("无法读取文件")).use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (output.size() <= 12 * 1024 * 1024) {
                val count = input.read(buffer); if (count < 0) break; output.write(buffer, 0, count)
            }
            require(output.size() <= 12 * 1024 * 1024) { "文件超过 12 MB" }
            output.toByteArray()
        }
    }
    val openJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) task { jsonPreview = withContext(Dispatchers.IO) { TimetableJsonCodec.decode(read(uri).toString(Charsets.UTF_8)) } }
    }
    val openIcs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) task { icsPreview = withContext(Dispatchers.IO) { IcsScheduleCodec.parse(read(uri), state.config).getOrThrow() } }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassPreferenceSection("导入") {
            SettingsGroup(backdrop, state.config) {
                SettingsNavigationRow("JSON 课表", "检查课程、作息、调休后新增课表", onClick = { if (!busy) openJson.launch(arrayOf("*/*")) })
                SettingsDivider()
                SettingsNavigationRow("ICS 日历", "本机解析定时事件与日 / 周重复规则", onClick = { if (!busy) openIcs.launch(arrayOf("*/*")) })
            }
        }
        GlassPreferenceSection("导出所选课表") {
            SettingsGroup(backdrop, state.config) {
                SleepDownLiquidDropdownPreference(items = profiles.map { it.name },
                    selectedIndex = profiles.indexOf(selected).coerceAtLeast(0), title = "课表",
                    backdrop = backdrop, config = state.config, insideMargin = PaddingValues(14.dp),
                    maxHeight = 300.dp, onExpandedChange = {}, onSelectedIndexChange = { profiles.getOrNull(it)?.let { p -> selectedId = p.id } })
                SettingsDivider()
                SettingsNavigationRow("导出 JSON", "保留课程、节次、多套作息与调休；不含图片", onClick = {
                    selected?.let { profile -> task {
                        pendingText = TimetableJsonService(app).export(profile.id)
                        saveJson.launch("${profile.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")}.json")
                    } }
                })
                SettingsDivider()
                SettingsNavigationRow("导出 ICS", "按实际日期导出，采用课表作息与调休", onClick = {
                    selected?.let { profile -> task {
                        val snapshot = withContext(Dispatchers.IO) { app.repository.scheduleSnapshot(profile.id) }
                        val report = withContext(Dispatchers.IO) { IcsScheduleCodec.exportReport(profile.name, snapshot.config, snapshot.periods, snapshot.courses) }
                        require(report.events > 0) { "没有可导出的定时课程；跳过 ${report.skipped} 个缺少有效时间的安排" }
                        pendingText = report.content
                        message = "将导出 ${report.events} 个事件，跳过 ${report.skipped} 个缺少时间的安排"
                        pendingIcsReport = "${report.events} 个事件，跳过 ${report.skipped} 个缺少时间的安排"
                        saveIcs.launch("${profile.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")}.ics")
                    } }
                })
            }
        }
        if (busy) Text("正在处理文件…", style = MaterialTheme.typography.bodySmall)
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
    jsonPreview?.let { archive ->
        val schedule = archive.data.schedules.single()
        val duplicate = profiles.any { it.name == schedule.name }
        LiquidAlertDialog("导入预览 · ${schedule.name}",
            "${schedule.courses.size} 门课程 · ${schedule.periods.size} 节 · ${schedule.periodSchemes.size} 套作息\n" +
                "${if (duplicate) "已有同名课表，将另存一张。" else "将新增课表，原课表保留。"}\n" + schedule.courses.take(8).joinToString("\n") { it.name },
            listOf(LiquidAlertAction("取消", LiquidAlertActionStyle.Secondary) { jsonPreview = null },
                LiquidAlertAction("新增课表", LiquidAlertActionStyle.Primary) { task {
                    TimetableJsonService(app).append(archive); jsonPreview = null; message = "已新增课表，可到课表管理中切换"
                } }), backdrop, state.config, { if (!busy) jsonPreview = null })
    }
    icsPreview?.let { draft ->
        LiquidAlertDialog("ICS 导入预览", "${draft.courses.size} 门课程 · ${draft.periods.size} 个时间段\n" +
            "将另存课表；ICS 不含完整作息方案及调休编辑记录。\n" + draft.warnings.joinToString("\n") + "\n" + draft.courses.take(8).joinToString("\n") { it.name },
            listOf(LiquidAlertAction("取消", LiquidAlertActionStyle.Secondary) { icsPreview = null },
                LiquidAlertAction("新增课表", LiquidAlertActionStyle.Primary) { task {
                    app.repository.importDraft(draft, createNewSchedule = true); icsPreview = null
                    com.xiaomanjun.sleepdownschedule.feature.reminder.NotificationScheduler.requestReschedule(app)
                    message = "日历已导入新课表"
                } }), backdrop, state.config, { if (!busy) icsPreview = null })
    }
}
