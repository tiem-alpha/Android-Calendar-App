package com.oneui.calendar.data.provider

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.oneui.calendar.data.local.CalendarDatabaseHelper
import com.oneui.calendar.domain.model.AccountGroupType
import com.oneui.calendar.domain.model.CalendarAccount
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.EventColorTag
import java.time.ZoneId

class SystemCalendarSyncManager(private val context: Context) {

    fun hasReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun syncFromSystemCalendars(dbHelper: CalendarDatabaseHelper): Int {
        if (!hasReadPermission()) return 0

        return runCatching {
            val resolver = context.contentResolver
            val externalToLocalCalId = mutableMapOf<Long, Long>()

            val calProjection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.VISIBLE
            )

            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                calProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val accNameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val dispNameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val visIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)

                while (cursor.moveToNext()) {
                    val extCalId = cursor.getLong(idIdx)
                    val accountName = cursor.getString(accNameIdx) ?: "Device"
                    val accountType = cursor.getString(accTypeIdx) ?: ""
                    val displayName = cursor.getString(dispNameIdx) ?: accountName
                    val colorInt = cursor.getInt(colorIdx)
                    val isVisible = cursor.getInt(visIdx) == 1

                    val existing = dbHelper.getAccountByExternalCalendarId(extCalId)
                    val account = CalendarAccount(
                        id = existing?.id ?: 0L,
                        accountName = accountName,
                        displayName = displayName,
                        groupType = mapAccountGroupType(accountType),
                        colorHex = (colorInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L,
                        isVisible = existing?.isVisible ?: isVisible,
                        externalCalendarId = extCalId
                    )
                    val localCalId = dbHelper.upsertAccount(account)
                    externalToLocalCalId[extCalId] = localCalId
                }
            }

            if (externalToLocalCalId.isEmpty()) return@runCatching 0

            val eventProjection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.HAS_ALARM
            )

            var syncedCount = 0
            resolver.query(
                CalendarContract.Events.CONTENT_URI,
                eventProjection,
                "${CalendarContract.Events.DELETED} != 1",
                null,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val calIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val tzIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_TIMEZONE)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
                val rruleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
                val alarmIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.HAS_ALARM)

                while (cursor.moveToNext()) {
                    val extEventId = cursor.getLong(idIdx)
                    val extCalId = cursor.getLong(calIdIdx)
                    val localCalId = externalToLocalCalId[extCalId] ?: continue
                    val title = cursor.getString(titleIdx)?.takeIf { it.isNotBlank() } ?: continue
                    val dtStart = if (cursor.isNull(startIdx)) continue else cursor.getLong(startIdx)
                    val isAllDay = cursor.getInt(allDayIdx) == 1
                    val rawDtEnd = if (cursor.isNull(endIdx)) 0L else cursor.getLong(endIdx)
                    val dtEnd = if (rawDtEnd > dtStart) {
                        rawDtEnd
                    } else if (isAllDay) {
                        dtStart + 86_400_000L
                    } else {
                        dtStart + 3_600_000L
                    }

                    val existingEvent = dbHelper.getEventByExternalId(extEventId)
                    val event = CalendarEvent(
                        id = existingEvent?.id ?: 0L,
                        calendarId = localCalId,
                        title = title,
                        location = cursor.getString(locIdx) ?: "",
                        description = cursor.getString(descIdx) ?: "",
                        startTimeMillis = dtStart,
                        endTimeMillis = dtEnd,
                        timeZone = cursor.getString(tzIdx) ?: ZoneId.systemDefault().id,
                        isAllDay = isAllDay,
                        isDDay = existingEvent?.isDDay ?: false,
                        isLunar = existingEvent?.isLunar ?: false,
                        rrule = cursor.getString(rruleIdx),
                        colorTag = existingEvent?.colorTag ?: pickColorTagForCalendar(localCalId),
                        stickerId = existingEvent?.stickerId,
                        hasAlarm = cursor.getInt(alarmIdx) == 1,
                        reminderMinutesBefore = existingEvent?.reminderMinutesBefore ?: 10,
                        externalEventId = extEventId
                    )
                    dbHelper.upsertEvent(event)
                    syncedCount++
                }
            }

            syncedCount
        }.getOrDefault(0)
    }

    fun pushEventToSystemCalendar(event: CalendarEvent): Long? {
        if (!hasWritePermission()) return null

        return runCatching {
            val resolver = context.contentResolver
            val targetSystemCalId = findWritableSystemCalendarId() ?: return@runCatching null

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, targetSystemCalId)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.startTimeMillis)
                put(CalendarContract.Events.DTEND, maxOf(event.endTimeMillis, event.startTimeMillis))
                put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
                if (!event.rrule.isNullOrBlank()) {
                    put(CalendarContract.Events.RRULE, event.rrule)
                } else {
                    putNull(CalendarContract.Events.RRULE)
                }
                put(CalendarContract.Events.HAS_ALARM, if (event.hasAlarm) 1 else 0)
            }

            val externalId = if (event.externalEventId != null && event.externalEventId > 0L) {
                val updateUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    event.externalEventId
                )
                val updated = resolver.update(updateUri, values, null, null)
                if (updated > 0) {
                    event.externalEventId
                } else {
                    resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                        ?.let { ContentUris.parseId(it) }
                }
            } else {
                resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    ?.let { ContentUris.parseId(it) }
            }

            if (externalId != null && event.hasAlarm) {
                resolver.delete(
                    CalendarContract.Reminders.CONTENT_URI,
                    "${CalendarContract.Reminders.EVENT_ID} = ?",
                    arrayOf(externalId.toString())
                )
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, externalId)
                    put(CalendarContract.Reminders.MINUTES, event.reminderMinutesBefore.coerceAtLeast(0))
                    put(
                        CalendarContract.Reminders.METHOD,
                        CalendarContract.Reminders.METHOD_ALERT
                    )
                }
                resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            externalId
        }.getOrNull()
    }

    private fun findWritableSystemCalendarId(): Long? {
        if (!hasReadPermission()) return null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection =
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }
        return null
    }

    private fun mapAccountGroupType(accountType: String): AccountGroupType {
        return when {
            accountType.contains("samsung", ignoreCase = true) -> AccountGroupType.SAMSUNG
            accountType.contains("google", ignoreCase = true) -> AccountGroupType.GOOGLE
            else -> AccountGroupType.MY_PHONE
        }
    }

    private fun pickColorTagForCalendar(localCalId: Long): EventColorTag {
        val tags = EventColorTag.entries
        return tags[(localCalId % tags.size).toInt()]
    }
}
