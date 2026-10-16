package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogLiquidButton
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertActionStyle
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertDialog
import com.xiaomanjun.sleepdownschedule.domain.schedule.*
import com.xiaomanjun.sleepdownschedule.model.*

/** A missing school time is editable data, never a reason to hide a period from the editor. */
@Composable
internal fun IncompletePeriodTimesEditor(config: ScheduleConfigEntity, active: PeriodSchemeDraft,
    backdrop: Backdrop?, onCancel: () -> Unit, onDone: (PeriodSchemeDraft) -> Unit, initialPeriodIndex: Int? = null) {
    val initial = remember(active, config.id) { editablePeriodTimes(config, active) }
    var times by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    val targetRow = remember { BringIntoViewRequester() }
    LaunchedEffect(initialPeriodIndex) {
        if (initialPeriodIndex != null && times.any { it.periodIndex == initialPeriodIndex }) {
            withFrameNanos { }; targetRow.bringIntoView()
        }
    }
    fun cancel() { if (times != initial) confirmExit = true else onCancel() }
    fun save() {
        val problem = validateEditablePeriodTimes(times)
        if (problem != null) error = problem else onDone(active.copy(
            scheme = active.scheme.copy(mode = PeriodSchemeMode.MANUAL), times = times,
            specialBreaks = emptyMap(), overriddenPeriods = emptySet()))
    }
    BackHandler { cancel() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().imePadding()
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DialogLiquidButton(backdrop, "取消", ::cancel, monochromeNeutral = true)
            Text("逐节编辑作息", style = MaterialTheme.typography.titleLarge)
            DialogLiquidButton(backdrop, "完成", ::save)
        }
        Text("请按实际时间补填，可逐节保存。未知节次保持留空，不生成提醒；开始和结束须同时填写。", style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PeriodDayPart.entries.forEach { part ->
            val inPart = times.filter { it.periodIndex in config.periodRange(part) }
            if (inPart.isNotEmpty()) SettingsGroup(backdrop, config) {
                Text(when(part) { PeriodDayPart.MORNING -> "上午"; PeriodDayPart.NOON -> "中午";
                    PeriodDayPart.AFTERNOON -> "下午"; PeriodDayPart.EVENING -> "晚上" }, Modifier.padding(14.dp))
                inPart.forEach { time ->
                    Column(if (time.periodIndex == initialPeriodIndex) Modifier.bringIntoViewRequester(targetRow) else Modifier) {
                    SettingsTimePickerRow("第 ${time.periodIndex} 节 · 开始", time.startTime, { next ->
                        times = times.map { if (it.periodIndex == time.periodIndex) it.copy(startTime = next) else it }; error = null
                    }, backdrop, config, initiallyOpen = time.periodIndex == initialPeriodIndex)
                    SettingsTimePickerRow("结束", time.endTime, { next ->
                        times = times.map { if (it.periodIndex == time.periodIndex) it.copy(endTime = next) else it }; error = null
                    }, backdrop, config)
                    SettingsDivider()
                    }
                }
            }
        }
    }
    if (confirmExit) LiquidAlertDialog("放弃未保存的时间？", "本次补填尚未保存。", listOf(
        LiquidAlertAction("继续编辑", LiquidAlertActionStyle.Secondary) { confirmExit = false },
        LiquidAlertAction("放弃", LiquidAlertActionStyle.Destructive) { confirmExit = false; onCancel() }
    ), backdrop, config, { confirmExit = false })
}
