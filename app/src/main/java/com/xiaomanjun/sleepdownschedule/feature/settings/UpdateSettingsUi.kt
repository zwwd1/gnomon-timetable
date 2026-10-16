package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    var dialog by remember { mutableStateOf<SettingsUpdateDialog?>(null) }
    var downloaded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val installPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        downloaded?.let { path ->
            if (GiteeAppUpdater.canRequestPackageInstalls(context)) runCatching {
                GiteeAppUpdater.launchInstaller(context, java.io.File(path))
            }.onSuccess { dialog = null }.onFailure { dialog = SettingsUpdateDialog.Error("无法启动安装，请重试") }
        }
    }
    fun check() {
        if (busy) return
        scope.launch {
            busy = true; message = null; dialog = SettingsUpdateDialog.Checking
            try {
                GiteeAppUpdater.checkForUpdate(context, BuildConfig.VERSION_NAME).fold(onSuccess = { result ->
                    GiteeAppUpdater.recordCheckResult(context, result)
                    latest = when (result) { is GiteeUpdateCheckResult.UpdateAvailable -> result.release; is GiteeUpdateCheckResult.UpToDate -> result.release }
                    message = if (result is GiteeUpdateCheckResult.UpdateAvailable) "发现新版本 ${latest?.tagName}" else "当前已是最新版本"
                    dialog = when (result) {
                        is GiteeUpdateCheckResult.UpdateAvailable -> if (result.release.apkUrl.isNullOrBlank()) SettingsUpdateDialog.NoApk(result.release) else SettingsUpdateDialog.Available(result.release)
                        is GiteeUpdateCheckResult.UpToDate -> SettingsUpdateDialog.UpToDate(result.release.tagName)
                    }
                }, onFailure = { dialog = SettingsUpdateDialog.Error("检查失败，请检查网络后重试") })
            } finally { busy = false }
        }
    }
    fun download(release: GiteeReleaseInfo) {
        if (busy) return
        scope.launch {
            busy = true; dialog = SettingsUpdateDialog.Downloading(release)
            try {
                GiteeAppUpdater.downloadApk(context, release).fold(onSuccess = { file ->
                    downloaded = file.absolutePath
                    if (GiteeAppUpdater.canRequestPackageInstalls(context)) {
                        runCatching { GiteeAppUpdater.launchInstaller(context, file) }
                            .onSuccess { dialog = null }.onFailure { dialog = SettingsUpdateDialog.Error("无法启动安装，请重试") }
                    } else dialog = SettingsUpdateDialog.InstallPermissionRequired
                }, onFailure = { dialog = SettingsUpdateDialog.Error("下载未完成，请检查网络后重试") })
            } finally { busy = false }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, detailContentTopPadding(), 16.dp, DockScrollPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { GlassPreferenceSection("版本") { SettingsGroup(backdrop, state.config) {
            SettingsInfoRow("当前版本", "Gnomon 课程表 ${BuildConfig.VERSION_NAME}")
            SettingsDivider()
            SettingsNavigationRow(if (busy) "正在处理…" else "检查更新", "应用内检查、下载，再由系统确认安装", onClick = ::check)
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
            SettingsNavigationRow("下载此版本", "在应用内下载 APK", onClick = { download(release) })
            SettingsDivider()
            SettingsNavigationRow("查看发布说明", "在应用内浏览 GitHub 版本记录", onClick = { context.openInAppProjectPage(release.releaseUrl) })
        } } }
    }
    SettingsUpdateDialogHost(dialog, backdrop, state.config, onDismiss = { dialog = null }, onRetry = ::check,
        onDownload = ::download, onOpenRelease = { context.openInAppProjectPage(it.releaseUrl) },
        onOpenBackup = { context.openInAppProjectPage("https://github.com/zwwd1/gnomon-timetable/releases") },
        onRequestInstallPermission = { installPermission.launch(GiteeAppUpdater.unknownSourcesSettingsIntent(context)) })
}
