package cn.scvtc.campus.core

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TeachingCalendarTest {
    @Test fun schoolMondayIsIndependentOfDeviceTimezone() {
        val instant = Instant.parse("2026-10-25T16:30:00Z")
        val firstMonday = LocalDate.parse("2026-09-07")
        for (zone in listOf("America/Los_Angeles", "UTC", "Pacific/Auckland")) {
            val today = TeachingCalendar.today(Clock.fixed(instant, ZoneId.of(zone)))
            assertEquals(LocalDate.parse("2026-10-26"), today)
            assertEquals(8, TeachingCalendar.weekAt(firstMonday, today))
        }
    }

    @Test fun sundayBeforeSchoolMidnightStillBelongsToPreviousWeek() {
        val date = TeachingCalendar.today(Clock.fixed(Instant.parse("2026-10-25T15:59:59Z"), ZoneId.of("UTC")))
        assertEquals(LocalDate.parse("2026-10-25"), date)
        assertEquals(7, TeachingCalendar.weekAt(LocalDate.parse("2026-09-07"), date))
    }

    @Test fun daysBeforeTheFirstMondayDoNotTruncateIntoWeekOne() {
        assertEquals(0, TeachingCalendar.weekAt(LocalDate.parse("2026-09-07"), LocalDate.parse("2026-09-06")))
    }
}
