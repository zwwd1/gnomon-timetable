package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.PeriodEntity
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

/** Missing clock values remain unknown across the home, reminder, widget and export queries. */
fun periodSkeleton(config: ScheduleConfigEntity, stored: List<PeriodEntity>): List<PeriodEntity> {
    val byIndex = stored.associateBy { it.periodIndex }
    return ((1..config.totalPeriodCount()).toList() + byIndex.keys).distinct().sorted().map { index ->
        byIndex[index] ?: PeriodEntity(index, "", "", config.id)
    }
}
