package com.oneui.calendar.core.common

import com.oneui.calendar.domain.model.EventInstance
import com.oneui.calendar.domain.model.NlpConflictPreview
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NlpTimeAndConflictDetector {

    private val TIME_DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.US)

    private val RANGE_12H_OR_24H_REGEX = Regex(
        """(?i)\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s*(?:-|~|to)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b"""
    )
    private val SINGLE_12H_REGEX = Regex(
        """(?i)\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b"""
    )
    private val SINGLE_24H_REGEX = Regex(
        """\b([01]?\d|2[0-3]):([0-5]\d)\b"""
    )

    private val STOP_WORDS = setOf(
        "with", "at", "the", "a", "an", "in", "on", "for", "to", "and", "or", "of", "by"
    )

    fun analyzeTitleInput(
        rawTitle: String,
        targetDate: LocalDate,
        sameDayInstances: List<EventInstance>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): NlpConflictPreview? {
        val trimmed = rawTitle.trim()
        if (trimmed.isEmpty()) return null

        val parsed = parseTimeExpression(trimmed) ?: return null

        val startZdt = targetDate.atTime(parsed.startTime).atZone(zoneId)
        var endZdt = targetDate.atTime(parsed.endTime).atZone(zoneId)
        if (!endZdt.isAfter(startZdt)) {
            endZdt = startZdt.plusHours(1)
        }

        val suggestedStartMillis = startZdt.toInstant().toEpochMilli()
        val suggestedEndMillis = endZdt.toInstant().toEpochMilli()
        val detectedTimeRangeText =
            "${TIME_DISPLAY_FORMATTER.format(startZdt.toLocalTime())} ~ ${TIME_DISPLAY_FORMATTER.format(endZdt.toLocalTime())}"

        val inputKeywords = extractKeywords(
            trimmed.replace(parsed.matchedPhrase, " ")
        )

        val scoredConflicts = sameDayInstances.mapNotNull { instance ->
            val eventKeywords = extractKeywords(instance.event.title)
            val sharesKeyword = inputKeywords.isNotEmpty() && inputKeywords.any { it in eventKeywords }
            val overlapsTime = !instance.event.isAllDay &&
                instance.instanceStartMillis < suggestedEndMillis &&
                instance.instanceEndMillis > suggestedStartMillis
            val isAllDay = instance.event.isAllDay

            if (!sharesKeyword && !overlapsTime && !isAllDay) {
                null
            } else {
                val score = (if (sharesKeyword) 4 else 0) +
                    (if (overlapsTime) 2 else 0) +
                    (if (isAllDay) 1 else 0)
                instance to score
            }
        }.sortedWith(
            compareByDescending<Pair<EventInstance, Int>> { it.second }
                .thenBy { it.first.instanceStartMillis }
                .thenBy { it.first.event.title }
        ).map { it.first }

        return NlpConflictPreview(
            detectedTimeRangeText = detectedTimeRangeText,
            suggestedStartMillis = suggestedStartMillis,
            suggestedEndMillis = suggestedEndMillis,
            matchedPhrase = parsed.matchedPhrase,
            conflictingEvents = scoredConflicts
        )
    }

    private data class ParsedTimeRange(
        val startTime: LocalTime,
        val endTime: LocalTime,
        val matchedPhrase: String
    )

    private fun parseTimeExpression(input: String): ParsedTimeRange? {
        RANGE_12H_OR_24H_REGEX.find(input)?.let { match ->
            val h1 = match.groupValues[1].toIntOrNull() ?: return@let
            val m1 = match.groupValues[2].toIntOrNull() ?: 0
            val amPm1 = match.groupValues[3].lowercase(Locale.US).ifEmpty { null }
            val h2 = match.groupValues[4].toIntOrNull() ?: return@let
            val m2 = match.groupValues[5].toIntOrNull() ?: 0
            val amPm2 = match.groupValues[6].lowercase(Locale.US).ifEmpty { null }

            if (amPm1 != null || amPm2 != null) {
                val effectiveAmPm2 = amPm2 ?: amPm1
                val effectiveAmPm1 = amPm1 ?: inferStartAmPm(h1, h2, effectiveAmPm2)
                val startTime = toLocalTime12(h1, m1, effectiveAmPm1) ?: return@let
                val endTime = toLocalTime12(h2, m2, effectiveAmPm2) ?: return@let
                return ParsedTimeRange(startTime, endTime, match.value.trim())
            } else if (match.groupValues[2].isNotEmpty() && match.groupValues[5].isNotEmpty()) {
                val startTime = toLocalTime24(h1, m1) ?: return@let
                val endTime = toLocalTime24(h2, m2) ?: return@let
                return ParsedTimeRange(startTime, endTime, match.value.trim())
            }
        }

        SINGLE_12H_REGEX.find(input)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase(Locale.US)
            val startTime = toLocalTime12(hour, minute, amPm) ?: return@let
            val endTime = startTime.plusHours(1)
            return ParsedTimeRange(startTime, endTime, match.value.trim())
        }

        SINGLE_24H_REGEX.find(input)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val minute = match.groupValues[2].toIntOrNull() ?: return@let
            val startTime = toLocalTime24(hour, minute) ?: return@let
            val endTime = startTime.plusHours(1)
            return ParsedTimeRange(startTime, endTime, match.value.trim())
        }

        return null
    }

    private fun inferStartAmPm(h1: Int, h2: Int, endAmPm: String?): String? {
        if (endAmPm == null) return null
        return if (endAmPm == "pm" && h1 > h2 && h1 != 12) "am" else endAmPm
    }

    private fun toLocalTime12(hour: Int, minute: Int, amPm: String?): LocalTime? {
        if (hour !in 1..12 || minute !in 0..59 || amPm == null) return null
        val hour24 = when {
            amPm == "am" && hour == 12 -> 0
            amPm == "pm" && hour == 12 -> 12
            amPm == "pm" -> hour + 12
            else -> hour
        }
        return LocalTime.of(hour24, minute)
    }

    private fun toLocalTime24(hour: Int, minute: Int): LocalTime? {
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    private fun extractKeywords(text: String): Set<String> {
        return text.lowercase(Locale.US)
            .split(Regex("""[^a-z0-9]+"""))
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .toSet()
    }
}
