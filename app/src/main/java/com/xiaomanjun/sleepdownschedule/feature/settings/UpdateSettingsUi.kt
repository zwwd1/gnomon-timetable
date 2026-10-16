package com.xiaomanjun.sleepdownschedule.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.app.ui.*
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.feature.update.*
import kotlinx.coroutines.launch

@Composable
internal fun UpdateSettingsScreen(state: AppState, backdrop: Backdrop?, update: (ScheduleConfigEntity) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var latest by remember { mutableStateOf<GiteeReleaseInfo?>(null) }
    var automatic by remember { mutableStateOf(state.config.autoCheckUpdates) }
    var reminder by remember { mutableStateOf(GiteeAppUpdater.remindersEnabled(context)) }
    var beta by remember { mutableStateOf(GiteeAppUpdater.includesBeta(context)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, detailContentTopPadding(), 16.dp, DockScrollPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { GlassPreferenceSection("版本") { SettingsGroup(backdrop, state.config) {
            SettingsInfoRow("当前版本", "Gnomon 课程表 ${BuildConfig.VERSION_NAME}")
            SettingsDivider()
            SettingsNavigationRow(if (busy) "正在检查…" else "检查更新", "GitHub · zwwd1/gnomon-timetable", onClick = {
                if (!busy) scope.launch {
                    busy = true; message = null
                    try {
                        GiteeAppUpdater.checkForUpdate(context, BuildConfig.VERSION_NAME).fold(onSuccess = { result ->
                            GiteeAppUpdater.recordCheckResult(context, result)
                            latest = when (result) { is GiteeUpdateCheckResult.UpdateAvailable -> result.release; is GiteeUpdateCheckResult.UpToDate -> result.release }
                            message = if (result is GiteeUpdateCheckResult.UpdateAvailable) "发现新版本 ${latest?.tagName}" else "当前已是最新版本"
                        }, onFailure = { message = "检查失败，请检查网络后重试" })
                    } finally { busy = false }
                }
            })
        } } }
        item { GlassPreferenceSection("检查与提醒") { SettingsGroup(backdrop, state.config) {
            SettingsToggleRow("自动检查更新", "每天首次打开时读取本项目发布记录", automatic, backdrop) {
                automatic = it; update(state.config.copy(autoCheckUpdates = it))
            }
            SettingsDivider()
            SettingsToggleRow("更新提醒", "自动检查发现新版时弹出提醒；不会自动安装", reminder, backdrop, enabled = automatic) {
                reminder = it; GiteeAppUpdater.setRemindersEnabled(context, it)
            }
            SettingsDivider()
            SettingsToggleRow("接收 Beta 版更新", "关闭时仅检查正式版本", beta, backdrop) {
                beta = it; GiteeAppUpdater.setIncludesBeta(context, it)
            }
        } } }
        message?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
        latest?.let { release -> item { SettingsGroup(backdrop, state.config) {
            Text(release.notes, Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            SettingsNavigationRow("查看发布与下载", "在 GitHub 查看此版本的说明和安装包", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.releaseUrl)))
            })
        } } }
    }
}
