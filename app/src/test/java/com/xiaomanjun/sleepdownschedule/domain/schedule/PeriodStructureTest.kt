package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class PeriodStructureTest {
    @Test fun addingNoonKeepsAllSchemesExistingTimesAndExplicitMapping() {
        val config = defaultConfig().copy(morningPeriodCount = 1, noonPeriodCount = 0, afternoonPeriodCount = 1, eveningPeriodCount = 0)
        val schemes = listOf(1L, 2L).map { id -> PeriodSchemeDraft(PeriodSchemeEntity(id, 1, "作息$id"), listOf(
            PeriodSchemeTimeEntity(id, 1, "08:10", "08:55"), PeriodSchemeTimeEntity(id, 2, "14:10", "14:55"))) }
        val draft = SchedulePeriodSchemesDraft(schemes, 2)
        val target = config.copy(noonPeriodCount = 1)
        val result = resizePeriodStructure(config, draft, target)
        assertEquals(2L, result.activeSchemeId)
        assertEquals(listOf(PeriodTopologyOperation.AddAfter(1)), result.topologyOperations)
        result.schemes.forEach {
            assertEquals(listOf("08:10", "", "14:10"), it.times.map { time -> time.startTime })
            assertNull(validateEditablePeriodTimes(it.times))
        }
        val restored = resizePeriodStructure(target, result, config)
        assertEquals(schemes.map { it.times }, restored.schemes.map { it.times })
        assertEquals(PeriodTopologyOperation.Delete(2), restored.topologyOperations.last())
    }
    @Test fun emptyOrExcessiveStructureCannotBeSaved() {
        val config = defaultConfig()
        val draft = SchedulePeriodSchemesDraft(emptyList(), 0)
        assertTrue(runCatching { resizePeriodStructure(config, draft, config.copy(morningPeriodCount = 0,
            noonPeriodCount = 0, afternoonPeriodCount = 0, eveningPeriodCount = 0)) }.isFailure)
        assertTrue(runCatching { resizePeriodStructure(config, draft, config.copy(noonPeriodCount = 21)) }.isFailure)
    }
}
