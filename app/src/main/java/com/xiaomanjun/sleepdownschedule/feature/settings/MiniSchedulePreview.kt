package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import com.xiaomanjun.sleepdownschedule.model.AppState
import com.xiaomanjun.sleepdownschedule.domain.schedule.*
import com.xiaomanjun.sleepdownschedule.glass.ui.*
import com.xiaomanjun.sleepdownschedule.feature.home.week.WeekCourseLabel
import com.xiaomanjun.sleepdownschedule.feature.home.day.homeForegroundColor

/** Uses current courses and the same card components as the home grid; no demonstration data. */
@Composable
internal fun MiniSchedulePreview(state: AppState, backdrop: Backdrop) {
    val config = state.config
    val week = effectiveCurrentWeek(config)
    val monday = scheduleWeekStartDate(config, week)
    val textColor = homeForegroundColor(config)
    val row = maxOf(42.dp * config.weekCardHeightScale,
        com.xiaomanjun.sleepdownschedule.feature.home.minimumWeekTimelineRowHeightDp(androidx.compose.ui.platform.LocalDensity.current.fontScale).dp)
    val presentation = cn.scvtc.campus.LocalAppExperience.current
    val layout = weekGridLayout(config, state.periods, state.courses, row.value, presentation.showBreakDividers)
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("第 $week 周", color = textColor, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Spacer(Modifier.width(34.dp))
            (1..7).forEach { Text(listOf("一", "二", "三", "四", "五", "六", "日")[it - 1],
                modifier = Modifier.weight(1f), color = textColor, fontSize = 10.sp) }
        }
        Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
            Column(Modifier.width(34.dp)) {
                state.periods.forEachIndexed { index, p ->
                    if (layout.bands.any { it.beforeRow == index }) Spacer(Modifier.height(row * layout.bandRows))
                    Column(Modifier.height(row), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.periodIndex.toString(), color = textColor, fontSize = 11.sp)
                        if (cn.scvtc.campus.LocalAppExperience.current.showPeriodTimes) {
                            Text(p.startTime.takeIf(String::isNotBlank) ?: "待设", color = textColor, fontSize = 8.sp, lineHeight = 9.sp)
                            Text(p.endTime.takeIf(String::isNotBlank) ?: "待设", color = textColor, fontSize = 8.sp, lineHeight = 9.sp)
                        }
                    }
                }
            }
            (0..6).forEach { day ->
                BoxWithConstraints(Modifier.weight(1f).height(row * layout.totalRows(state.periods.size))) {
                    val width = maxWidth
                    layout.bands.forEach { band ->
                        Box(Modifier.fillMaxWidth().offset(y = row * layout.bottom(band.beforeRow.toFloat()))
                            .height(row * layout.bandRows), contentAlignment = Alignment.Center) {
                            Text(band.title, color = textColor.copy(alpha = .6f), fontSize = 8.sp, maxLines = 1)
                        }
                    }
                    coursesForDate(state, monday.plusDays(day.toLong())).forEach { course ->
                        val positions = state.periods.mapIndexedNotNull { index, p -> index.takeIf { p.periodIndex in course.periods } }
                        val runs = mutableListOf<MutableList<Int>>()
                        positions.forEach { index ->
                            if (runs.isEmpty() || runs.last().last() + 1 != index) runs += mutableListOf<Int>()
                            runs.last() += index
                        }
                        val exact = exactTimeWeekPlacement(course, state.periods)
                        val visualRuns = if (exact != null) listOf(exact.topRows to exact.topRows + exact.heightRows)
                            else runs.flatMap { layout.split(it.first(), it.last() + 1) }.map { it.first.toFloat() to (it.last + 1).toFloat() }
                        visualRuns.forEach { (start, end) ->
                            val height = (row * (layout.bottom(end) - layout.top(start)) - 2.dp).coerceAtLeast(1.dp)
                            CourseGlassCard(backdrop, config, course = course,
                                shape = RoundedRectangle((4f + config.weekCardCornerProgress * 12f).dp),
                                modifier = Modifier.offset(y = row * layout.top(start)).fillMaxWidth().height(height).padding(horizontal = 1.dp)) {
                                WeekCourseLabel(course, config, textColor, null, width, height)
                            }
                        }
                    }
                }
            }
        }
    }
}
