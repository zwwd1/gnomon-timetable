package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*

/** Keep the established clock-overlap rule; use structural changes when the old clock is unknown. */
internal fun remapPeriodsAfterStructure(source: List<Int>, oldTimes: List<PeriodEntity>,
    newTimes: List<PeriodSchemeTimeEntity>, operations: List<PeriodTopologyOperation>): List<Int> {
    if (newTimes.isEmpty()) return source
    val oldByIndex = oldTimes.associateBy { it.periodIndex }
    val known = newTimes.mapNotNull { time ->
        val start = parseMinuteOfDay(time.startTime) ?: return@mapNotNull null
        val end = parseMinuteOfDay(time.endTime) ?: return@mapNotNull null
        Triple(time.periodIndex, start, end)
    }
    fun structural(index: Int): Int {
        var mapped = index
        operations.forEach { op -> mapped = when (op) {
            is PeriodTopologyOperation.AddAfter -> if (mapped > op.periodIndex) mapped + 1 else mapped
            is PeriodTopologyOperation.Delete -> if (mapped > op.periodIndex) mapped - 1 else mapped
        } }
        return mapped.coerceIn(1, newTimes.size)
    }
    return source.flatMap { index ->
        val old = oldByIndex[index]
        val start = parseMinuteOfDay(old?.startTime)
        val end = parseMinuteOfDay(old?.endTime)
        if (start == null || end == null || known.isEmpty()) listOf(structural(index))
        else known.filter { (_, a, b) -> a < end && b > start }.map { it.first }.ifEmpty {
            listOf(known.minBy { (_, a, _) -> kotlin.math.abs(a - start) }.first)
        }
    }.distinct().sorted()
}
