package com.xiaomanjun.sleepdownschedule.feature.home.week

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.core.ui.text.CourseCardText
import com.xiaomanjun.sleepdownschedule.feature.home.LocalPersonalizationPreview
import kotlin.math.floor

/** One hierarchy for the grid, lifted card and morph source. Foreground text stays sharp. */
@Composable
internal fun WeekCourseLabel(course: CourseEntity, config: ScheduleConfigEntity, color: Color,
    themeColor: Color?, width: Dp, height: Dp, adaptiveContrast: Boolean = true) {
    val density = LocalDensity.current
    val scale = (LocalPersonalizationPreview.current?.cardFontScale ?: config.courseCardFontScale)
        .coerceIn(.8f, 1.35f) * if (width >= 120.dp) 1.18f else 1f
    val nameSize = 11.sp * scale
    val nameLine = 13.sp * scale
    val secondarySize = 9.sp * scale
    val secondaryLine = 11.sp * scale
    val rowDp = with(density) { secondaryLine.toDp().value }
    val nameDp = with(density) { nameLine.toDp().value }
    val usable = (height.value - 10f).coerceAtLeast(nameDp)
    val presentation = cn.scvtc.campus.LocalAppExperience.current
    val location = course.location?.takeIf { presentation.showLocation && it.isNotBlank() }
    val teacher = course.teacher?.takeIf { presentation.showTeacher && it.isNotBlank() }
    val locationLines = if (location != null && usable >= nameDp + rowDp + 3f)
        if (usable >= nameDp * 3 + rowDp * 3 + 6f) 2 else 1 else 0
    val showTeacher = teacher != null && usable >= nameDp * 2 + rowDp * (locationLines + 1) + 6f
    val reserved = rowDp * (locationLines + if (showTeacher) 1 else 0) + 3f *
        ((if (locationLines > 0) 1 else 0) + if (showTeacher) 1 else 0)
    val nameLines = floor((usable - reserved) / nameDp).toInt().coerceIn(1, 12)
    Column(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        CourseCardText(course.name, color = color, themeColor = themeColor,
            fontSize = nameSize, lineHeight = nameLine, fontWeight = FontWeight.SemiBold,
            maxLines = nameLines, overflow = TextOverflow.Ellipsis, adaptiveContrast = adaptiveContrast)
        if (locationLines > 0) CourseCardText(location.orEmpty(), color = color.copy(alpha = .88f),
            themeColor = themeColor, fontSize = secondarySize, lineHeight = secondaryLine,
            maxLines = locationLines, overflow = TextOverflow.Ellipsis, adaptiveContrast = adaptiveContrast)
        if (showTeacher) CourseCardText(teacher.orEmpty(), color = color.copy(alpha = .72f),
            themeColor = themeColor, fontSize = secondarySize, lineHeight = secondaryLine,
            maxLines = 1, overflow = TextOverflow.Ellipsis, adaptiveContrast = adaptiveContrast)
    }
}
