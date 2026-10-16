package com.xiaomanjun.sleepdownschedule.feature.backup

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.feature.settings.*
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField

@Composable
internal fun BackupStoragePanel(state: AppState, backdrop: Backdrop?, onOpenPreview: (Uri) -> Unit) {
    val app = LocalContext.current.applicationContext as CourseScheduleApp
    val scope = rememberCoroutineScope()
    val history = remember(app) { LocalBackupHistory(app) }
    val store = remember(app) { WebDavConfigStore(app) }
    var local by remember { mutableStateOf(history.list()) }
    var remote by remember { mutableStateOf<List<WebDavEntry>>(emptyList()) }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    fun task(label: String, block: suspend () -> String) {
        if (busy != null) return
        scope.launch {
            busy = label; notice = null
            try { notice = block() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { notice = if (error is java.net.SocketTimeoutException) "连接超时，请检查网络与 WebDAV 地址" else error.message ?: "备份操作未完成" }
            finally { busy = null; local = withContext(Dispatchers.IO) { history.list() } }
        }
    }
    LaunchedEffect(store) {
        try {
            withContext(Dispatchers.IO) { store.load() }?.let { url = it.url; username = it.username; password = it.password }
        } catch (error: Exception) { notice = error.message }
    }
    fun client(): WebDavBackupClient {
        require(username.isNotBlank() && password.isNotBlank()) { "请填写 WebDAV 账号与应用密码" }
        return WebDavBackupClient(WebDavConfig(url, username, password))
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassPreferenceSection("本地备份历史") {
            SettingsGroup(backdrop, state.config) {
                SettingsNavigationRow("新建完整备份", "全部课表、设置与图片 · 仅保存在本机", onClick = { task("正在保存本地备份…") {
                    history.create(); "本地备份已完成"
                } })
                local.forEach { file ->
                    SettingsDivider()
                    SettingsNavigationRow(file.name.removePrefix("Gnomon-").substringBeforeLast('-').take(24),
                        "%.2f MB · 点此预览恢复".format(file.length() / 1048576.0), onClick = { if (busy == null) onOpenPreview(Uri.fromFile(file)) })
                    TextButton(onClick = { if (busy == null) deleteTarget = file }, modifier = Modifier.padding(horizontal = 14.dp)) { Text("删除这份备份", color = MaterialTheme.colorScheme.error) }
                }
                if (local.isEmpty()) Text("还没有本地备份", Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        GlassPreferenceSection("WebDAV 手动备份") {
            SettingsGroup(backdrop, state.config) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("使用自己的 HTTPS WebDAV 目录。连接信息加密留在本机，不进入备份。上传内容与完整本地备份相同。", style = MaterialTheme.typography.bodySmall)
                    MiuixTextField(url, { url = it; remote = emptyList() }, label = "WebDAV 目录地址 · HTTPS",
                        singleLine = true, enabled = busy == null, modifier = Modifier.fillMaxWidth())
                    MiuixTextField(username, { username = it }, label = "账号", singleLine = true,
                        enabled = busy == null, modifier = Modifier.fillMaxWidth())
                    MiuixTextField(password, { password = it }, label = "应用密码", singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "隐藏" else "显示") } },
                        enabled = busy == null, modifier = Modifier.fillMaxWidth())
                    SettingsActionButton("加密保存连接", backdrop, onClick = { task("正在保存连接…") {
                        withContext(Dispatchers.IO) { store.save(WebDavConfig(url, username, password)) }; "连接信息已加密保存"
                    } })
                    SettingsActionButton("测试连接 / 刷新历史", backdrop, onClick = { task("正在连接 WebDAV…") {
                        val entries = withContext(Dispatchers.IO) { client().run { prepare(); list() } }
                        remote = entries
                        "连接成功，找到 ${entries.size} 份 Gnomon 备份"
                    } })
                    SettingsActionButton("上传一份完整备份", backdrop, onClick = { task("正在备份并上传…") {
                        val dav = client()
                        val file = history.create()
                        val name = withContext(Dispatchers.IO) { dav.prepare(); dav.upload(file) }
                        try {
                            remote = withContext(Dispatchers.IO) { dav.list() }
                            "上传完成：$name。本机同时保留一份。"
                        } catch (error: java.io.IOException) {
                            "备份已上传，本机也已保存；远端列表刷新失败，请手动刷新历史。"
                        }
                    } })
                    SettingsActionButton("移除本机连接信息", backdrop, onClick = { task("正在移除连接…") {
                        withContext(Dispatchers.IO) { store.clear() }; password = ""; username = ""; url = ""; remote = emptyList()
                        "本机连接已移除，远端备份未改动"
                    } })
                    Text("每次上传独立保存，不覆盖旧历史。恢复前将下载、校验并展示范围，确认后才替换当前数据。", style = MaterialTheme.typography.bodySmall)
                }
                remote.forEach { entry ->
                    SettingsDivider()
                    SettingsNavigationRow(entry.name.removePrefix("Gnomon-").take(24),
                        "%.2f MB · ${entry.modified} · 下载并预览".format(entry.bytes / 1048576.0), onClick = {
                            task("正在下载并校验备份…") {
                                val file = withContext(Dispatchers.IO) {
                                    val target = File.createTempFile("webdav-", ".sleepdown", app.cacheDir)
                                    client().download(entry.name, target); target
                                }
                                onOpenPreview(Uri.fromFile(file)); "已校验，等待确认恢复"
                            }
                        })
                }
            }
        }
        busy?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
    deleteTarget?.let { target -> LiquidAlertDialog("删除这份本地备份？", target.name,
        listOf(LiquidAlertAction("取消", LiquidAlertActionStyle.Secondary) { deleteTarget = null },
            LiquidAlertAction("删除", LiquidAlertActionStyle.Destructive) {
                deleteTarget = null; task("正在删除…") { withContext(Dispatchers.IO) { history.delete(target) }; "已删除所选备份" }
            }), backdrop, state.config, { deleteTarget = null }) }
}
