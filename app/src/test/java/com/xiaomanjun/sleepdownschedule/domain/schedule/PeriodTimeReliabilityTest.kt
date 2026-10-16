package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class PeriodTimeReliabilityTest {
    @Test fun skeletonNeverInventsClockAndRetainsKnownTimes() {
        val config = defaultConfig().copy(morningPeriodCount = 3, noonPeriodCount = 0, afternoonPeriodCount = 0, eveningPeriodCount = 0)
        val result = periodSkeleton(config, listOf(PeriodEntity(2, "09:20", "10:05")))
        assertEquals(listOf(1, 2, 3), result.map { it.periodIndex })
        assertEquals(listOf("", "09:20", ""), result.map { it.startTime })
    }
    @Test fun partialSchemeCanBeSavedButHalfFilledAndOverlappingCannot() {
        val blank = PeriodSchemeTimeEntity(1, 1, "", "")
        val known = PeriodSchemeTimeEntity(1, 2, "09:20", "10:05")
        assertNull(validateEditablePeriodTimes(listOf(blank, known)))
        assertNotNull(validateEditablePeriodTimes(listOf(blank.copy(startTime = "08:00"), known)))
        assertNotNull(validateEditablePeriodTimes(listOf(blank.copy(startTime = "09:00", endTime = "09:30"), known)))
    }
    @Test fun detailUsesCustomCourseClockAndNamesMissingSchemeClearly() {
        val course = CourseEntity(1, "课程", null, null, 1, listOf(1), listOf(1), WeekParity.ALL, null)
        assertEquals("节次时间待设置", courseTimeLabel(course, listOf(PeriodEntity(1, "", ""))))
        assertEquals("10:10 – 11:45", courseTimeLabel(course.copy(customStartTime = "10:10", customEndTime = "11:45"), emptyList()))
    }
}
