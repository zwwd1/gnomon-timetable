package com.xiaomanjun.sleepdownschedule.feature.importing

import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.domain.schedule.normalizeCourseClock

import android.content.Context
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object IcsScheduleCodec {
    private val CalendarZone = ZoneId.of("Asia/Shanghai")
    private val compactDate = DateTimeFormatter.BASIC_ISO_DATE
    private val compactDateTime = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val compactDateTimeMinutes = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
    private val displayTime = DateTimeFormatter.ofPattern("HH:mm")

    fun parse(bytes: ByteArray, baseConfig: ScheduleConfigEntity): Result<ImportDraft> = runCatching {
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        require(text.contains("BEGIN:VCALENDAR", ignoreCase = true)) { "所选文件不是有效的 ICS 日历" }
        val metadata = parseSleepDownMetadata(text)
        val events = parseEvents(text)
        require(events.isNotEmpty()) { "ICS 中没有找到日历事件" }

        var expandedCount = 0
        val expanded = events.map { event ->
            expandEvent(event).also { occurrences ->
                expandedCount += occurrences.size
                require(expandedCount <= 20_000) { "展开后的日历超过 20000 个事件，请按学期拆分后导入" }
            }
        }
        val ignored = expanded.count { it.isEmpty() }
        val occurrences = expanded.flatten().distinctBy {
            listOf(it.name, it.location, it.description, it.start, it.end, it.customPeriodTimes)
        }
        require(occurrences.isNotEmpty()) { "ICS 中没有可导入的定时课程事件" }

        val firstMonday = (metadata.termStartDate ?: occurrences.minOf { it.start.toLocalDate() })
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val rawLastWeek = occurrences.maxOf { occurrence ->
            ChronoUnit.WEEKS.between(firstMonday, occurrence.start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))).toInt() + 1
        }
        val totalWeeks = (metadata.totalWeeks ?: rawLastWeek).coerceIn(1, 60)
        val usable = occurrences.filter { occurrence ->
            val week = ChronoUnit.WEEKS.between(
                firstMonday,
                occurrence.start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ).toInt() + 1
            week in 1..totalWeeks
        }

        val timeRanges = usable
            .map { it.start.toLocalTime() to it.end.toLocalTime() }
            .distinct()
            .sortedWith(compareBy<Pair<LocalTime, LocalTime>> { it.first }.thenBy { it.second })
        require(timeRanges.isNotEmpty()) { "ICS 中没有可识别的课程时间" }
        val periodIndexByRange = timeRanges.withIndex().associate { it.value to it.index + 1 }
        val inferredPeriods = timeRanges.mapIndexed { index, range ->
            PeriodEntity(
                periodIndex = index + 1,
                startTime = range.first.format(displayTime),
                endTime = range.second.format(displayTime),
                scheduleId = baseConfig.id
            )
        }
        val periods = metadata.periods.ifEmpty { inferredPeriods }
            .distinctBy { it.periodIndex }
            .sortedBy { it.periodIndex }
            .map { it.copy(scheduleId = baseConfig.id) }
        val periodsByIndex = periods.associateBy { it.periodIndex }

        data class CourseKey(
            val name: String,
            val teacher: String?,
            val location: String?,
            val weekday: Int,
            val sourceIdentity: String,
            val note: String?,
            val periods: List<Int>,
            val start: LocalTime,
            val end: LocalTime,
            val customPeriodTimes: String?
        )

        val grouped = usable.groupBy { occurrence ->
            val teacher = extractTeacher(occurrence.description)
            val embeddedPeriods = occurrence.periodIndices
                .filter { it in periodsByIndex }
                .distinct()
                .sorted()
            val inferredPeriod = periodIndexByRange[occurrence.start.toLocalTime() to occurrence.end.toLocalTime()]
            val anchors = embeddedPeriods.ifEmpty {
                listOfNotNull(inferredPeriod).ifEmpty {
                    courseAnchorPeriodsForTimeRange(occurrence.start.toLocalTime(), occurrence.end.toLocalTime(), periods)
                }
            }
            CourseKey(
                name = occurrence.name.ifBlank { "未命名课程" },
                teacher = teacher,
                location = occurrence.location.takeIf { it.isNotBlank() },
                weekday = occurrence.start.dayOfWeek.value,
                sourceIdentity = occurrence.sourceCourseId ?: "generic-${anchors.joinToString(",")}",
                note = occurrence.description
                    .takeIf { it.isNotBlank() }
                    ?.takeUnless { description -> teacher != null && description.trim() == "教师：$teacher" },
                periods = anchors,
                start = occurrence.start.toLocalTime(),
                end = occurrence.end.toLocalTime(),
                customPeriodTimes = occurrence.customPeriodTimes
            )
        }
        val courses = grouped.map { (key, values) ->
            val weeks = values.map { occurrence ->
                ChronoUnit.WEEKS.between(
                    firstMonday,
                    occurrence.start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ).toInt() + 1
            }.filter { it in 1..totalWeeks }.distinct().sorted()
            val standardStart = key.periods.firstOrNull()?.let(periodsByIndex::get)?.startTime
            val standardEnd = key.periods.lastOrNull()?.let(periodsByIndex::get)?.endTime
            val hasDistinctTime = key.start.format(displayTime) != standardStart ||
                key.end.format(displayTime) != standardEnd
            val clock = normalizeCourseClock(
                key.start.format(displayTime).takeIf { hasDistinctTime || key.customPeriodTimes != null },
                key.end.format(displayTime).takeIf { hasDistinctTime || key.customPeriodTimes != null },
                key.customPeriodTimes,
                key.periods
            )
            CourseEntity(
                name = key.name,
                teacher = key.teacher,
                location = key.location,
                weekday = key.weekday,
                periods = key.periods,
                weeks = weeks,
                weekParity = WeekParity.ALL,
                note = key.note,
                customStartTime = clock.start,
                customEndTime = clock.end,
                customPeriodTimes = clock.periodTimes,
                scheduleId = baseConfig.id
            )
        }.filter { it.weeks.isNotEmpty() }
            .sortedWith(compareBy<CourseEntity> { it.weekday }.thenBy { it.periods.firstOrNull() ?: 0 }.thenBy { it.name })
        require(courses.isNotEmpty()) { "ICS 中没有可导入的课程" }

        val termStart = metadata.termStartDate ?: firstMonday
        val today = LocalDate.now()
        val detectedWeek = (ChronoUnit.DAYS.between(termStart, today).toInt() / 7 + 1).coerceIn(1, totalWeeks)
        val inferredCounts = inferPeriodCounts(periods)
        val counts = metadata.partCounts?.takeIf {
            it.morning + it.noon + it.afternoon + it.evening == periods.size
        } ?: inferredCounts
        ImportDraft(
            config = baseConfig.copy(
                totalWeeks = totalWeeks,
                currentWeek = detectedWeek,
                termStartDate = termStart.toString(),
                autoCurrentWeek = true,
                morningPeriodCount = counts.morning,
                noonPeriodCount = counts.noon,
                afternoonPeriodCount = counts.afternoon,
                eveningPeriodCount = counts.evening
            ),
            periods = periods,
            courses = courses,
            warnings = listOfNotNull(if (ignored > 0) "跳过 $ignored 个取消、全天或缺少起止时间的事件" else null,
                if (rawLastWeek > 60) "只导入前 60 周的事件" else null)
        )
    }

    data class ExportReport(val content: String, val events: Int, val skipped: Int)

    fun export(calendarName: String, config: ScheduleConfigEntity, periods: List<PeriodEntity>,
        courses: List<CourseEntity>, today: LocalDate = LocalDate.now()): String =
        exportReport(calendarName, config, periods, courses, today).content

    fun exportReport(calendarName: String, config: ScheduleConfigEntity, periods: List<PeriodEntity>,
        courses: List<CourseEntity>, today: LocalDate = LocalDate.now()): ExportReport {
        val termStart = parseScheduleDate(config.termStartDate)
            ?: throw IllegalArgumentException("请先设置学期开始日期，再导出日历")
        val effective = config.copy(autoCurrentWeek = true)
        val state = AppState(config = effective, courses = courses, periods = periods)
        val firstMonday = termStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val periodsByIndex = periods.associateBy { it.periodIndex }
        var count = 0
        var skipped = 0
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC).format(Instant.now())
        val events = buildString {
            repeat(config.totalWeeks.coerceIn(1, 60) * 7) { offset ->
                val date = firstMonday.plusDays(offset.toLong())
                val teachingDate = com.xiaomanjun.sleepdownschedule.domain.schedule.teachingDateForSchedule(effective, date)
                coursesForDate(state, date).forEach { course ->
                    val ranges = course.customTimeRangeOrNull()?.let { (start, end) ->
                        listOf(IcsPeriodRange(course.periods.distinct().sorted(), start, end))
                    } ?: if (course.periods.isNotEmpty() && course.periods.all { index ->
                        periodsByIndex[index]?.let { p ->
                            val start = runCatching { LocalTime.parse(p.startTime) }.getOrNull()
                            val end = runCatching { LocalTime.parse(p.endTime) }.getOrNull()
                            start != null && end != null && end.isAfter(start)
                        } == true
                    }) contiguousPeriodRanges(course.periods, periodsByIndex) else emptyList()
                    if (ranges.isEmpty()) skipped++
                    ranges.forEachIndexed { rangeIndex, range ->
                        val identity = if (course.id > 0) course.id.toString() else
                            java.util.UUID.nameUUIDFromBytes(listOf(course.name, course.teacher, course.location,
                                course.weekday, course.periods).joinToString("|").toByteArray()).toString()
                        appendLine("BEGIN:VEVENT")
                        appendLine("UID:gnomon-${config.id}-$identity-$date-$rangeIndex@gnomon.local")
                        appendLine("DTSTAMP:$stamp")
                        // UTC is unambiguous without a proprietary or incomplete VTIMEZONE definition.
                        appendLine("DTSTART:${date.atTime(range.start).atZone(CalendarZone).withZoneSameInstant(ZoneOffset.UTC).format(compactDateTime)}Z")
                        appendLine("DTEND:${date.atTime(range.end).atZone(CalendarZone).withZoneSameInstant(ZoneOffset.UTC).format(compactDateTime)}Z")
                        appendLine("SUMMARY:${escapeText(course.name)}")
                        appendLine("X-SLEEPDOWN-COURSE-ID:${course.id}")
                        course.location?.takeIf(String::isNotBlank)?.let { appendLine("LOCATION:${escapeText(it)}") }
                        val description = listOfNotNull(course.teacher?.takeIf(String::isNotBlank)?.let { "教师：$it" },
                            course.note?.takeIf(String::isNotBlank)).joinToString("\n")
                        if (description.isNotBlank()) appendLine("DESCRIPTION:${escapeText(description)}")
                        appendLine("X-SLEEPDOWN-WEEK:${teachingDate?.let { effectiveCurrentWeek(effective, it) } ?: effectiveCurrentWeek(effective, date)}")
                        appendLine("X-SLEEPDOWN-PERIODS:${range.indices.joinToString(",")}")
                        course.customPeriodTimes?.let { appendLine("X-SLEEPDOWN-PERIOD-TIMES:$it") }
                        appendLine("END:VEVENT")
                        count++
                    }
                }
            }
        }
        val content = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Gnomon Timetable//Course Schedule//CN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            appendLine("X-WR-CALNAME:${escapeText(calendarName)}")
            appendLine("X-WR-TIMEZONE:${CalendarZone.id}")
            appendLine("X-SLEEPDOWN-SCHEMA:2")
            appendLine("X-SLEEPDOWN-TOTAL-WEEKS:${config.totalWeeks}")
            appendLine("X-SLEEPDOWN-TERM-START:$termStart")
            appendLine("X-SLEEPDOWN-PART-COUNTS:${config.morningPeriodCount},${config.noonPeriodCount},${config.afternoonPeriodCount},${config.eveningPeriodCount}")
            appendLine("X-GNOMON-SKIPPED:$skipped")
            periods.sortedBy { it.periodIndex }.forEach {
                appendLine("X-SLEEPDOWN-PERIOD:${it.periodIndex},${it.startTime},${it.endTime}")
            }
            append(events)
            appendLine("END:VCALENDAR")
        }
        return ExportReport(foldCalendar(content), count, skipped)
    }

    private fun foldCalendar(text: String): String = buildString {
        text.lineSequence().filter(String::isNotEmpty).forEach { line ->
            var bytes = 0
            line.codePoints().toArray().forEach { code ->
                val character = String(Character.toChars(code))
                val size = character.toByteArray(Charsets.UTF_8).size
                if (bytes + size > 75) { append("\r\n "); bytes = 1 }
                append(character); bytes += size
            }
            append("\r\n")
        }
    }
    fun writeShareFile(
        context: Context,
        calendarName: String,
        config: ScheduleConfigEntity,
        periods: List<PeriodEntity>,
        courses: List<CourseEntity>
    ): File {
        val directory = File(context.cacheDir, "shared_schedules").apply { mkdirs() }
        val safeName = calendarName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Gnomon 课程表" }
        return File(directory, "$safeName.ics").apply {
            writeText(export(calendarName, config, periods, courses), Charsets.UTF_8)
        }
    }

    private data class IcsProperty(val name: String, val parameters: Map<String, String>, val value: String)
    private data class IcsEvent(val properties: Map<String, List<IcsProperty>>)
    private data class Occurrence(
        val name: String,
        val location: String,
        val description: String,
        val start: LocalDateTime,
        val end: LocalDateTime,
        val periodIndices: List<Int> = emptyList(),
        val sourceCourseId: String? = null,
        val customPeriodTimes: String? = null
    )

    private data class SleepDownMetadata(
        val totalWeeks: Int? = null,
        val termStartDate: LocalDate? = null,
        val partCounts: PeriodPartCounts? = null,
        val periods: List<PeriodEntity> = emptyList()
    )

    private data class IcsPeriodRange(val indices: List<Int>, val start: LocalTime, val end: LocalTime)

    private fun unfoldLines(text: String): List<String> {
        val unfolded = mutableListOf<String>()
        text.replace("\r\n", "\n").replace('\r', '\n').lineSequence().forEach { line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] += line.drop(1)
            } else {
                unfolded += line
            }
        }
        return unfolded
    }

    private fun parseSleepDownMetadata(text: String): SleepDownMetadata {
        val properties = unfoldLines(text).mapNotNull(::parseProperty)
        val totalWeeks = properties.firstOrNull { it.name == "X-SLEEPDOWN-TOTAL-WEEKS" }?.value?.toIntOrNull()
        val termStart = properties.firstOrNull { it.name == "X-SLEEPDOWN-TERM-START" }?.value
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val counts = properties.firstOrNull { it.name == "X-SLEEPDOWN-PART-COUNTS" }?.value
            ?.split(',')?.mapNotNull(String::toIntOrNull)
            ?.takeIf { it.size == 4 }
            ?.let { PeriodPartCounts(it[0], it[1], it[2], it[3]) }
        val periods = properties.filter { it.name == "X-SLEEPDOWN-PERIOD" }.mapNotNull { property ->
            val values = property.value.split(',')
            val index = values.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val start = values.getOrNull(1)?.takeIf { runCatching { LocalTime.parse(it) }.isSuccess } ?: return@mapNotNull null
            val end = values.getOrNull(2)?.takeIf { runCatching { LocalTime.parse(it) }.isSuccess } ?: return@mapNotNull null
            PeriodEntity(index, start, end)
        }
        return SleepDownMetadata(totalWeeks, termStart, counts, periods)
    }

    private fun parseEvents(text: String): List<IcsEvent> {
        val unfolded = unfoldLines(text)
        val events = mutableListOf<IcsEvent>()
        var current: MutableMap<String, MutableList<IcsProperty>>? = null
        unfolded.forEach { line ->
            when (line.trim().uppercase()) {
                "BEGIN:VEVENT" -> current = linkedMapOf()
                "END:VEVENT" -> current?.let { values ->
                    events += IcsEvent(values.mapValues { it.value.toList() })
                    current = null
                }
                else -> current?.let { values ->
                    parseProperty(line)?.let { property ->
                        values.getOrPut(property.name) { mutableListOf() } += property
                    }
                }
            }
        }
        return events
    }

    private fun parseProperty(line: String): IcsProperty? {
        val separator = line.indexOf(':')
        if (separator <= 0) return null
        val header = line.substring(0, separator).split(';')
        val name = header.first().uppercase()
        val parameters = header.drop(1).mapNotNull { part ->
            val equals = part.indexOf('=')
            if (equals <= 0) null else part.substring(0, equals).uppercase() to part.substring(equals + 1).trim('"')
        }.toMap()
        return IcsProperty(name, parameters, line.substring(separator + 1))
    }

    private fun expandEvent(event: IcsEvent): List<Occurrence> {
        if (event.properties["STATUS"]?.firstOrNull()?.value.equals("CANCELLED", true)) return emptyList()
        require(event.properties["RECURRENCE-ID"].isNullOrEmpty()) { "包含单次重复例外，请先在日历软件中展开事件后再导入" }
        val startProperty = event.properties["DTSTART"]?.firstOrNull() ?: return emptyList()
        val sourceZone = propertyZone(startProperty)
        val start = parseDateTime(startProperty, sourceZone) ?: return emptyList()
        val end = event.properties["DTEND"]?.firstOrNull()?.let { parseDateTime(it, sourceZone) }
            ?: event.properties["DURATION"]?.firstOrNull()?.value?.let { value ->
                runCatching { start.plus(Duration.parse(value)) }.getOrNull()
            }
            ?: return emptyList()
        if (!end.isAfter(start)) return emptyList()
        if (end.toLocalDate() != start.toLocalDate()) return emptyList()
        val duration = Duration.between(start, end)
        val exclusions = event.properties["EXDATE"].orEmpty().flatMap { property ->
            property.value.split(',').mapNotNull { value -> parseDateTime(property.copy(value = value), sourceZone) }
        }.toSet()
        val additions = event.properties["RDATE"].orEmpty().flatMap { property ->
            property.value.split(',').mapNotNull { value -> parseDateTime(property.copy(value = value), sourceZone) }
        }
        val recurrence = event.properties["RRULE"]?.firstOrNull()?.value?.split(';')
            ?.mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].uppercase() to it[1] } }
            ?.toMap()
        require(recurrence == null || (recurrence["FREQ"]?.uppercase() in setOf("DAILY", "WEEKLY") &&
            recurrence.keys.all { it in setOf("FREQ", "INTERVAL", "COUNT", "UNTIL", "BYDAY", "WKST") } &&
            (recurrence["WKST"] == null || recurrence["WKST"] == "MO"))) { "目前支持日 / 周重复；其他规则请先在日历软件展开为独立事件" }
        recurrence?.get("COUNT")?.let { require(it.toIntOrNull() in 1..1000) { "重复次数须在 1–1000 之间" } }
        recurrence?.get("INTERVAL")?.let { require(it.toIntOrNull() in 1..365) { "重复间隔须在 1–365 之间" } }
        recurrence?.get("BYDAY")?.let { value -> require(value.split(',').all { it in setOf("MO", "TU", "WE", "TH", "FR", "SA", "SU") }) { "暂不支持带序号的重复星期" } }
        val starts = when (recurrence?.get("FREQ")?.uppercase()) {
            "WEEKLY" -> expandWeekly(start, recurrence, sourceZone)
            "DAILY" -> expandDaily(start, recurrence, sourceZone)
            else -> listOf(start)
        }.plus(additions).distinct().filterNot { it in exclusions }.sorted()
        val name = unescapeText(event.properties["SUMMARY"]?.firstOrNull()?.value.orEmpty()).ifBlank { "未命名课程" }
        val location = unescapeText(event.properties["LOCATION"]?.firstOrNull()?.value.orEmpty())
        val description = unescapeText(event.properties["DESCRIPTION"]?.firstOrNull()?.value.orEmpty())
        val embeddedPeriods = event.properties["X-SLEEPDOWN-PERIODS"]?.firstOrNull()?.value
            ?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty()
        val sourceCourseId = event.properties["X-SLEEPDOWN-COURSE-ID"]?.firstOrNull()?.value?.trim()?.takeIf { it.isNotBlank() }
        val customPeriodTimes = event.properties["X-SLEEPDOWN-PERIOD-TIMES"]?.firstOrNull()?.value
        return starts.take(1000).map { occurrenceStart ->
            val occurrence = Occurrence(name, location, description,
                occurrenceStart.atZone(sourceZone).withZoneSameInstant(CalendarZone).toLocalDateTime(),
                occurrenceStart.plus(duration).atZone(sourceZone).withZoneSameInstant(CalendarZone).toLocalDateTime(),
                embeddedPeriods, sourceCourseId, customPeriodTimes)
            require(occurrence.start.toLocalDate() == occurrence.end.toLocalDate()) {
                "事件换算为北京时间后跨越零点，请在日历软件中拆分后导入"
            }
            occurrence
        }
    }

    private fun expandWeekly(start: LocalDateTime, rule: Map<String, String>, zone: ZoneId): List<LocalDateTime> {
        val interval = rule["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val count = rule["COUNT"]?.toIntOrNull()?.coerceIn(1, 1000)
        val until = rule["UNTIL"]?.let { parseRuleUntil(it, start, zone) }
        val weekdays = rule["BYDAY"]?.split(',')?.mapNotNull(::parseWeekday)?.distinct()?.sortedBy { it.value }
            .orEmpty().ifEmpty { listOf(start.dayOfWeek) }
        val firstWeek = start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val output = mutableListOf<LocalDateTime>()
        var weekOffset = 0L
        while (output.size < (count ?: 1000) && weekOffset < 60L * interval) {
            val week = firstWeek.plusWeeks(weekOffset)
            weekdays.forEach { weekday ->
                val candidate = week.plusDays((weekday.value - 1).toLong()).atTime(start.toLocalTime())
                if (!candidate.isBefore(start) && (until == null || !candidate.isAfter(until)) && output.size < (count ?: 1000)) {
                    output += candidate
                }
            }
            if (until != null && week.atStartOfDay().isAfter(until)) break
            weekOffset += interval
        }
        return output
    }

    private fun expandDaily(start: LocalDateTime, rule: Map<String, String>, zone: ZoneId): List<LocalDateTime> {
        val interval = rule["INTERVAL"]?.toLongOrNull()?.coerceAtLeast(1) ?: 1L
        val count = rule["COUNT"]?.toIntOrNull()?.coerceIn(1, 1000)
        val until = rule["UNTIL"]?.let { parseRuleUntil(it, start, zone) }
        val weekdays = rule["BYDAY"]?.split(',')?.mapNotNull(::parseWeekday)?.toSet().orEmpty()
        val output = mutableListOf<LocalDateTime>()
        var candidate = start
        var checked = 0
        while (output.size < (count ?: 1000) && checked < 420) {
            if ((weekdays.isEmpty() || candidate.dayOfWeek in weekdays) && (until == null || !candidate.isAfter(until))) output += candidate
            if (until != null && candidate.isAfter(until)) break
            candidate = candidate.plusDays(interval)
            checked++
        }
        return output
    }

    private fun propertyZone(property: IcsProperty): ZoneId = if (property.value.endsWith('Z', true)) ZoneOffset.UTC
        else property.parameters["TZID"]?.let { ZoneId.of(it) } ?: CalendarZone

    private fun parseDateTime(property: IcsProperty, targetZone: ZoneId = CalendarZone): LocalDateTime? {
        val value = property.value.trim()
        if (property.parameters["VALUE"].equals("DATE", ignoreCase = true) || (!value.contains('T') && value.length == 8)) return null
        val zone = propertyZone(property)
        val utc = value.endsWith('Z', ignoreCase = true)
        val raw = if (utc) value.dropLast(1) else value
        val local = runCatching { LocalDateTime.parse(raw, compactDateTime) }
            .recoverCatching { LocalDateTime.parse(raw, compactDateTimeMinutes) }
            .getOrNull() ?: return null
        return local.atZone(zone).withZoneSameInstant(targetZone).toLocalDateTime()
    }

    private fun parseRuleUntil(value: String, reference: LocalDateTime, zone: ZoneId): LocalDateTime? {
        if (!value.contains('T') && value.length == 8) {
            return runCatching { LocalDate.parse(value, compactDate).atTime(LocalTime.MAX) }.getOrNull()
        }
        return parseDateTime(IcsProperty("UNTIL", mapOf("TZID" to zone.id), value), zone) ?: reference
    }

    private fun parseWeekday(value: String): DayOfWeek? = when (value.trim().takeLast(2).uppercase()) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun contiguousPeriodRanges(
        indexes: List<Int>,
        periodsByIndex: Map<Int, PeriodEntity>
    ): List<IcsPeriodRange> {
        val sorted = indexes.distinct().sorted().mapNotNull { index -> periodsByIndex[index]?.let { index to it } }
        if (sorted.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<Pair<Int, PeriodEntity>>>()
        sorted.forEach { value ->
            val current = groups.lastOrNull()
            if (current == null || value.first != current.last().first + 1) groups += mutableListOf(value) else current += value
        }
        return groups.mapNotNull { group ->
            val start = runCatching { LocalTime.parse(group.first().second.startTime) }.getOrNull()
            val end = runCatching { LocalTime.parse(group.last().second.endTime) }.getOrNull()
            if (start != null && end != null && end.isAfter(start)) IcsPeriodRange(group.map { it.first }, start, end) else null
        }
    }

    private fun extractTeacher(description: String): String? {
        return Regex("(?:教师|老师|授课教师)\\s*[:：]\\s*([^\\n;,，；]+)")
            .find(description)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun unescapeText(value: String): String = value
        .replace("\\n", "\n", ignoreCase = true)
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
        .trim()

    private fun escapeText(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")
}
