package com.xiaomanjun.sleepdownschedule.feature.course.editor

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.xiaomanjun.sleepdownschedule.model.PeriodEntity

/** Measure the same text styles as the detail; the shell and touch bounds share this target. */
internal fun courseDetailHeight(request: CourseEditorOverlayRequest, periods: List<PeriodEntity>,
    widthPx: Float, maximumPx: Float, density: Density, type: Typography, measurer: TextMeasurer): Float {
    val contentWidth = (widthPx - with(density) { 40.dp.toPx() }).toInt().coerceAtLeast(1)
    fun text(value: String, style: TextStyle) = measurer.measure(value, style,
        constraints = Constraints(maxWidth = contentWidth)).size.height.toFloat()
    val c = request.course
    val time = com.xiaomanjun.sleepdownschedule.domain.schedule.courseTimeLabel(c, periods)
    val missing = com.xiaomanjun.sleepdownschedule.domain.schedule.courseStartTime(c, periods) == null ||
        com.xiaomanjun.sleepdownschedule.domain.schedule.courseEndTime(c, periods) == null
    val blocks = listOf(text(c.name, type.headlineLarge),
        text("上课时间", type.labelMedium) + text(time, type.titleLarge) + with(density) { 4.dp.toPx() },
        text("上课地点", type.labelMedium) + text(c.location?.takeIf(String::isNotBlank) ?: "未填写地点", type.titleLarge) + with(density) { 4.dp.toPx() },
        text("授课教师 · ${c.teacher?.takeIf(String::isNotBlank) ?: "未填写"}", type.bodyLarge),
        text("上课周次 · ${compactWeekSelectionLabel(c.weeks)}", type.bodyMedium)) +
        listOfNotNull(c.note?.takeIf(String::isNotBlank)?.let { text(it, type.bodyLarge) },
            request.contextMessage?.let { text(it, type.bodyMedium) })
    val measured = blocks.sum() + with(density) {
        (40.dp + 48.dp * (if (missing) 3 else 2) + 12.dp * (blocks.size + if (missing) 2 else 1)).toPx()
    }
    return measured.coerceIn(minOf(with(density) { 240.dp.toPx() }, maximumPx), maximumPx)
}
