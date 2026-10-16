package com.xiaomanjun.sleepdownschedule.feature.course.editor

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaomanjun.sleepdownschedule.*

/** Readable content inside the existing course glass morph, without editable fields. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun CourseExpandedCard(
    course: CourseEntity,
    periods: List<PeriodEntity>,
    week: Int?,
    contextMessage: String?,
    textColor: Color,
    onClose: () -> Unit,
    onTimeSettings: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(Modifier.fillMaxSize().combinedClickable(
        onClick = {}, onLongClickLabel = "编辑课程", onLongClick = onLongPress
    ).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(week?.let { "第 $it 周 · ${weekdayLabel(course.weekday)}" }
                ?: weekdayLabel(course.weekday), color = textColor.copy(alpha = .72f),
                style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onClose) { Text("关闭", color = textColor) }
        }
        Text(course.name, color = textColor, style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold)
        Detail("上课时间", courseTimeLabel(course, periods), textColor)
        if (com.xiaomanjun.sleepdownschedule.domain.schedule.courseStartTime(course, periods) == null ||
            com.xiaomanjun.sleepdownschedule.domain.schedule.courseEndTime(course, periods) == null)
            TextButton(onClick = onTimeSettings) { Text("设置节次时间") }
        Detail("上课地点", course.location?.takeIf(String::isNotBlank) ?: "未填写地点", textColor)
        Text("授课教师 · ${course.teacher?.takeIf(String::isNotBlank) ?: "未填写"}",
            color = textColor.copy(alpha = .85f), style = MaterialTheme.typography.bodyLarge)
        Text("上课周次 · ${compactWeekSelectionLabel(course.weeks)}",
            color = textColor.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium)
        course.note?.takeIf(String::isNotBlank)?.let {
            Text(it, color = textColor, style = MaterialTheme.typography.bodyLarge)
        }
        contextMessage?.let {
            Text(it, color = textColor.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onLongPress) { Text("编辑课程", color = textColor) }
        }
    }
}

@Composable
private fun Detail(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = color.copy(alpha = .66f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = color, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold)
    }
}
