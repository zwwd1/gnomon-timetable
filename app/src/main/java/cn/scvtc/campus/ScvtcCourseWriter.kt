package cn.scvtc.campus

import cn.scvtc.campus.core.Extraction
import cn.scvtc.campus.core.ScheduleWriteGuard
import com.xiaomanjun.sleepdownschedule.model.CourseEntity
import org.json.JSONObject

/** The caller keeps course rows and their ownership ledger in one Room transaction. */
internal interface SchoolCourseStore {
    suspend fun courses(scheduleId: Int): List<CourseEntity>
    suspend fun ownership(scheduleId: Int): String?
    suspend fun put(course: CourseEntity): Long
    suspend fun delete(id: Long)
    suspend fun ownership(scheduleId: Int, value: String)
}

internal object ScvtcCourseWriter {
    fun fingerprint(course: CourseEntity) = scvtcKey(course.copy(id = 0).toString())
    private fun schoolKey(course: CourseEntity) = scvtcKey(listOf(course.name, course.teacher,
        course.location, course.weekday, course.periods).joinToString("|"))

    suspend fun write(store: SchoolCourseStore, id: Int, extraction: Extraction) {
        require(extraction.module == "schedule")
        ScheduleWriteGuard.validate(null, extraction)
        val covered = extraction.coverageWeeks.toSet()
        val incoming = extraction.meetings.map { it.nativeCourse(id) }
        val previous = store.ownership(id)?.let(::JSONObject) ?: JSONObject()
        val current = store.courses(id).associateBy { it.id }
        val next = JSONObject()
        // Older ledgers identify variants by sorted position, not stable school IDs.
        // A partial query can renumber them. Keep a user-owned arrangement together
        // rather than attributing a deleted/edited variant to a different row.
        val protected = previous.keys().asSequence().filter { key ->
            val saved = previous.getJSONObject(key)
            val before = current[saved.getLong("id")]
            before == null || fingerprint(before) != saved.getString("hash")
        }.map { it.substringBeforeLast(':') }.toSet()
        val groups = incoming.groupBy(::schoolKey)
        for ((base, rows) in groups) for ((index, row) in rows.sortedBy { it.weeks.joinToString(",") }.withIndex()) {
            if (base in protected) continue
            val item = "$base:$index"
            val saved = previous.optJSONObject(item)
            val before = saved?.optLong("id")?.let(current::get)
            if (saved != null && (before == null || fingerprint(before) != saved.optString("hash"))) {
                next.put(item, saved)
                continue
            }
            val duplicate = if (saved == null) current.values.firstOrNull {
                it.name == row.name && it.weekday == row.weekday && it.periods == row.periods &&
                    it.weeks.sorted() == row.weeks && it.teacher == row.teacher && it.location == row.location
            } else null
            if (duplicate != null) continue
            val retainedWeeks = before?.weeks.orEmpty().let { if (extraction.full) it - covered else it }
            val value = row.copy(id = before?.id ?: 0, weeks = (retainedWeeks + row.weeks).distinct().sorted())
            val rowId = store.put(value)
            next.put(item, JSONObject().put("id", rowId).put("hash", fingerprint(value.copy(id = rowId))))
        }
        previous.keys().forEach { key ->
            if (key.substringBeforeLast(':') in protected) next.put(key, previous.getJSONObject(key))
            if (!next.has(key)) {
                val saved = previous.getJSONObject(key)
                val before = current[saved.getLong("id")]
                if (before != null && fingerprint(before) == saved.getString("hash")) {
                    val weeks = if (extraction.full) before.weeks - covered else before.weeks
                    if (weeks.isEmpty()) store.delete(before.id)
                    else {
                        val retained = before.copy(weeks = weeks)
                        store.put(retained)
                        next.put(key, JSONObject().put("id", before.id).put("hash", fingerprint(retained)))
                    }
                } else next.put(key, saved)
            }
        }
        store.ownership(id, next.toString())
    }
}
