package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*

/** Change each part at its tail. Unknown new times stay unknown; all schemes share the mapping. */
internal fun resizePeriodStructure(
    config: ScheduleConfigEntity, draft: SchedulePeriodSchemesDraft, target: ScheduleConfigEntity
): SchedulePeriodSchemesDraft {
    require(PeriodDayPart.entries.all { target.periodCount(it) in 0..20 } && target.totalPeriodCount() in 1..40)
    var current = config
    var result = draft.copy(schemes = draft.schemes.map {
        it.copy(scheme = it.scheme.copy(mode = PeriodSchemeMode.MANUAL),
            times = editablePeriodTimes(config, it), specialBreaks = emptyMap(), overriddenPeriods = emptySet())
    })
    PeriodDayPart.entries.forEach { part ->
        while (current.periodCount(part) != target.periodCount(part)) {
            val adding = current.periodCount(part) < target.periodCount(part)
            val after = PeriodDayPart.entries.take(part.ordinal + 1).sumOf { current.periodCount(it) }
            result = result.copy(schemes = result.schemes.map { scheme ->
                val times = if (adding) scheme.times.map {
                    if (it.periodIndex > after) it.copy(periodIndex = it.periodIndex + 1) else it
                } + PeriodSchemeTimeEntity(scheme.scheme.id, after + 1, "", "")
                else scheme.times.filterNot { it.periodIndex == after }.map {
                    if (it.periodIndex > after) it.copy(periodIndex = it.periodIndex - 1) else it
                }
                scheme.copy(times = times.sortedBy { it.periodIndex })
            }, topologyOperations = result.topologyOperations + if (adding)
                PeriodTopologyOperation.AddAfter(after) else PeriodTopologyOperation.Delete(after))
            current = current.withTimelineCount(part, current.periodCount(part) + if (adding) 1 else -1)
        }
    }
    return result
}
