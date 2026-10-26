package cn.scvtc.campus

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class SchoolDefaultPeriodsTest {
    @Test fun newSchedulesUseFourMorningFourAfternoonTwoEveningPeriods() {
        val config = defaultConfig()
        assertEquals(4, config.morningPeriodCount)
        assertEquals(0, config.noonPeriodCount)
        assertEquals(4, config.afternoonPeriodCount)
        assertEquals(2, config.eveningPeriodCount)
        assertEquals(10, defaultPeriods().size)
        assertTrue(defaultPeriods(7).all { it.scheduleId == 7 })
    }
    @Test fun unknownLegacyTimesCanBeFilledWithoutRemovingPeriods() {
        val filled = withSchoolDefaultsIfUnknown((1..12).map { PeriodEntity(it, "", "", 7) })
        assertEquals(12, filled.size)
        assertEquals("08:30", filled.first().startTime)
        assertEquals("21:00", filled[9].endTime)
        assertEquals("", filled[10].startTime)
    }
    @Test fun partiallyCustomizedTimelineIsKeptExactly() {
        val saved = listOf(PeriodEntity(1, "09:00", "10:00"), PeriodEntity(2, "", ""))
        assertEquals(saved, withSchoolDefaultsIfUnknown(saved))
    }
    @Test fun missingTimelineRetainsExplicitPeriodCounts() {
        val config = defaultConfig(7).copy(morningPeriodCount = 4, afternoonPeriodCount = 4, eveningPeriodCount = 0)
        val skeleton = com.xiaomanjun.sleepdownschedule.domain.schedule.periodSkeleton(config, emptyList())
        val filled = withSchoolDefaultsIfUnknown(skeleton)
        assertEquals(8, filled.size)
        assertEquals("08:30", filled.first().startTime)
        assertEquals("17:30", filled.last().endTime)
    }
}
