package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import kotlin.math.abs

data class WeekRestBand(val beforeRow: Int, val title: String, val start: String? = null, val end: String? = null) {
    val label: String get() = if (start != null && end != null) "$title  $start–$end" else title
}

/** Coordinates are measured in lesson rows. Rest bands have a visual height, never a period index. */
data class WeekGridLayout(val bands: List<WeekRestBand> = emptyList(), val bandRows: Float = 0f) {
    fun top(row: Float): Float = row + bands.count { it.beforeRow <= row } * bandRows
    fun bottom(row: Float): Float = row + bands.count { it.beforeRow < row } * bandRows
    fun totalRows(periodCount: Int): Float = periodCount + bands.size * bandRows
    fun height(start: Int, span: Int): Float = bottom((start + span).toFloat()) - top(start.toFloat())
    fun logicalAt(visualRow: Float): Float? {
        var shift = 0f
        bands.forEach {
            val beginning = it.beforeRow + shift
            if (visualRow >= beginning && visualRow < beginning + bandRows) return null
            if (visualRow >= beginning + bandRows) shift += bandRows
        }
        return visualRow - shift
    }
    fun nearestStart(visualRow: Float, periodCount: Int): Int = (0 until periodCount.coerceAtLeast(1))
        .minBy { abs(top(it.toFloat()) - visualRow) }
    fun nearestSpan(start: Int, heightRows: Float, maxSpan: Int): Int = (1..maxSpan.coerceAtLeast(1))
        .minBy { abs(height(start, it) - heightRows) }
    fun split(start: Int, endExclusive: Int): List<IntRange> {
        val boundaries = listOf(start) + bands.map { it.beforeRow }.filter { it > start && it < endExclusive } + endExclusive
        return boundaries.zipWithNext { a, b -> a until b }
    }
}

/** A visual fragment still moves the whole course; never drop its other fragments on save. */
internal fun shiftedCoursePeriods(course: CourseEntity, indexes: List<Int>, anchor: Int, target: Int): List<Int>? {
    val from = indexes.indexOf(anchor).takeIf { it >= 0 } ?: return null
    val to = indexes.indexOf(target).takeIf { it >= 0 } ?: return null
    return course.periods.map { period ->
        val row = indexes.indexOf(period).takeIf { it >= 0 } ?: return null
        indexes.getOrNull(row + to - from) ?: return null
    }.distinct().sorted()
}

internal fun resizedCourseTail(course: CourseEntity, indexes: List<Int>, anchor: Int, span: Int): List<Int> {
    val start = indexes.indexOf(anchor)
    require(start >= 0 && span >= 1 && start + span <= indexes.size)
    return (course.periods.filter { indexes.indexOf(it) in 0 until start } + indexes.drop(start).take(span)).distinct().sorted()
}

/** Noon is a teaching part. Only an actual gap on either side may become the lunch band. */
fun weekGridLayout(config: ScheduleConfigEntity, periods: List<PeriodEntity>, courses: List<CourseEntity>,
    rowHeightDp: Float, enabled: Boolean): WeekGridLayout {
    if (!enabled || periods.isEmpty()) return WeekGridLayout()
    val indexed = periods.mapIndexed { row, p -> p.periodIndex to row }.toMap()
    fun candidate(left: PeriodDayPart, right: PeriodDayPart, title: String, allowUnknown: Boolean): WeekRestBand? {
        val a = config.periodRange(left).lastOrNull()?.let { index -> periods.firstOrNull { it.periodIndex == index } } ?: return null
        val b = config.periodRange(right).firstOrNull()?.let { index -> periods.firstOrNull { it.periodIndex == index } } ?: return null
        val row = indexed[b.periodIndex] ?: return null
        if (row <= 0 || periods[row - 1].periodIndex != a.periodIndex) return null
        val end = parseMinuteOfDay(a.endTime)
        val start = parseMinuteOfDay(b.startTime)
        if (end != null && start != null && start <= end) return null
        if ((end == null || start == null) && !allowUnknown) return null
        // Exact-time classes may occupy what would otherwise look like a break. Do not obscure them.
        if (courses.any { course ->
            val cs = parseMinuteOfDay(course.customStartTime)
            val ce = parseMinuteOfDay(course.customEndTime)
            if (cs == null || ce == null) false
            else if (end != null && start != null) cs < start && ce > end
            else course.periods.any { it <= a.periodIndex } && course.periods.any { it >= b.periodIndex }
        }) return null
        return WeekRestBand(row, title, if (end != null && start != null) a.endTime else null,
            if (end != null && start != null) b.startTime else null)
    }
    val lunch = if (config.noonPeriodCount > 0) listOfNotNull(
        candidate(PeriodDayPart.MORNING, PeriodDayPart.NOON, "午休", false),
        candidate(PeriodDayPart.NOON, PeriodDayPart.AFTERNOON, "午休", false)
    ).maxByOrNull { parseMinuteOfDay(it.end)!! - parseMinuteOfDay(it.start)!! }
    else candidate(PeriodDayPart.MORNING, PeriodDayPart.AFTERNOON, "午休", true)
    val evening = candidate(PeriodDayPart.AFTERNOON, PeriodDayPart.EVENING, "晚休", true)
    return WeekGridLayout(listOfNotNull(lunch, evening).sortedBy { it.beforeRow }, 24f / rowHeightDp.coerceAtLeast(1f))
}
