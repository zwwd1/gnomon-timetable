package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class EditablePeriodTimesTest {
    private val config = defaultConfig().copy(morningPeriodCount = 2, noonPeriodCount = 0, afternoonPeriodCount = 1, eveningPeriodCount = 0)
    private val scheme = PeriodSchemeEntity(id = 71, scheduleId = config.id, name = "自定作息", mode = PeriodSchemeMode.MANUAL)

    @Test fun missingSchoolTimesRemainBlankButEveryPeriodCanBeEdited() {
        val rows = editablePeriodTimes(config, PeriodSchemeDraft(scheme, emptyList()))
        assertEquals(listOf(1, 2, 3), rows.map { it.periodIndex })
        assertTrue(rows.all { it.startTime.isEmpty() && it.endTime.isEmpty() })
        assertNotNull(validateResolvedPeriodTimes(rows))
    }

    @Test fun fillingMissingSlotKeepsLegacyTimesAndOtherSchemeUntouched() {
        val custom = PeriodSchemeTimeEntity(71, 2, "10:12", "11:02")
        val original = PeriodSchemeDraft(scheme, listOf(custom))
        val other = PeriodSchemeDraft(scheme.copy(id = 72), listOf(custom.copy(schemeId = 72, startTime = "10:20")))
        val rows = editablePeriodTimes(config, original)
        assertEquals(custom, rows[1])
        val completed = rows.map { when(it.periodIndex) {
            1 -> it.copy(startTime = "08:13", endTime = "09:01")
            3 -> it.copy(startTime = "14:21", endTime = "15:09")
            else -> it
        } }
        assertNull(validateResolvedPeriodTimes(completed))
        assertEquals(listOf(custom), original.times)
        assertEquals("10:20", other.times.single().startTime)
    }
}
