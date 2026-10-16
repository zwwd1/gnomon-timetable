package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import com.xiaomanjun.sleepdownschedule.domain.schedule.*
import com.xiaomanjun.sleepdownschedule.model.*

@Composable
internal fun PeriodSchemeRecovery(state: AppState, backdrop: Backdrop?, config: ScheduleConfigEntity,
    topPadding: Dp, leadingContent: @Composable () -> Unit, onDraftChange: (SchedulePeriodSchemesDraft) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
        start = 16.dp, end = 16.dp, top = topPadding + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        leadingContent()
        SettingsGroup(backdrop, state.config) {
            Text("这张课表还没有作息方案", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            Text("可以保留已知时间，建立一套可逐节填写的作息。未知时间会留空。", Modifier.padding(horizontal = 16.dp))
            DialogLiquidButton(backdrop, "建立作息", {
                val id = -1L
                val times = periodSkeleton(config, state.periods.filter { it.scheduleId == config.id }).map {
                    PeriodSchemeTimeEntity(id, it.periodIndex, it.startTime, it.endTime)
                }
                onDraftChange(SchedulePeriodSchemesDraft(listOf(PeriodSchemeDraft(
                    PeriodSchemeEntity(id = id, scheduleId = config.id, name = "日常作息", isActive = true), times)), id))
            }, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
internal fun PeriodCountsEditor(config: ScheduleConfigEntity, backdrop: Backdrop?, onCancel: () -> Unit,
    onDone: (ScheduleConfigEntity) -> Unit) {
    var values by remember { mutableStateOf(PeriodDayPart.entries.map { config.periodCount(it).toString() }) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    fun cancel() { if (values != PeriodDayPart.entries.map { config.periodCount(it).toString() }) confirmExit = true else onCancel() }
    BackHandler { cancel() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
        .imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DialogLiquidButton(backdrop, "取消", ::cancel, monochromeNeutral = true)
            Text("节次数量", style = MaterialTheme.typography.titleLarge)
            DialogLiquidButton(backdrop, "完成", {
                val counts = values.map { it.toIntOrNull() }
                if (counts.any { it == null || it !in 0..20 } || counts.filterNotNull().sum() !in 1..40)
                    error = "每个时段可设 0–20 节，总计保留 1–40 节。"
                else onDone(config.copy(morningPeriodCount = counts[0]!!, noonPeriodCount = counts[1]!!,
                    afternoonPeriodCount = counts[2]!!, eveningPeriodCount = counts[3]!!))
            })
        }
        Text("修改当前课表的全部作息。新增节次时间留空；减少节次会调整课程对应关系，最终保存前会提示影响。中午仍是教学时段。",
            style = MaterialTheme.typography.bodySmall)
        SettingsGroup(backdrop, config) {
            PeriodDayPart.entries.forEachIndexed { index, part ->
                SettingsTextFieldRow(part.timelineLabel(), values[index], { next ->
                    values = values.toMutableList().apply { this[index] = next.filter(Char::isDigit).take(2) }; error = null
                }, KeyboardType.Number)
                if (index < 3) SettingsDivider()
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (confirmExit) LiquidAlertDialog("放弃节数调整？", "这些节数尚未加入草稿。", listOf(
        LiquidAlertAction("继续编辑", LiquidAlertActionStyle.Secondary) { confirmExit = false },
        LiquidAlertAction("放弃", LiquidAlertActionStyle.Destructive) { confirmExit = false; onCancel() }
    ), backdrop, config, { confirmExit = false })
}
