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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

@Composable
internal fun GlassAppearanceEditor(state: AppState, backdrop: Backdrop?) {
    val context = LocalContext.current
    val store = remember(context) { AppExperiencePreferences.get(context) }
    val app = context.applicationContext as com.xiaomanjun.sleepdownschedule.CourseScheduleApp
    val scope = rememberCoroutineScope()
    val configSaver = remember(state.config.id) { appearanceConfigSaver(state.config) }
    val presentationSaver = remember(store) { appearancePresentationSaver(store.state.value) }
    var configEntry by rememberSaveable(state.config.id, stateSaver = configSaver) { mutableStateOf(state.config) }
    var configDraft by rememberSaveable(state.config.id, stateSaver = configSaver) { mutableStateOf(configEntry) }
    var presentationEntry by rememberSaveable(stateSaver = presentationSaver) { mutableStateOf(store.state.value) }
    var presentationDraft by rememberSaveable(stateSaver = presentationSaver) { mutableStateOf(presentationEntry) }
    var group by rememberSaveable { mutableIntStateOf(1) }
    var saving by remember { mutableStateOf(false) }
    var entry by rememberSaveable { mutableStateOf(store.state.value.glass.encode()) }
    var encoded by rememberSaveable { mutableStateOf(entry) }
    val draft = remember(encoded) { GlassAppearance.decode(encoded) }
    var exitPrompt by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(draft) { store.previewGlass(draft) }
    LaunchedEffect(presentationDraft) { store.previewPresentation(presentationDraft) }
    DisposableEffect(store) { onDispose { store.previewGlass(null); store.previewPresentation(null) } }
    val dirty = encoded != entry || configDraft != configEntry || presentationDraft != presentationEntry
    BackHandler(enabled = dirty || saving) { if (!saving) exitPrompt = true }
    fun update(parameters: GlassParameters) { encoded = draft.update(parameters).encode(); notice = null }
    fun cancelPreview() {
        encoded = entry; configDraft = configEntry; presentationDraft = presentationEntry
        notice = "已恢复进入时的外观"
    }
    fun apply() {
        if (saving) return
        saving = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { app.repository.saveConfigChanges(configEntry, configDraft) }
                store.presentation(presentationDraft); store.saveGlass(draft)
                configEntry = configDraft; presentationEntry = presentationDraft; entry = encoded
                notice = "外观已应用，三种材质参数分别保留"
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { notice = error.message ?: "外观保存失败，请重试" }
            finally { saving = false }
        }
    }
    val chooseWallpaper = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val source = withContext(Dispatchers.IO) { com.xiaomanjun.sleepdownschedule.core.wallpaper.persistWallpaperSource(context, uri) }
                    ?: error("无法读取这张图片")
                configDraft = configDraft.copy(wallpaperUri = source.toString(),
                    wallpaperSourceWidth = null, wallpaperSourceHeight = null,
                    wallpaperPortraitCenterX = .5f, wallpaperPortraitCenterY = .5f, wallpaperPortraitScale = 1f,
                    wallpaperLandscapeCenterX = .5f, wallpaperLandscapeCenterY = .5f, wallpaperLandscapeScale = 1f)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { notice = error.message ?: "壁纸读取失败" }
        }
    }
    LazyColumn(Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = detailContentTopPadding() + 16.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            LiquidGlassDockPreview(state.copy(config = configDraft), com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme(configDraft))
        }
        item {
            Text("实时预览", style = MaterialTheme.typography.titleMedium)
            Text("使用本机课表和壁纸。默认液态沿用原版玻璃；高斯更朦胧，柔光更柔和。按钮和滑块保持原来的形状。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("壁纸、卡片尺寸与字大小：当前课表。材质、着色、详情透明度及显示开关：全部课表。",
                style = MaterialTheme.typography.bodySmall)
        }
        item {
            TabRow(listOf("壁纸", "效果", "布局"), group, { group = it })
        }
        if (group == 0) item {
            SettingsGroup(backdrop, configDraft) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsActionButton("选择壁纸", backdrop, onClick = { chooseWallpaper.launch(arrayOf("image/*")) })
                    SettingsActionButton("使用简洁背景", backdrop, onClick = { configDraft = configDraft.copy(wallpaperUri = null,
                        defaultWallpaperStyle = com.xiaomanjun.sleepdownschedule.model.DefaultWallpaperStyle.NONE) })
                    Text("按原有居中裁剪规则显示；精细裁剪仍可在主页外观中调整。", style = MaterialTheme.typography.bodySmall)
                }
                GlassValue("壁纸模糊", configDraft.wallpaperBlur, 0f..32f, "dp", backdrop) { configDraft = configDraft.copy(wallpaperBlur = it) }
                GlassValue("壁纸亮度", configDraft.wallpaperBrightness, .35f..1f, "%", backdrop) { configDraft = configDraft.copy(wallpaperBrightness = it) }
            }
        }
        if (group == 2) item {
            SettingsGroup(backdrop, configDraft) {
                GlassValue("课程卡片高度", configDraft.weekCardHeightScale, .72f..1.45f, "×", backdrop) { configDraft = configDraft.copy(weekCardHeightScale = it, weekCardHeightDp = null) }
                GlassValue("课程卡片圆角", configDraft.weekCardCornerProgress, 0f..1f, "%", backdrop) { configDraft = configDraft.copy(weekCardCornerProgress = it) }
                GlassValue("课程文字大小", configDraft.courseCardFontScale, .8f..1.35f, "×", backdrop) { configDraft = configDraft.copy(courseCardFontScale = it) }
                GlassValue("卡片材质不透明度", configDraft.cardAlpha, 0f..1f, "%", backdrop) { configDraft = configDraft.copy(cardAlpha = it) }
                GlassValue("课程色着色强度", presentationDraft.cardTint, 0f..1f, "%", backdrop) { presentationDraft = presentationDraft.copy(cardTint = it) }
                GlassValue("详情卡片不透明度", presentationDraft.detailOpacity, .45f..1f, "%", backdrop) { presentationDraft = presentationDraft.copy(detailOpacity = it) }
                SettingsToggleRow("显示节次时间", "压缩行高仍显示，必要时滚动查看", presentationDraft.showPeriodTimes, backdrop) {
                    presentationDraft = presentationDraft.copy(showPeriodTimes = it)
                }
                SettingsToggleRow("显示午休与晚休", "按真实作息边界分隔，保留中午教学节次", presentationDraft.showBreakDividers, backdrop) {
                    presentationDraft = presentationDraft.copy(showBreakDividers = it)
                }
            }
        }
        if (group == 1) item {
            TabRow(GlassStyle.entries.map { it.title.removeSuffix("玻璃") }, draft.style.ordinal,
                { encoded = draft.copy(style = GlassStyle.entries[it], customized = true).encode() })
        }
        if (group == 1 && draft.style == GlassStyle.SOFT) item {
            Text("参考澎湃 OS 的柔光风格，使用本机公开渲染能力，无需系统模块。", style = MaterialTheme.typography.bodySmall)
        }
        if (group == 1) item {
            SettingsGroup(backdrop, state.config) {
                GlassValue("模糊半径", draft.current.blur, 0f..64f, "dp", backdrop) { update(draft.current.copy(blur = it)) }
                GlassValue("材质不透明度", draft.current.opacity, 0f..1f, "%", backdrop) { update(draft.current.copy(opacity = it)) }
                GlassValue("着色", draft.current.tint, 0f..1f, "%", backdrop) { update(draft.current.copy(tint = it)) }
                GlassValue("边缘高光", draft.current.highlight, 0f..1f, "%", backdrop) { update(draft.current.copy(highlight = it)) }
                GlassValue("阴影半径", draft.current.shadow, 0f..24f, "dp", backdrop) { update(draft.current.copy(shadow = it)) }
            }
        }
        if (group == 1) item {
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
                    SettingsActionButton(if (draft.style == GlassStyle.LIQUID) "恢复原版液态玻璃" else "恢复当前材质默认值", backdrop,
                        onClick = { update(GlassParameters.defaults(draft.style)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsActionButton("取消预览", backdrop, onClick = ::cancelPreview, modifier = Modifier.weight(1f))
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
        LiquidAlertAction("舍弃预览", LiquidAlertActionStyle.Destructive) { cancelPreview(); exitPrompt = false },
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
