package com.oneui.calendar.core.common

import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.EventInstance
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object RRuleEvaluator {

    private data class ParsedRRule(
        val freq: String,
        val interval: Int = 1,
        val count: Int? = null,
        val untilMillis: Long? = null,
        val byDay: List<DayOfWeek> = emptyList()
    )

    private const val MAX_ITERATIONS = 10_000

    fun expandEvents(
        events: List<CalendarEvent>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<EventInstance> {
        if (rangeEnd.isBefore(rangeStart)) return emptyList()

        val result = mutableListOf<EventInstance>()
        for (event in events) {
            val safeEndMillis = maxOf(event.endTimeMillis, event.startTimeMillis)
            val startZdt = Instant.ofEpochMilli(event.startTimeMillis).atZone(zoneId)
            val endZdt = Instant.ofEpochMilli(safeEndMillis).atZone(zoneId)

            val baseStartDate = startZdt.toLocalDate()
            val baseEndDate = computeInclusiveEndDate(
                startDate = baseStartDate,
                endDate = endZdt.toLocalDate(),
                endTime = endZdt.toLocalTime(),
                hasPositiveDuration = safeEndMillis > event.startTimeMillis
            )
            val durationMillis = safeEndMillis - event.startTimeMillis
            val spanDays = ChronoUnit.DAYS.between(baseStartDate, baseEndDate).coerceAtLeast(0L)

            val rrule = event.rrule?.trim()?.takeIf { it.isNotEmpty() }
            if (rrule == null) {
                if (!baseEndDate.isBefore(rangeStart) && !baseStartDate.isAfter(rangeEnd)) {
                    result += EventInstance(
                        event = event,
                        instanceStartMillis = event.startTimeMillis,
                        instanceEndMillis = safeEndMillis,
                        startDate = baseStartDate,
                        endDate = baseEndDate
                    )
                }
                continue
            }

            val parsed = parseRRule(rrule, zoneId) ?: continue
            if (event.isLunar && parsed.freq == "YEARLY") {
                expandLunarYearly(
                    event = event,
                    parsed = parsed,
                    baseStartDate = baseStartDate,
                    baseStartTime = startZdt.toLocalTime(),
                    durationMillis = durationMillis,
                    spanDays = spanDays,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    zoneId = zoneId,
                    out = result
                )
            } else {
                expandStandardRRule(
                    event = event,
                    parsed = parsed,
                    baseStartDate = baseStartDate,
                    baseStartTime = startZdt.toLocalTime(),
                    durationMillis = durationMillis,
                    spanDays = spanDays,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    zoneId = zoneId,
                    out = result
                )
            }
        }

        return result.sortedWith(
            compareBy<EventInstance> { it.instanceStartMillis }
                .thenBy { it.instanceEndMillis }
                .thenBy { it.event.title }
        )
    }

    private fun computeInclusiveEndDate(
        startDate: LocalDate,
        endDate: LocalDate,
        endTime: LocalTime,
        hasPositiveDuration: Boolean
    ): LocalDate {
        val adjusted = if (hasPositiveDuration && endTime == LocalTime.MIDNIGHT && endDate.isAfter(startDate)) {
            endDate.minusDays(1)
        } else {
            endDate
        }
        return if (adjusted.isBefore(startDate)) startDate else adjusted
    }

    private fun expandLunarYearly(
        event: CalendarEvent,
        parsed: ParsedRRule,
        baseStartDate: LocalDate,
        baseStartTime: LocalTime,
        durationMillis: Long,
        spanDays: Long,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        zoneId: ZoneId,
        out: MutableList<EventInstance>
    ) {
        val baseLunar = LunarCalendarConverter.solarToLunar(baseStartDate)
        val maxYear = rangeEnd.year + 1
        var year = baseLunar.year
        var generated = 0
        var iterations = 0

        while (year <= maxYear && iterations < MAX_ITERATIONS) {
            iterations++
            val occStartDate = LunarCalendarConverter.lunarToSolar(
                lunarDay = baseLunar.day,
                lunarMonth = baseLunar.month,
                lunarYear = year,
                isLeapMonth = baseLunar.isLeapMonth
            )
            val occStartMillis = occStartDate.atTime(baseStartTime).atZone(zoneId).toInstant().toEpochMilli()
            if (parsed.untilMillis != null && occStartMillis > parsed.untilMillis) break

            generated++
            val occEndDate = occStartDate.plusDays(spanDays)
            if (!occEndDate.isBefore(rangeStart) && !occStartDate.isAfter(rangeEnd)) {
                out += EventInstance(
                    event = event,
                    instanceStartMillis = occStartMillis,
                    instanceEndMillis = occStartMillis + durationMillis,
                    startDate = occStartDate,
                    endDate = occEndDate
                )
            }

            if (parsed.count != null && generated >= parsed.count) break
            year += parsed.interval
        }
    }

    private fun expandStandardRRule(
        event: CalendarEvent,
        parsed: ParsedRRule,
        baseStartDate: LocalDate,
        baseStartTime: LocalTime,
        durationMillis: Long,
        spanDays: Long,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        zoneId: ZoneId,
        out: MutableList<EventInstance>
    ) {
        val effectiveStartWindow = rangeStart.minusDays(spanDays)
        var generatedCount = 0

        fun emitIfValid(occDate: LocalDate): Boolean {
            if (occDate.isBefore(baseStartDate)) return true
            val occStartMillis = occDate.atTime(baseStartTime).atZone(zoneId).toInstant().toEpochMilli()
            if (parsed.untilMillis != null && occStartMillis > parsed.untilMillis) return false

            generatedCount++
            val occEndDate = occDate.plusDays(spanDays)
            if (!occEndDate.isBefore(rangeStart) && !occDate.isAfter(rangeEnd)) {
                out += EventInstance(
                    event = event,
                    instanceStartMillis = occStartMillis,
                    instanceEndMillis = occStartMillis + durationMillis,
                    startDate = occDate,
                    endDate = occEndDate
                )
            }
            return parsed.count == null || generatedCount < parsed.count
        }

        when (parsed.freq) {
            "DAILY" -> {
                var current = baseStartDate
                if (parsed.count == null && current.isBefore(effectiveStartWindow)) {
                    val daysDiff = ChronoUnit.DAYS.between(current, effectiveStartWindow)
                    val steps = (daysDiff / parsed.interval).coerceAtLeast(0L)
                    current = current.plusDays(steps * parsed.interval)
                }
                var iterations = 0
                while (!current.isAfter(rangeEnd) && iterations < MAX_ITERATIONS) {
                    iterations++
                    if (!emitIfValid(current)) break
                    current = current.plusDays(parsed.interval.toLong())
                }
            }

            "WEEKLY" -> {
                val targetDays = if (parsed.byDay.isNotEmpty()) {
                    parsed.byDay.distinct()
                } else {
                    listOf(baseStartDate.dayOfWeek)
                }
                val baseWeekStart = baseStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                var weekStart = baseWeekStart
                if (parsed.count == null && weekStart.plusDays(6).isBefore(effectiveStartWindow)) {
                    val weeksDiff = ChronoUnit.WEEKS.between(weekStart, effectiveStartWindow)
                    val steps = (weeksDiff / parsed.interval).coerceAtLeast(0L)
                    weekStart = weekStart.plusWeeks(steps * parsed.interval)
                }
                var iterations = 0
                outer@ while (!weekStart.isAfter(rangeEnd) && iterations < MAX_ITERATIONS) {
                    iterations++
                    for (offset in 0..6) {
                        val candidate = weekStart.plusDays(offset.toLong())
                        if (candidate.isAfter(rangeEnd)) break@outer
                        if (candidate.dayOfWeek in targetDays) {
                            if (!emitIfValid(candidate)) break@outer
                        }
                    }
                    weekStart = weekStart.plusWeeks(parsed.interval.toLong())
                }
            }

            "MONTHLY" -> {
                var ym = YearMonth.from(baseStartDate)
                if (parsed.count == null && ym.atEndOfMonth().isBefore(effectiveStartWindow)) {
                    val monthsDiff = ChronoUnit.MONTHS.between(ym.atDay(1), effectiveStartWindow.withDayOfMonth(1))
                    val steps = (monthsDiff / parsed.interval).coerceAtLeast(0L)
                    ym = ym.plusMonths(steps * parsed.interval)
                }
                var iterations = 0
                while (!ym.atDay(1).isAfter(rangeEnd) && iterations < MAX_ITERATIONS) {
                    iterations++
                    val dayOfMonth = baseStartDate.dayOfMonth
                    if (dayOfMonth <= ym.lengthOfMonth()) {
                        val candidate = ym.atDay(dayOfMonth)
                        if (candidate.isAfter(rangeEnd)) break
                        if (!emitIfValid(candidate)) break
                    }
                    ym = ym.plusMonths(parsed.interval.toLong())
                }
            }

            "YEARLY" -> {
                var year = baseStartDate.year
                if (parsed.count == null && year < effectiveStartWindow.year) {
                    val yearsDiff = (effectiveStartWindow.year - year).toLong()
                    val steps = (yearsDiff / parsed.interval).coerceAtLeast(0L)
                    year += (steps * parsed.interval).toInt()
                }
                var iterations = 0
                while (year <= rangeEnd.year && iterations < MAX_ITERATIONS) {
                    iterations++
                    val ym = YearMonth.of(year, baseStartDate.month)
                    if (baseStartDate.dayOfMonth <= ym.lengthOfMonth()) {
                        val candidate = LocalDate.of(year, baseStartDate.month, baseStartDate.dayOfMonth)
                        if (candidate.isAfter(rangeEnd)) break
                        if (!emitIfValid(candidate)) break
                    }
                    year += parsed.interval
                }
            }
        }
    }

    private fun parseRRule(raw: String, zoneId: ZoneId): ParsedRRule? {
        val cleaned = raw.removePrefix("RRULE:")
        val map = mutableMapOf<String, String>()
        for (part in cleaned.split(";")) {
            val eqIdx = part.indexOf('=')
            if (eqIdx > 0) {
                val key = part.substring(0, eqIdx).trim().uppercase(Locale.US)
                val value = part.substring(eqIdx + 1).trim().uppercase(Locale.US)
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        val freq = map["FREQ"] ?: return null
        val interval = map["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val count = map["COUNT"]?.toIntOrNull()?.takeIf { it > 0 }
        val untilMillis = map["UNTIL"]?.let { parseUntilMillis(it, zoneId) }
        val byDay = map["BYDAY"]
            ?.split(",")
            ?.mapNotNull { parseDayOfWeek(it.trim()) }
            ?: emptyList()

        return ParsedRRule(
            freq = freq,
            interval = interval,
            count = count,
            untilMillis = untilMillis,
            byDay = byDay
        )
    }

    private fun parseDayOfWeek(token: String): DayOfWeek? {
        val suffix = if (token.length >= 2) token.takeLast(2) else return null
        return when (suffix) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            "SA" -> DayOfWeek.SATURDAY
            "SU" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun parseUntilMillis(raw: String, zoneId: ZoneId): Long? {
        return runCatching {
            when {
                raw.endsWith("Z") && raw.length == 16 -> {
                    val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
                    java.time.LocalDateTime.parse(raw, fmt).toInstant(ZoneOffset.UTC).toEpochMilli()
                }
                raw.contains("T") && raw.length == 15 -> {
                    val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)
                    java.time.LocalDateTime.parse(raw, fmt).atZone(zoneId).toInstant().toEpochMilli()
                }
                raw.length == 8 -> {
                    val date = LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE)
                    date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
                }
                raw.length == 10 && raw.contains("-") -> {
                    val date = LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
                    date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
                }
                else -> Instant.parse(raw).toEpochMilli()
            }
        }.getOrNull()
    }
}
