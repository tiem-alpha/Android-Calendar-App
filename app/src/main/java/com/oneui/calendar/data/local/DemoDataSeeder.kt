package com.oneui.calendar.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.oneui.calendar.data.local.CalendarDatabaseHelper.Companion.toContentValues
import com.oneui.calendar.data.local.CalendarDatabaseHelper.Companion.upsertEventInternal
import com.oneui.calendar.domain.model.AccountGroupType
import com.oneui.calendar.domain.model.CalendarAccount
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.DailyMetadata
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.ReminderItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

object DemoDataSeeder {

    val ANCHOR_TODAY: LocalDate = LocalDate.of(2026, 10, 24)

    fun seedInitialData(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // Fresh install starts completely clean with 0 pre-filled events/reminders/searches,
            // only creating the default local calendar account so user can immediately add events.
            val defaultAccount = CalendarAccount(
                id = 1L,
                accountName = "My phone",
                displayName = "My calendars",
                groupType = AccountGroupType.MY_PHONE,
                colorHex = 0xFFB39DFF,
                isVisible = true
            )
            db.insertWithOnConflict(
                CalendarDatabaseHelper.TABLE_ACCOUNTS,
                null,
                defaultAccount.toContentValues(includeId = true),
                SQLiteDatabase.CONFLICT_REPLACE
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun seedAccounts(db: SQLiteDatabase) {
        val accounts = listOf(
            CalendarAccount(
                id = 1L,
                accountName = "My phone",
                displayName = "My calendars",
                groupType = AccountGroupType.MY_PHONE,
                colorHex = 0xFFB39DFF,
                isVisible = true
            ),
            CalendarAccount(
                id = 2L,
                accountName = "My phone",
                displayName = "abc123@gmail.com",
                groupType = AccountGroupType.MY_PHONE,
                colorHex = 0xFF8E8E93,
                isVisible = false
            ),
            CalendarAccount(
                id = 3L,
                accountName = "abc123@gmail.com",
                displayName = "Samsung",
                groupType = AccountGroupType.SAMSUNG,
                colorHex = 0xFF0381FE,
                isVisible = true
            ),
            CalendarAccount(
                id = 4L,
                accountName = "abc123@gmail.com",
                displayName = "Google",
                groupType = AccountGroupType.GOOGLE,
                colorHex = 0xFF34A853,
                isVisible = true
            )
        )

        accounts.forEach { account ->
            db.insertWithOnConflict(
                CalendarDatabaseHelper.TABLE_ACCOUNTS,
                null,
                account.toContentValues(includeId = true),
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun seedRecentSearches(db: SQLiteDatabase) {
        // Ordered from newest to oldest so UI renders: Birthday, Design Review, Travel, Emily, Jejudo
        val searches = listOf("Birthday", "Design Review", "Travel", "Emily", "Jejudo")
        val baseTime = 1_792_800_000_000L
        searches.forEachIndexed { index, query ->
            val values = ContentValues().apply {
                put("query", query)
                put("updated_at_millis", baseTime - index * 60_000L)
            }
            db.insertWithOnConflict(
                CalendarDatabaseHelper.TABLE_SEARCH_HISTORY,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun seedDailyMetadata(db: SQLiteDatabase) {
        val metadataList = listOf(
            DailyMetadata(
                dateIso = "2026-10-22",
                stickerId = null,
                tempHighC = 14,
                tempLowC = 9,
                weatherCode = 0,
                weatherEmoji = "☀️"
            ),
            DailyMetadata(
                dateIso = "2026-10-24",
                stickerId = null,
                tempHighC = 12,
                tempLowC = 8,
                weatherCode = 0,
                weatherEmoji = "☀️"
            )
        )
        metadataList.forEach { item ->
            db.insertWithOnConflict(
                CalendarDatabaseHelper.TABLE_DAILY_METADATA,
                null,
                item.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun seedOctober2026Events(
        db: SQLiteDatabase,
        zone: ZoneId,
        timeZoneId: String
    ) {
        val events = listOf(
            // Oct 2
            allDayEvent(
                date = LocalDate.of(2026, 10, 2),
                title = "Calendar",
                colorTag = EventColorTag.PASTEL_MINT,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 2),
                start = LocalTime.of(9, 0),
                end = LocalTime.of(10, 0),
                title = "Yoga",
                colorTag = EventColorTag.PASTEL_CORAL,
                stickerId = "WORKOUT",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 6
            allDayEvent(
                date = LocalDate.of(2026, 10, 6),
                title = "Festival",
                colorTag = EventColorTag.PASTEL_BLUE,
                stickerId = "PARTY",
                calendarId = 3L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 8 - Oct 10
            multiDayAllDayEvent(
                startDate = LocalDate.of(2026, 10, 8),
                endDateInclusive = LocalDate.of(2026, 10, 10),
                title = "Design Sprint",
                colorTag = EventColorTag.PASTEL_CORAL,
                calendarId = 4L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            multiDayAllDayEvent(
                startDate = LocalDate.of(2026, 10, 8),
                endDateInclusive = LocalDate.of(2026, 10, 9),
                title = "Project Sync",
                colorTag = EventColorTag.PASTEL_MINT,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 11 - Oct 13
            multiDayAllDayEvent(
                startDate = LocalDate.of(2026, 10, 11),
                endDateInclusive = LocalDate.of(2026, 10, 13),
                title = "Camping",
                colorTag = EventColorTag.PASTEL_CORAL,
                stickerId = "CAMP",
                location = "Jejudo",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 15
            allDayEvent(
                date = LocalDate.of(2026, 10, 15),
                title = "Birthday",
                colorTag = EventColorTag.PASTEL_PINK,
                stickerId = "CAKE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 16
            allDayEvent(
                date = LocalDate.of(2026, 10, 16),
                title = "Meeting",
                colorTag = EventColorTag.PASTEL_BLUE,
                calendarId = 3L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 16),
                start = LocalTime.of(14, 0),
                end = LocalTime.of(15, 0),
                title = "Coffee",
                colorTag = EventColorTag.PASTEL_MINT,
                stickerId = "COFFEE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 18
            allDayEvent(
                date = LocalDate.of(2026, 10, 18),
                title = "Travel Plan",
                colorTag = EventColorTag.PASTEL_BLUE,
                stickerId = "FLIGHT",
                calendarId = 4L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 21 (Sunday in Week View)
            timedEvent(
                date = LocalDate.of(2026, 10, 21),
                start = LocalTime.of(12, 0),
                end = LocalTime.of(13, 0),
                title = "conference call",
                colorTag = EventColorTag.PASTEL_BLUE,
                calendarId = 3L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 21),
                start = LocalTime.of(15, 0),
                end = LocalTime.of(15, 45),
                title = "Team meeting",
                colorTag = EventColorTag.PASTEL_MINT,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 24 (Today in Screenshots 1..5)
            allDayEvent(
                date = LocalDate.of(2026, 10, 24),
                title = "Emily's Birthday",
                colorTag = EventColorTag.PASTEL_LAVENDER,
                isDDay = true,
                location = "Cafe",
                stickerId = "CAKE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            allDayEvent(
                date = LocalDate.of(2026, 10, 24),
                title = "Kate's birthday",
                colorTag = EventColorTag.PASTEL_BLUE,
                stickerId = "GIFT",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 24),
                start = LocalTime.of(10, 0),
                end = LocalTime.of(10, 30),
                title = "Team meeting",
                colorTag = EventColorTag.PASTEL_MINT,
                hasAlarm = true,
                reminderMinutesBefore = 10,
                calendarId = 3L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 24),
                start = LocalTime.of(12, 0),
                end = LocalTime.of(13, 30),
                title = "Lunch with YK",
                colorTag = EventColorTag.PASTEL_PINK,
                location = "Cafe",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 24),
                start = LocalTime.of(13, 0),
                end = LocalTime.of(14, 0),
                title = "Lunch with a friend",
                colorTag = EventColorTag.PASTEL_CORAL,
                location = "Downtown",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            allDayEvent(
                date = LocalDate.of(2026, 10, 24),
                title = "3pm Movie with Jake",
                colorTag = EventColorTag.PASTEL_PINK,
                stickerId = "LOVE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 24),
                start = LocalTime.of(15, 0),
                end = LocalTime.of(16, 0),
                title = "3pm Movie with Emily",
                colorTag = EventColorTag.PASTEL_MINT,
                calendarId = 4L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 26
            allDayEvent(
                date = LocalDate.of(2026, 10, 26),
                title = "Calendar",
                colorTag = EventColorTag.PASTEL_BLUE,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            allDayEvent(
                date = LocalDate.of(2026, 10, 26),
                title = "B-day",
                colorTag = EventColorTag.PASTEL_MINT,
                stickerId = "CAKE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 26),
                start = LocalTime.of(11, 0),
                end = LocalTime.of(12, 0),
                title = "Brunch",
                colorTag = EventColorTag.PASTEL_LAVENDER,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 26),
                start = LocalTime.of(14, 0),
                end = LocalTime.of(15, 0),
                title = "Coffee",
                colorTag = EventColorTag.PASTEL_BLUE,
                stickerId = "COFFEE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 27 (Saturday in Week View with +3 cluster at 12:30 - 13:30)
            timedEvent(
                date = LocalDate.of(2026, 10, 27),
                start = LocalTime.of(12, 30),
                end = LocalTime.of(13, 0),
                title = "coffee",
                colorTag = EventColorTag.PASTEL_LAVENDER,
                hasAlarm = true,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 27),
                start = LocalTime.of(12, 45),
                end = LocalTime.of(13, 30),
                title = "Yoga class",
                colorTag = EventColorTag.PASTEL_YELLOW,
                stickerId = "WORKOUT",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 27),
                start = LocalTime.of(12, 30),
                end = LocalTime.of(13, 15),
                title = "Design Review",
                colorTag = EventColorTag.PASTEL_BLUE,
                calendarId = 4L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 27),
                start = LocalTime.of(12, 45),
                end = LocalTime.of(13, 30),
                title = "Book Club",
                colorTag = EventColorTag.PASTEL_PINK,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 27),
                start = LocalTime.of(13, 0),
                end = LocalTime.of(13, 30),
                title = "Grocery pickup",
                colorTag = EventColorTag.PASTEL_MINT,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Oct 30
            timedEvent(
                date = LocalDate.of(2026, 10, 30),
                start = LocalTime.of(10, 0),
                end = LocalTime.of(11, 30),
                title = "Design",
                colorTag = EventColorTag.PASTEL_MINT,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = LocalDate.of(2026, 10, 30),
                start = LocalTime.of(12, 30),
                end = LocalTime.of(13, 30),
                title = "Lunch",
                colorTag = EventColorTag.PASTEL_CORAL,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),

            // Nov 2, 2026
            allDayEvent(
                date = LocalDate.of(2026, 11, 2),
                title = "Party",
                colorTag = EventColorTag.PASTEL_LAVENDER,
                stickerId = "PARTY",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            )
        )

        events.forEach { event ->
            upsertEventInternal(db, event)
        }
    }

    private fun seedCurrentMonthSupplementalIfNeeded(
        db: SQLiteDatabase,
        zone: ZoneId,
        timeZoneId: String
    ) {
        val deviceToday = LocalDate.now(zone)
        val anchorMonth = YearMonth.of(2026, 10)
        val currentMonth = YearMonth.from(deviceToday)
        if (currentMonth == anchorMonth) return

        // Avoid touching Nov 2, 2026 if currentMonth happens to be 2026-11
        val todayMetadata = DailyMetadata(
            dateIso = deviceToday.toString(),
            stickerId = null,
            tempHighC = 14,
            tempLowC = 9,
            weatherCode = 0,
            weatherEmoji = "☀️"
        )
        db.insertWithOnConflict(
            CalendarDatabaseHelper.TABLE_DAILY_METADATA,
            null,
            todayMetadata.toContentValues(),
            SQLiteDatabase.CONFLICT_IGNORE
        )

        val currentEvents = listOf(
            timedEvent(
                date = deviceToday,
                start = LocalTime.of(10, 0),
                end = LocalTime.of(11, 0),
                title = "Weekly Sync",
                colorTag = EventColorTag.PASTEL_MINT,
                hasAlarm = true,
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            ),
            timedEvent(
                date = deviceToday,
                start = LocalTime.of(14, 0),
                end = LocalTime.of(15, 0),
                title = "Coffee Break",
                colorTag = EventColorTag.PASTEL_BLUE,
                stickerId = "COFFEE",
                calendarId = 1L,
                zone = zone,
                timeZoneId = timeZoneId
            )
        )
        currentEvents.forEach { event ->
            upsertEventInternal(db, event)
        }
    }

    private fun seedReminders(db: SQLiteDatabase, zone: ZoneId) {
        val reminders = listOf(
            ReminderItem(
                eventId = null,
                title = "Prepare slides for Team meeting",
                triggerTimeMillis = LocalDate.of(2026, 10, 24)
                    .atTime(9, 30)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli(),
                isCompleted = false,
                colorTag = EventColorTag.PASTEL_MINT
            ),
            ReminderItem(
                eventId = null,
                title = "Buy birthday gift for Emily",
                triggerTimeMillis = LocalDate.of(2026, 10, 24)
                    .atTime(11, 0)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli(),
                isCompleted = false,
                colorTag = EventColorTag.PASTEL_LAVENDER
            ),
            ReminderItem(
                eventId = null,
                title = "Book camping gear for Jejudo",
                triggerTimeMillis = LocalDate.of(2026, 10, 25)
                    .atTime(14, 0)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli(),
                isCompleted = true,
                colorTag = EventColorTag.PASTEL_CORAL
            ),
            ReminderItem(
                eventId = null,
                title = "Call dentist",
                triggerTimeMillis = LocalDate.of(2026, 10, 26)
                    .atTime(16, 0)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli(),
                isCompleted = false,
                colorTag = EventColorTag.PASTEL_BLUE
            )
        )

        reminders.forEach { item ->
            db.insert(CalendarDatabaseHelper.TABLE_REMINDERS, null, item.toContentValues())
        }
    }

    private fun allDayEvent(
        date: LocalDate,
        title: String,
        colorTag: EventColorTag,
        calendarId: Long,
        zone: ZoneId,
        timeZoneId: String,
        location: String = "",
        stickerId: String? = null,
        isDDay: Boolean = false
    ): CalendarEvent {
        return multiDayAllDayEvent(
            startDate = date,
            endDateInclusive = date,
            title = title,
            colorTag = colorTag,
            calendarId = calendarId,
            zone = zone,
            timeZoneId = timeZoneId,
            location = location,
            stickerId = stickerId,
            isDDay = isDDay
        )
    }

    private fun multiDayAllDayEvent(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        title: String,
        colorTag: EventColorTag,
        calendarId: Long,
        zone: ZoneId,
        timeZoneId: String,
        location: String = "",
        stickerId: String? = null,
        isDDay: Boolean = false
    ): CalendarEvent {
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endDateInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return CalendarEvent(
            calendarId = calendarId,
            title = title,
            location = location,
            description = "",
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            timeZone = timeZoneId,
            isAllDay = true,
            isDDay = isDDay,
            isLunar = false,
            rrule = null,
            colorTag = colorTag,
            stickerId = stickerId,
            hasAlarm = false,
            reminderMinutesBefore = 10
        )
    }

    private fun timedEvent(
        date: LocalDate,
        start: LocalTime,
        end: LocalTime,
        title: String,
        colorTag: EventColorTag,
        calendarId: Long,
        zone: ZoneId,
        timeZoneId: String,
        location: String = "",
        stickerId: String? = null,
        hasAlarm: Boolean = false,
        reminderMinutesBefore: Int = 10
    ): CalendarEvent {
        val startMillis = date.atTime(start).atZone(zone).toInstant().toEpochMilli()
        val endMillis = date.atTime(end).atZone(zone).toInstant().toEpochMilli()
        return CalendarEvent(
            calendarId = calendarId,
            title = title,
            location = location,
            description = "",
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            timeZone = timeZoneId,
            isAllDay = false,
            isDDay = false,
            isLunar = false,
            rrule = null,
            colorTag = colorTag,
            stickerId = stickerId,
            hasAlarm = hasAlarm,
            reminderMinutesBefore = reminderMinutesBefore
        )
    }
}
