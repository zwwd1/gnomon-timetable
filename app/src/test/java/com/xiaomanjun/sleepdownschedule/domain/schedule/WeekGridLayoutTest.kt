package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class WeekGridLayoutTest {
    private val config = defaultConfig().copy(morningPeriodCount = 2, noonPeriodCount = 1, afternoonPeriodCount = 2, eveningPeriodCount = 1)
    private val times = listOf("08:10" to "08:55", "09:10" to "09:55", "12:10" to "12:55",
        "14:10" to "14:55", "15:10" to "15:55", "19:10" to "19:55")
        .mapIndexed { i, (start, end) -> PeriodEntity(i + 1, start, end) }

    @Test fun noonTeachingSurvivesAndRestUsesAnActualGap() {
        val layout = weekGridLayout(config, times, emptyList(), 48f, true)
        assertEquals(listOf(2, 5), layout.bands.map { it.beforeRow })
        assertEquals("09:55", layout.bands.first().start)
        assertEquals("12:10", layout.bands.first().end)
        assertEquals(7f, layout.totalRows(6), .0001f)
        assertEquals(2.5f, layout.top(2f), .0001f)
        assertEquals(1f, layout.height(2, 1), .0001f) // Noon remains its own ordinary row.
    }
    @Test fun forwardInverseAndBandHitStayAlignedAtDifferentRowHeights() {
        listOf(42f, 90f, 160f).forEach { row ->
            val layout = weekGridLayout(config, times, emptyList(), row, true)
            times.indices.forEach { index ->
                val visual = layout.top(index.toFloat()) + .4f
                assertEquals(index + .4f, layout.logicalAt(visual)!!, .0001f)
                assertEquals(index, layout.nearestStart(layout.top(index.toFloat()), times.size))
            }
            layout.bands.forEach { band ->
                assertNull(layout.logicalAt(layout.bottom(band.beforeRow.toFloat()) + layout.bandRows / 2))
                assertEquals(24f, layout.bandRows * row, .0001f)
            }
        }
    }
    @Test fun splitKeepsEveryTeachingRowAndLeavesBreakOutOfCards() {
        val layout = weekGridLayout(config, times, emptyList(), 48f, true)
        val pieces = layout.split(0, 6)
        assertEquals(listOf(0..1, 2..4, 5..5), pieces)
        assertEquals((0..5).toList(), pieces.flatMap { it.toList() })
        pieces.zipWithNext().forEach { (a, b) ->
            assertEquals(.5f, layout.top(b.first.toFloat()) - layout.bottom((a.last + 1).toFloat()), .0001f)
        }
    }
    @Test fun noEveningAndUnknownNoonDoNotCreateFalseBands() {
        val unknown = times.map { it.copy(startTime = "", endTime = "") }
        assertTrue(weekGridLayout(config.copy(eveningPeriodCount = 0), unknown.take(5), emptyList(), 48f, true).bands.isEmpty())
        val noNoon = config.copy(noonPeriodCount = 0, eveningPeriodCount = 0)
        val layout = weekGridLayout(noNoon, unknown.take(4), emptyList(), 48f, true)
        assertEquals(listOf("午休"), layout.bands.map { it.label })
        assertTrue(weekGridLayout(config, times, emptyList(), 48f, false).bands.isEmpty())
    }
    @Test fun zeroGapAndExactTimeCourseOccupyingRestSuppressBand() {
        val noNoon = config.copy(noonPeriodCount = 0, eveningPeriodCount = 0)
        val rows = times.take(4).toMutableList().apply { this[2] = this[2].copy(startTime = "09:55", endTime = "10:40") }
        assertTrue(weekGridLayout(noNoon, rows, emptyList(), 48f, true).bands.isEmpty())
        val course = CourseEntity(1, "午间实验", null, null, 1, listOf(2, 3), listOf(1), WeekParity.ALL, null,
            customStartTime = "11:00", customEndTime = "13:00")
        assertEquals(listOf("晚休"), weekGridLayout(config, times, listOf(course), 48f, true).bands.map { it.title })
    }
    @Test fun resizeRoundTripsAcrossBothBands() {
        val layout = weekGridLayout(config, times, emptyList(), 48f, true)
        (1..6).forEach { count -> assertEquals(count, layout.nearestSpan(0, layout.height(0, count), 6)) }
    }
    @Test fun movingLowerFragmentKeepsAllPeriodsAndResizeKeepsEarlierFragment() {
        val course = CourseEntity(1, "跨时段课程", null, null, 1, listOf(1, 2, 3), listOf(1), WeekParity.ALL, null)
        assertEquals(listOf(2, 3, 4), shiftedCoursePeriods(course, (1..6).toList(), 3, 4))
        assertNull(shiftedCoursePeriods(course, (1..6).toList(), 3, 1))
        assertEquals(listOf(1, 2, 3, 4), resizedCourseTail(course, (1..6).toList(), 3, 2))
    }
}
