package com.oneui.calendar.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CalendarViewMode(val label: String) {
    YEAR("Year"),
    MONTH("Month"),
    WEEK("Week"),
    DAY("Day"),
    REMINDER("Reminder")
}

enum class EventColorTag(
    val id: String,
    val displayName: String,
    val lightBgHex: Long,
    val lightBarHex: Long,
    val darkBgHex: Long,
    val darkBarHex: Long
) {
    PASTEL_CORAL("PASTEL_CORAL", "Coral Peach", 0xFFFAD2C8, 0xFFE87A5D, 0xFF38201B, 0xFFF59B82),
    PASTEL_MINT("PASTEL_MINT", "Mint Sage", 0xFFDCE8B5, 0xFF8AB833, 0xFF1E2B16, 0xFFA8D96C),
    PASTEL_BLUE("PASTEL_BLUE", "Mist Blue", 0xFFC8DDF8, 0xFF5B9BF3, 0xFF162438, 0xFF84B9FF),
    PASTEL_PINK("PASTEL_PINK", "Blush Pink", 0xFFFCD3D7, 0xFFEC6F86, 0xFF361A20, 0xFFF593A6),
    PASTEL_LAVENDER("PASTEL_LAVENDER", "Soft Lavender", 0xFFE2D6F8, 0xFF966FD6, 0xFF231B36, 0xFFBFA2F7),
    PASTEL_YELLOW("PASTEL_YELLOW", "Butter Yellow", 0xFFFDE8B2, 0xFFE5A91A, 0xFF332914, 0xFFF5C85B);

    companion object {
        fun fromId(id: String?): EventColorTag {
            if (id.isNullOrBlank()) return PASTEL_BLUE
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PASTEL_BLUE
        }
    }
}

enum class CalendarSticker(
    val id: String,
    val emoji: String,
    val label: String
) {
    CAKE("CAKE", "🎂", "Birthday Cake"),
    PARTY("PARTY", "🎉", "Party Popper"),
    LOVE("LOVE", "💌", "Love Letter"),
    GIFT("GIFT", "🎁", "Gift Box"),
    CAMP("CAMP", "⛺", "Camping Tent"),
    SMILE("SMILE", "😊", "Happy Day"),
    COFFEE("COFFEE", "☕", "Coffee"),
    WORKOUT("WORKOUT", "🧘", "Yoga / Workout"),
    FLIGHT("FLIGHT", "✈️", "Travel"),
    STAR("STAR", "⭐", "Important");

    companion object {
        fun fromId(id: String?): CalendarSticker? {
            if (id.isNullOrBlank()) return null
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}

enum class AccountGroupType {
    MY_PHONE,
    SAMSUNG,
    GOOGLE
}

data class CalendarAccount(
    val id: Long,
    val accountName: String,
    val displayName: String,
    val groupType: AccountGroupType,
    val colorHex: Long,
    val isVisible: Boolean,
    val externalCalendarId: Long? = null
)

data class CalendarEvent(
    val id: Long = 0L,
    val calendarId: Long = 1L,
    val title: String,
    val location: String = "",
    val description: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val timeZone: String = "Asia/Ho_Chi_Minh",
    val isAllDay: Boolean = false,
    val isDDay: Boolean = false,
    val isLunar: Boolean = false,
    val rrule: String? = null,
    val colorTag: EventColorTag = EventColorTag.PASTEL_BLUE,
    val stickerId: String? = null,
    val hasAlarm: Boolean = false,
    val reminderMinutesBefore: Int = 10,
    val externalEventId: Long? = null
)

data class EventInstance(
    val event: CalendarEvent,
    val instanceStartMillis: Long,
    val instanceEndMillis: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val isMultiDay: Boolean
        get() = endDate.isAfter(startDate)

    val isBannerEvent: Boolean
        get() = event.isAllDay || isMultiDay

    fun formattedTimeRange(): String {
        if (event.isAllDay) return "All day"
        val zone = resolveZone()
        val startTime = Instant.ofEpochMilli(instanceStartMillis).atZone(zone).toLocalTime()
        val endTime = Instant.ofEpochMilli(instanceEndMillis).atZone(zone).toLocalTime()
        return "${TIME_RANGE_FORMATTER.format(startTime)} - ${TIME_RANGE_FORMATTER.format(endTime)}"
    }

    fun formattedStartTimeShort(): String {
        if (event.isAllDay) return ""
        val zone = resolveZone()
        val startTime = Instant.ofEpochMilli(instanceStartMillis).atZone(zone).toLocalTime()
        return TIME_SHORT_FORMATTER.format(startTime)
    }

    fun dDayBadgeText(referenceDate: LocalDate): String {
        val daysUntil = ChronoUnit.DAYS.between(referenceDate, startDate)
        return when {
            daysUntil == 0L -> "D-Day"
            daysUntil > 0L -> "D-$daysUntil"
            else -> "D+${-daysUntil}"
        }
    }

    private fun resolveZone(): ZoneId {
        return runCatching { ZoneId.of(event.timeZone) }
            .getOrDefault(ZoneId.systemDefault())
    }

    private companion object {
        val TIME_RANGE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val TIME_SHORT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.US)
    }
}

data class ReminderItem(
    val id: Long = 0L,
    val eventId: Long? = null,
    val title: String,
    val triggerTimeMillis: Long,
    val isCompleted: Boolean = false,
    val colorTag: EventColorTag = EventColorTag.PASTEL_BLUE
)

data class DailyMetadata(
    val dateIso: String,
    val stickerId: String? = null,
    val tempHighC: Int = 12,
    val tempLowC: Int = 8,
    val weatherCode: Int = 0,
    val weatherEmoji: String = "☀️"
)

data class NlpConflictPreview(
    val detectedTimeRangeText: String,
    val suggestedStartMillis: Long,
    val suggestedEndMillis: Long,
    val matchedPhrase: String,
    val conflictingEvents: List<EventInstance>
)
