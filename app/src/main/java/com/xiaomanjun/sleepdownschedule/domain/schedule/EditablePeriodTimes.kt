package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.PeriodSchemeTimeEntity
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

internal fun editablePeriodTimes(config: ScheduleConfigEntity, active: PeriodSchemeDraft): List<PeriodSchemeTimeEntity> {
    val existing = resolveSchemeTimes(config, active).associateBy { it.periodIndex }
    return (1..config.totalPeriodCount()).map { index ->
        existing[index] ?: PeriodSchemeTimeEntity(active.scheme.id, index, "", "")
    }
}
