package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class PeriodCourseMappingTest {
    @Test fun unknownClocksFollowExplicitInsertionAcrossNoon() {
        val old = (1..3).map { PeriodEntity(it, "", "") }
        val next = (1..4).map { PeriodSchemeTimeEntity(1, it, "", "") }
        assertEquals(listOf(1, 3, 4), remapPeriodsAfterStructure(listOf(1, 2, 3), old, next,
            listOf(PeriodTopologyOperation.AddAfter(1))))
    }
    @Test fun knownClocksKeepOverlapRuleAndUnknownCourseDoesNotJumpToKnownNeighbor() {
        val old = listOf(PeriodEntity(1, "", ""), PeriodEntity(2, "14:10", "14:55"))
        val next = listOf(PeriodSchemeTimeEntity(1, 1, "", ""), PeriodSchemeTimeEntity(1, 2, "", ""),
            PeriodSchemeTimeEntity(1, 3, "14:10", "14:55"))
        val ops = listOf(PeriodTopologyOperation.AddAfter(1))
        assertEquals(listOf(1), remapPeriodsAfterStructure(listOf(1), old, next, ops))
        assertEquals(listOf(3), remapPeriodsAfterStructure(listOf(2), old, next, ops))
    }
    @Test fun deletingTailRetainsCourseInAnExistingSlotAfterConfirmation() {
        val old = (1..3).map { PeriodEntity(it, "", "") }
        val next = (1..2).map { PeriodSchemeTimeEntity(1, it, "", "") }
        assertEquals(listOf(2), remapPeriodsAfterStructure(listOf(3), old, next,
            listOf(PeriodTopologyOperation.Delete(3))))
    }
}
