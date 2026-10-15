package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.scvtc.campus.AppExperiencePreferences
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.AppState
import com.xiaomanjun.sleepdownschedule.app.ui.*
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.glass.*
import top.yukonga.miuix.kmp.basic.TabRow
import java.util.Locale

@Composable
internal fun GlassAppearanceEditor(state: AppState, backdrop: Backdrop?) {
    val context = LocalContext.current
    val store = remember(context) { AppExperiencePreferences.get(context) }
    var entry by rememberSaveable { mutableStateOf(store.state.value.glass.encode()) }
    var encoded by rememberSaveable { mutableStateOf(entry) }
    val draft = remember(encoded) { GlassAppearance.decode(encoded) }
    var exitPrompt by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(draft) { store.previewGlass(draft) }
    DisposableEffect(store) { onDispose { store.previewGlass(null) } }
    BackHandler(enabled = encoded != entry) { exitPrompt = true }
    fun update(parameters: GlassParameters) { encoded = draft.update(parameters).encode(); notice = null }
    fun apply() { store.saveGlass(draft); entry = encoded; notice = "已应用，三套参数分别保存" }
    LazyColumn(Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = detailContentTopPadding() + 16.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            LiquidGlassDockPreview(state.config, com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme(state.config))
        }
        item {
            Text("实时预览", style = MaterialTheme.typography.titleMedium)
            Text("点预览底栏切换页面。这里与实际底栏、成绩控件和弹层共用材质；课程卡片保持原样。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            TabRow(GlassStyle.entries.map { it.title.removeSuffix("玻璃") }, draft.style.ordinal,
                { encoded = draft.copy(style = GlassStyle.entries[it], customized = true).encode() })
        }
        if (draft.style == GlassStyle.SOFT) item {
            Text("参考澎湃 OS 的柔光风格，使用本机公开渲染能力，无需系统模块。", style = MaterialTheme.typography.bodySmall)
        }
        item {
            SettingsGroup(backdrop, state.config) {
                GlassValue("模糊半径", draft.current.blur, 0f..64f, "dp", backdrop) { update(draft.current.copy(blur = it)) }
                GlassValue("材质不透明度", draft.current.opacity, 0f..1f, "%", backdrop) { update(draft.current.copy(opacity = it)) }
                GlassValue("着色", draft.current.tint, 0f..1f, "%", backdrop) { update(draft.current.copy(tint = it)) }
                GlassValue("圆角", draft.current.corner, 12f..40f, "dp", backdrop) { update(draft.current.copy(corner = it)) }
                GlassValue("边缘高光", draft.current.highlight, 0f..1f, "%", backdrop) { update(draft.current.copy(highlight = it)) }
                GlassValue("描边宽度", draft.current.stroke, 0f..2f, "dp", backdrop) { update(draft.current.copy(stroke = it)) }
                GlassValue("阴影半径", draft.current.shadow, 0f..24f, "dp", backdrop) { update(draft.current.copy(shadow = it)) }
            }
        }
        item {
            SettingsGroup(backdrop, state.config) {
                when (draft.style) {
                    GlassStyle.LIQUID -> {
                        GlassValue("折射高度", draft.current.lensHeight, 0f..32f, "dp", backdrop) { update(draft.current.copy(lensHeight = it)) }
                        GlassValue("折射强度", draft.current.lensAmount, 0f..48f, "dp", backdrop) { update(draft.current.copy(lensAmount = it)) }
                        SettingsToggleRow("色散", "低折射或关闭色散可减少光学处理", draft.current.dispersion, backdrop) { update(draft.current.copy(dispersion = it)) }
                        GlassValue("按压形变", draft.current.press, 0f..1f, "%", backdrop) { update(draft.current.copy(press = it)) }
                        GlassValue("回弹响应", draft.current.response, .5f..1.5f, "×", backdrop) { update(draft.current.copy(response = it)) }
                    }
                    GlassStyle.GAUSSIAN -> SettingsInfoRow("背景模糊", "关闭透镜和色散，保留切页、拖动与选中动效。")
                    GlassStyle.SOFT -> {
                        GlassValue("纹理强度", draft.current.noise, 0f..0.04f, "%", backdrop) { update(draft.current.copy(noise = it)) }
                        GlassValue("亮度", draft.current.brightness, -0.15f..0.15f, "%", backdrop) { update(draft.current.copy(brightness = it)) }
                        GlassValue("对比度", draft.current.contrast, .6f..1.4f, "×", backdrop) { update(draft.current.copy(contrast = it)) }
                        GlassValue("饱和度", draft.current.saturation, 0f..1.5f, "×", backdrop) { update(draft.current.copy(saturation = it)) }
                        GlassValue("柔和混色", draft.current.blend, 0f..0.6f, "%", backdrop) { update(draft.current.copy(blend = it)) }
                    }
                }
            }
        }
        item {
            SettingsGroup(backdrop, state.config) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsActionButton("恢复当前样式默认值", backdrop, onClick = { update(GlassParameters.defaults(draft.style)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsActionButton("取消预览", backdrop, onClick = { encoded = entry; notice = "已恢复进入时的设置" }, modifier = Modifier.weight(1f))
                        SettingsActionButton("应用", backdrop, onClick = ::apply, modifier = Modifier.weight(1f))
                    }
                    notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text("退出页面不会自动保存预览；只有应用后才写入。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (exitPrompt) LiquidAlertDialog("保留玻璃调整？", "尚未应用的参数仅用于预览。", listOf(
        LiquidAlertAction("应用", LiquidAlertActionStyle.Primary) { apply(); exitPrompt = false },
        LiquidAlertAction("舍弃预览", LiquidAlertActionStyle.Destructive) { encoded = entry; exitPrompt = false },
        LiquidAlertAction("继续调整", LiquidAlertActionStyle.Secondary) { exitPrompt = false }
    ), backdrop, state.config, { exitPrompt = false })
}

@Composable
private fun GlassValue(title: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String,
    backdrop: Backdrop?, change: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(String.format(Locale.ROOT, if (unit == "%") "%.1f %%" else "%.1f %s",
                *if (unit == "%") arrayOf<Any>(value * 100f) else arrayOf<Any>(value, unit)), style = MaterialTheme.typography.bodySmall)
        }
        LiquidControlSlider(value, change, range, backdrop = backdrop, onLiveValueChange = change, modifier = Modifier.fillMaxWidth())
    }
}
