package com.xiaomanjun.sleepdownschedule.feature.backup

import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.domain.schedule.*
import org.junit.Assert.*
import org.junit.Test

internal fun fileTestArchive(): BackupArchive {
    val config = defaultConfig().copy(morningPeriodCount = 2, noonPeriodCount = 0, afternoonPeriodCount = 0,
        eveningPeriodCount = 0, termStartDate = "2026-09-07", autoCurrentWeek = true,
        scheduleAdjustmentsJson = encodeScheduleAdjustments(listOf(ScheduleAdjustment("2026-09-08"))))
    val snapshot = BackupRoomSnapshot(schedules = listOf(ScheduleProfileEntity(1, "测试课表", true)),
        configs = listOf(config), periods = listOf(PeriodEntity(1, "08:00", "08:45"), PeriodEntity(2, "", "")),
        courses = listOf(CourseEntity(1, "课程", "教师", "教室", 1, listOf(1), listOf(1, 2), WeekParity.ALL, null)),
        periodSchemes = listOf(PeriodSchemeEntity(1, 1, "常用", isActive = true), PeriodSchemeEntity(2, 1, "备用")),
        periodSchemeTimes = listOf(PeriodSchemeTimeEntity(1, 1, "08:00", "08:45"), PeriodSchemeTimeEntity(1, 2, "", ""),
            PeriodSchemeTimeEntity(2, 1, "09:00", "09:45"), PeriodSchemeTimeEntity(2, 2, "10:00", "10:45")))
    return BackupExportMapper.toArchive(BackupSourceMetadata("2026-10-15T00:00:00Z", "1.2.13", 26,
        "cn.scvtc.campus.preview", 39, "Android"), snapshot, BackupPreferences(1))
}

class TimetableJsonTest {
    @Test fun jsonRoundTripRetainsTwoSchemesUnknownClockAndAdjustments() {
        val original = fileTestArchive()
        val document = TimetableJsonDocument(metadata = original.metadata, schedule = original.data.schedules.single())
        val text = TimetableJsonCodec.encode(document)
        val restored = TimetableJsonCodec.decode(text)
        assertEquals(original.data.schedules, restored.data.schedules)
        assertFalse(text.contains("password", true))
        assertEquals("", restored.data.schedules.single().periods.last().startTime)
    }
    @Test fun corruptVersionAndMissingPeriodReferenceFailBeforeImportPlan() {
        val original = fileTestArchive()
        val document = TimetableJsonDocument(metadata = original.metadata, schedule = original.data.schedules.single())
        assertTrue(runCatching { TimetableJsonCodec.encode(document.copy(version = 99)) }.isFailure)
        assertTrue(runCatching { TimetableJsonCodec.encode(document.copy(schedule = document.schedule.copy(periods = emptyList()))) }.isFailure)
    }
    @Test fun backupKeepsPresentationAndRejectsInvalidDockWithoutLoadingCredentials() {
        val original = fileTestArchive()
        val preferences = original.preferences.copy(presentation = BackupPresentationPreferences(false,
            listOf("courses", "grades", "settings"), com.xiaomanjun.sleepdownschedule.glass.GlassAppearance().encode()))
        assertEquals(preferences, BackupCodec.decode(BackupCodec.encode(original.copy(preferences = preferences))).preferences)
        assertTrue(runCatching { BackupCodec.encode(original.copy(preferences = preferences.copy(
            presentation = preferences.presentation!!.copy(dock = listOf("unknown"))))) }.isFailure)
    }
}
