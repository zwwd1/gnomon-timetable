package cn.scvtc.campus

import cn.scvtc.campus.core.Extraction
import cn.scvtc.campus.core.Meeting
import com.xiaomanjun.sleepdownschedule.model.CourseEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ScvtcCourseWriterTest {
    private val course = Meeting(name = "测试课程", day = 2, startNode = 1, endNode = 2, weeks = listOf(5, 6, 9))
    private fun response(meetings: List<Meeting>, weeks: List<Int> = (1..28).toList()) =
        Extraction("schedule", "test-account", "test-term", meetings = meetings,
            coverageWeeks = weeks, full = true, emptyConfirmed = meetings.isEmpty())

    private class Store : SchoolCourseStore {
        val rows = linkedMapOf<Long, CourseEntity>()
        var ledger: String? = null
        var writes = 0
        private var nextId = 1L
        override suspend fun courses(scheduleId: Int) = rows.values.filter { it.scheduleId == scheduleId }
        override suspend fun ownership(scheduleId: Int) = ledger
        override suspend fun put(course: CourseEntity): Long {
            writes++
            val id = course.id.takeIf { it > 0 } ?: nextId++
            rows[id] = course.copy(id = id)
            return id
        }
        override suspend fun delete(id: Long) { writes++; rows.remove(id) }
        override suspend fun ownership(scheduleId: Int, value: String) { writes++; ledger = value }
    }

    @Test fun wholeEmptyCannotEraseSchoolCoursesOrChangeOwnership() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course)))
        val before = store.rows.toMap(); val ledger = store.ledger; val writes = store.writes
        try {
            ScvtcCourseWriter.write(store, 1, response(emptyList()))
            fail("Unproved whole-term empty result must be rejected before writes")
        } catch (_: IllegalArgumentException) { }
        assertEquals(before, store.rows); assertEquals(ledger, store.ledger); assertEquals(writes, store.writes)
    }

    @Test fun emptyWeekOnlyRemovesThatWeekAndKeepsManualEdits() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course, course.copy(name = "用户已改课程"))))
        val edited = store.rows.values.first { it.name == "用户已改课程" }
        store.rows[edited.id] = edited.copy(location = "手动教室", note = "保留备注")
        val manual = course.copy(name = "手动课程").nativeCourse(1).copy(id = 99)
        store.rows[manual.id] = manual
        ScvtcCourseWriter.write(store, 1, response(emptyList(), listOf(6)))
        assertEquals(listOf(5, 9), store.rows.values.single { it.name == course.name }.weeks)
        assertEquals(edited.copy(location = "手动教室", note = "保留备注"), store.rows[edited.id])
        assertEquals(manual, store.rows[99])
    }

    @Test fun changedWeekKeepsOccurrencesOutsideTheReadRange() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course)))
        ScvtcCourseWriter.write(store, 1, response(listOf(course.copy(room = "新教室", weeks = listOf(6))), listOf(6)))
        assertEquals(listOf(5, 9), store.rows.values.single { it.location == null }.weeks)
        assertEquals(listOf(6), store.rows.values.single { it.location == "新教室" }.weeks)
    }

    @Test fun invalidRowsNeverPartiallyCommit() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course)))
        val before = store.rows.toMap(); val ledger = store.ledger; val writes = store.writes
        try {
            ScvtcCourseWriter.write(store, 1, response(listOf(course.copy(name = "新课"), course.copy(weeks = emptyList()))))
            fail("Malformed row accepted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(before, store.rows); assertEquals(ledger, store.ledger); assertEquals(writes, store.writes)
    }

    @Test fun deletedCourseIsNotRecreatedAndPartialRowsAreNotRemoved() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course, course.copy(name = "其他课"))))
        val deleted = store.rows.values.first { it.name == course.name }
        store.rows.remove(deleted.id)
        val other = store.rows.values.single()
        ScvtcCourseWriter.write(store, 1, response(listOf(course.copy(weeks = listOf(6))), listOf(6)).copy(full = false))
        assertNull(store.rows[deleted.id])
        assertEquals(other, store.rows[other.id])
    }

    @Test fun allConfirmedSingleWeekCancellationsCanRemoveSchoolCourses() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course)))
        for (week in course.weeks) ScvtcCourseWriter.write(store, 1, response(emptyList(), listOf(week)))
        assertTrue(store.rows.isEmpty())
    }

    @Test fun partialReadCannotRecreateDeletedVariantAtANewListPosition() = runBlocking {
        val store = Store()
        ScvtcCourseWriter.write(store, 1, response(listOf(course.copy(weeks = listOf(5)), course.copy(weeks = listOf(6, 9)))))
        val deleted = store.rows.values.first { 9 in it.weeks }
        store.rows.remove(deleted.id)
        val retained = store.rows.values.single()
        ScvtcCourseWriter.write(store, 1, response(listOf(course.copy(weeks = listOf(9))), listOf(9)))
        assertEquals(listOf(retained), store.rows.values.toList())
        assertFalse(store.rows.values.any { 9 in it.weeks })
    }
}
