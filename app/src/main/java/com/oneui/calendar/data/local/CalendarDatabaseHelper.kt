package com.oneui.calendar.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.oneui.calendar.domain.model.AccountGroupType
import com.oneui.calendar.domain.model.CalendarAccount
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.DailyMetadata
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.ReminderItem

class CalendarDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_ACCOUNTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_name TEXT NOT NULL,
                display_name TEXT NOT NULL,
                group_type TEXT NOT NULL,
                color_hex INTEGER NOT NULL,
                is_visible INTEGER NOT NULL DEFAULT 1,
                external_calendar_id INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                calendar_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                location TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                start_time_millis INTEGER NOT NULL,
                end_time_millis INTEGER NOT NULL,
                time_zone TEXT NOT NULL,
                is_all_day INTEGER NOT NULL DEFAULT 0,
                is_d_day INTEGER NOT NULL DEFAULT 0,
                is_lunar INTEGER NOT NULL DEFAULT 0,
                rrule TEXT,
                color_tag TEXT NOT NULL,
                sticker_id TEXT,
                has_alarm INTEGER NOT NULL DEFAULT 0,
                reminder_minutes_before INTEGER NOT NULL DEFAULT 10,
                external_event_id INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE VIRTUAL TABLE $TABLE_EVENTS_FTS USING fts4(
                event_id,
                title,
                location,
                description
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_REMINDERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id INTEGER,
                title TEXT NOT NULL,
                trigger_time_millis INTEGER NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                color_tag TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DAILY_METADATA (
                date_iso TEXT PRIMARY KEY,
                sticker_id TEXT,
                temp_high_c INTEGER NOT NULL DEFAULT 12,
                temp_low_c INTEGER NOT NULL DEFAULT 8,
                weather_code INTEGER NOT NULL DEFAULT 0,
                weather_emoji TEXT NOT NULL DEFAULT '☀️'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SEARCH_HISTORY (
                query TEXT PRIMARY KEY,
                updated_at_millis INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX idx_events_start_time ON $TABLE_EVENTS(start_time_millis)")
        db.execSQL("CREATE INDEX idx_events_calendar_id ON $TABLE_EVENTS(calendar_id)")
        db.execSQL("CREATE INDEX idx_events_external_id ON $TABLE_EVENTS(external_event_id)")

        DemoDataSeeder.seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS_FTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACCOUNTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REMINDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_METADATA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SEARCH_HISTORY")
        onCreate(db)
    }

    // -------------------------------------------------------------------------
    // Calendar Accounts CRUD
    // -------------------------------------------------------------------------

    fun getAllAccounts(): List<CalendarAccount> {
        val list = mutableListOf<CalendarAccount>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_ACCOUNTS ORDER BY id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list += cursor.toCalendarAccount()
            }
        }
        return list
    }

    fun getAccountByExternalCalendarId(externalCalendarId: Long): CalendarAccount? {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_ACCOUNTS WHERE external_calendar_id = ? LIMIT 1",
            arrayOf(externalCalendarId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toCalendarAccount() else null
        }
    }

    fun upsertAccount(account: CalendarAccount): Long {
        val db = writableDatabase
        val values = account.toContentValues(includeId = account.id > 0L)
        return if (account.id > 0L && existsById(db, TABLE_ACCOUNTS, account.id)) {
            values.remove("id")
            db.update(TABLE_ACCOUNTS, values, "id = ?", arrayOf(account.id.toString()))
            account.id
        } else {
            if (account.id <= 0L) values.remove("id")
            db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun toggleAccountVisibility(accountId: Long) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_ACCOUNTS SET is_visible = CASE WHEN is_visible = 1 THEN 0 ELSE 1 END WHERE id = ?",
            arrayOf(accountId)
        )
    }

    // -------------------------------------------------------------------------
    // Events CRUD & FTS4 Search
    // -------------------------------------------------------------------------

    fun getAllEvents(): List<CalendarEvent> {
        val list = mutableListOf<CalendarEvent>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_EVENTS ORDER BY start_time_millis ASC, id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list += cursor.toCalendarEvent()
            }
        }
        return list
    }

    fun getEventById(eventId: Long): CalendarEvent? {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_EVENTS WHERE id = ? LIMIT 1",
            arrayOf(eventId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toCalendarEvent() else null
        }
    }

    fun getEventByExternalId(externalEventId: Long): CalendarEvent? {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_EVENTS WHERE external_event_id = ? LIMIT 1",
            arrayOf(externalEventId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toCalendarEvent() else null
        }
    }

    fun upsertEvent(event: CalendarEvent): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val savedId = upsertEventInternal(db, event)
            db.setTransactionSuccessful()
            savedId
        } finally {
            db.endTransaction()
        }
    }

    fun deleteEvent(eventId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_EVENTS, "id = ?", arrayOf(eventId.toString()))
            db.delete(TABLE_EVENTS_FTS, "event_id = ?", arrayOf(eventId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateEventAlarm(eventId: Long, enabled: Boolean) {
        val values = ContentValues().apply {
            put("has_alarm", if (enabled) 1 else 0)
        }
        writableDatabase.update(TABLE_EVENTS, values, "id = ?", arrayOf(eventId.toString()))
    }

    fun searchEvents(
        query: String? = null,
        stickerId: String? = null,
        colorTag: EventColorTag? = null
    ): List<CalendarEvent> {
        val trimmedQuery = query?.trim().orEmpty()
        val hasQuery = trimmedQuery.isNotEmpty()
        val hasSticker = !stickerId.isNullOrBlank()
        val hasColor = colorTag != null

        if (!hasQuery && !hasSticker && !hasColor) {
            return getAllEvents()
        }

        val matchingIdsFromFts: Set<Long>? = if (hasQuery) {
            queryEventIdsViaFtsOrLike(trimmedQuery)
        } else {
            null
        }

        if (matchingIdsFromFts != null && matchingIdsFromFts.isEmpty()) {
            return emptyList()
        }

        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (matchingIdsFromFts != null) {
            val placeholders = matchingIdsFromFts.joinToString(",") { "?" }
            whereClauses += "id IN ($placeholders)"
            matchingIdsFromFts.forEach { args += it.toString() }
        }
        if (hasSticker && stickerId != null) {
            whereClauses += "sticker_id = ?"
            args += stickerId
        }
        if (hasColor && colorTag != null) {
            whereClauses += "color_tag = ?"
            args += colorTag.id
        }

        val whereSql = if (whereClauses.isNotEmpty()) {
            "WHERE ${whereClauses.joinToString(" AND ")}"
        } else {
            ""
        }

        val results = mutableListOf<CalendarEvent>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_EVENTS $whereSql ORDER BY start_time_millis ASC, id ASC",
            args.toTypedArray()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                results += cursor.toCalendarEvent()
            }
        }
        return results
    }

    private fun queryEventIdsViaFtsOrLike(rawQuery: String): Set<Long> {
        val ids = LinkedHashSet<Long>()
        val ftsMatchExpression = buildFtsPrefixExpression(rawQuery)
        if (ftsMatchExpression != null) {
            runCatching {
                readableDatabase.rawQuery(
                    "SELECT event_id FROM $TABLE_EVENTS_FTS WHERE $TABLE_EVENTS_FTS MATCH ?",
                    arrayOf(ftsMatchExpression)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        cursor.getString(0)?.toLongOrNull()?.let { ids += it }
                    }
                }
            }
        }

        // Also match substrings safely so partial words (e.g. "view" in "Review") always match.
        val likePattern = "%$rawQuery%"
        readableDatabase.rawQuery(
            """
            SELECT id FROM $TABLE_EVENTS
            WHERE title LIKE ? COLLATE NOCASE
               OR location LIKE ? COLLATE NOCASE
               OR description LIKE ? COLLATE NOCASE
            """.trimIndent(),
            arrayOf(likePattern, likePattern, likePattern)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                ids += cursor.getLong(0)
            }
        }

        return ids
    }

    private fun buildFtsPrefixExpression(rawQuery: String): String? {
        val tokens = rawQuery
            .replace(Regex("[^\\p{L}\\p{N}_\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }

    // -------------------------------------------------------------------------
    // Reminders CRUD
    // -------------------------------------------------------------------------

    fun getAllReminders(): List<ReminderItem> {
        val list = mutableListOf<ReminderItem>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_REMINDERS ORDER BY is_completed ASC, trigger_time_millis ASC, id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list += cursor.toReminderItem()
            }
        }
        return list
    }

    fun upsertReminder(reminder: ReminderItem): Long {
        val db = writableDatabase
        val values = reminder.toContentValues()
        return if (reminder.id > 0L && existsById(db, TABLE_REMINDERS, reminder.id)) {
            db.update(TABLE_REMINDERS, values, "id = ?", arrayOf(reminder.id.toString()))
            reminder.id
        } else {
            db.insert(TABLE_REMINDERS, null, values)
        }
    }

    fun toggleReminderCompleted(reminderId: Long) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_REMINDERS SET is_completed = CASE WHEN is_completed = 1 THEN 0 ELSE 1 END WHERE id = ?",
            arrayOf(reminderId)
        )
    }

    fun deleteReminders(ids: Set<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        val args = ids.map { it.toString() }.toTypedArray()
        writableDatabase.delete(TABLE_REMINDERS, "id IN ($placeholders)", args)
    }

    // -------------------------------------------------------------------------
    // Daily Metadata (Weather + Day Sticker) CRUD
    // -------------------------------------------------------------------------

    fun getAllDailyMetadata(): Map<String, DailyMetadata> {
        val map = LinkedHashMap<String, DailyMetadata>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_DAILY_METADATA ORDER BY date_iso ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val item = cursor.toDailyMetadata()
                map[item.dateIso] = item
            }
        }
        return map
    }

    fun getDailyMetadata(dateIso: String): DailyMetadata? {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_DAILY_METADATA WHERE date_iso = ? LIMIT 1",
            arrayOf(dateIso)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toDailyMetadata() else null
        }
    }

    fun upsertDailyMetadata(metadata: DailyMetadata) {
        writableDatabase.insertWithOnConflict(
            TABLE_DAILY_METADATA,
            null,
            metadata.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun upsertDailyMetadataBatch(items: List<DailyMetadata>) {
        if (items.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (item in items) {
                val existingSticker = getDailyMetadataWithDb(db, item.dateIso)?.stickerId
                val merged = if (item.stickerId == null && existingSticker != null) {
                    item.copy(stickerId = existingSticker)
                } else {
                    item
                }
                db.insertWithOnConflict(
                    TABLE_DAILY_METADATA,
                    null,
                    merged.toContentValues(),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setDaySticker(dateIso: String, stickerId: String?) {
        val existing = getDailyMetadata(dateIso) ?: DailyMetadata(dateIso = dateIso)
        upsertDailyMetadata(existing.copy(stickerId = stickerId))
    }

    // -------------------------------------------------------------------------
    // Search History CRUD
    // -------------------------------------------------------------------------

    fun getRecentSearches(limit: Int = 12): List<String> {
        val list = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT query FROM $TABLE_SEARCH_HISTORY ORDER BY updated_at_millis DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list += cursor.getString(0)
            }
        }
        return list
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val values = ContentValues().apply {
            put("query", trimmed)
            put("updated_at_millis", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_SEARCH_HISTORY,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun removeSearchQuery(query: String) {
        writableDatabase.delete(TABLE_SEARCH_HISTORY, "query = ?", arrayOf(query))
    }

    fun clearAllSearchHistory() {
        writableDatabase.delete(TABLE_SEARCH_HISTORY, null, null)
    }

    // -------------------------------------------------------------------------
    // Internal Helpers (Shared with DemoDataSeeder)
    // -------------------------------------------------------------------------

    private fun getDailyMetadataWithDb(db: SQLiteDatabase, dateIso: String): DailyMetadata? {
        db.rawQuery(
            "SELECT * FROM $TABLE_DAILY_METADATA WHERE date_iso = ? LIMIT 1",
            arrayOf(dateIso)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toDailyMetadata() else null
        }
    }

    private fun existsById(db: SQLiteDatabase, table: String, id: Long): Boolean {
        db.rawQuery("SELECT 1 FROM $table WHERE id = ? LIMIT 1", arrayOf(id.toString())).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    companion object {
        private const val DATABASE_NAME = "oneui_calendar.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_ACCOUNTS = "calendar_accounts"
        const val TABLE_EVENTS = "events"
        const val TABLE_EVENTS_FTS = "events_fts"
        const val TABLE_REMINDERS = "reminders"
        const val TABLE_DAILY_METADATA = "daily_metadata"
        const val TABLE_SEARCH_HISTORY = "search_history"

        internal fun upsertEventInternal(db: SQLiteDatabase, event: CalendarEvent): Long {
            val values = event.toContentValues()
            val exists = event.id > 0L && db.rawQuery(
                "SELECT 1 FROM $TABLE_EVENTS WHERE id = ? LIMIT 1",
                arrayOf(event.id.toString())
            ).use { it.moveToFirst() }

            val eventId = if (exists) {
                db.update(TABLE_EVENTS, values, "id = ?", arrayOf(event.id.toString()))
                event.id
            } else {
                db.insertOrThrow(TABLE_EVENTS, null, values)
            }

            syncFtsRow(db, eventId, event.title, event.location, event.description)
            return eventId
        }

        internal fun syncFtsRow(
            db: SQLiteDatabase,
            eventId: Long,
            title: String,
            location: String,
            description: String
        ) {
            db.delete(TABLE_EVENTS_FTS, "event_id = ?", arrayOf(eventId.toString()))
            val ftsValues = ContentValues().apply {
                put("event_id", eventId.toString())
                put("title", title)
                put("location", location)
                put("description", description)
            }
            db.insert(TABLE_EVENTS_FTS, null, ftsValues)
        }

        internal fun CalendarAccount.toContentValues(includeId: Boolean = true): ContentValues {
            return ContentValues().apply {
                if (includeId && id > 0L) put("id", id)
                put("account_name", accountName)
                put("display_name", displayName)
                put("group_type", groupType.name)
                put("color_hex", colorHex)
                put("is_visible", if (isVisible) 1 else 0)
                if (externalCalendarId != null) {
                    put("external_calendar_id", externalCalendarId)
                } else {
                    putNull("external_calendar_id")
                }
            }
        }

        internal fun CalendarEvent.toContentValues(): ContentValues {
            return ContentValues().apply {
                put("calendar_id", calendarId)
                put("title", title)
                put("location", location)
                put("description", description)
                put("start_time_millis", startTimeMillis)
                put("end_time_millis", endTimeMillis)
                put("time_zone", timeZone)
                put("is_all_day", if (isAllDay) 1 else 0)
                put("is_d_day", if (isDDay) 1 else 0)
                put("is_lunar", if (isLunar) 1 else 0)
                if (rrule != null) put("rrule", rrule) else putNull("rrule")
                put("color_tag", colorTag.id)
                if (stickerId != null) put("sticker_id", stickerId) else putNull("sticker_id")
                put("has_alarm", if (hasAlarm) 1 else 0)
                put("reminder_minutes_before", reminderMinutesBefore)
                if (externalEventId != null) {
                    put("external_event_id", externalEventId)
                } else {
                    putNull("external_event_id")
                }
            }
        }

        internal fun ReminderItem.toContentValues(): ContentValues {
            return ContentValues().apply {
                if (eventId != null) put("event_id", eventId) else putNull("event_id")
                put("title", title)
                put("trigger_time_millis", triggerTimeMillis)
                put("is_completed", if (isCompleted) 1 else 0)
                put("color_tag", colorTag.id)
            }
        }

        internal fun DailyMetadata.toContentValues(): ContentValues {
            return ContentValues().apply {
                put("date_iso", dateIso)
                if (stickerId != null) put("sticker_id", stickerId) else putNull("sticker_id")
                put("temp_high_c", tempHighC)
                put("temp_low_c", tempLowC)
                put("weather_code", weatherCode)
                put("weather_emoji", weatherEmoji)
            }
        }

        private fun Cursor.toCalendarAccount(): CalendarAccount {
            val groupRaw = getString(getColumnIndexOrThrow("group_type"))
            val group = runCatching { AccountGroupType.valueOf(groupRaw) }
                .getOrDefault(AccountGroupType.MY_PHONE)
            val extIdx = getColumnIndexOrThrow("external_calendar_id")
            return CalendarAccount(
                id = getLong(getColumnIndexOrThrow("id")),
                accountName = getString(getColumnIndexOrThrow("account_name")),
                displayName = getString(getColumnIndexOrThrow("display_name")),
                groupType = group,
                colorHex = getLong(getColumnIndexOrThrow("color_hex")),
                isVisible = getInt(getColumnIndexOrThrow("is_visible")) == 1,
                externalCalendarId = if (isNull(extIdx)) null else getLong(extIdx)
            )
        }

        private fun Cursor.toCalendarEvent(): CalendarEvent {
            val rruleIdx = getColumnIndexOrThrow("rrule")
            val stickerIdx = getColumnIndexOrThrow("sticker_id")
            val extIdx = getColumnIndexOrThrow("external_event_id")
            return CalendarEvent(
                id = getLong(getColumnIndexOrThrow("id")),
                calendarId = getLong(getColumnIndexOrThrow("calendar_id")),
                title = getString(getColumnIndexOrThrow("title")),
                location = getString(getColumnIndexOrThrow("location")) ?: "",
                description = getString(getColumnIndexOrThrow("description")) ?: "",
                startTimeMillis = getLong(getColumnIndexOrThrow("start_time_millis")),
                endTimeMillis = getLong(getColumnIndexOrThrow("end_time_millis")),
                timeZone = getString(getColumnIndexOrThrow("time_zone")),
                isAllDay = getInt(getColumnIndexOrThrow("is_all_day")) == 1,
                isDDay = getInt(getColumnIndexOrThrow("is_d_day")) == 1,
                isLunar = getInt(getColumnIndexOrThrow("is_lunar")) == 1,
                rrule = if (isNull(rruleIdx)) null else getString(rruleIdx),
                colorTag = EventColorTag.fromId(getString(getColumnIndexOrThrow("color_tag"))),
                stickerId = if (isNull(stickerIdx)) null else getString(stickerIdx),
                hasAlarm = getInt(getColumnIndexOrThrow("has_alarm")) == 1,
                reminderMinutesBefore = getInt(getColumnIndexOrThrow("reminder_minutes_before")),
                externalEventId = if (isNull(extIdx)) null else getLong(extIdx)
            )
        }

        private fun Cursor.toReminderItem(): ReminderItem {
            val eventIdIdx = getColumnIndexOrThrow("event_id")
            return ReminderItem(
                id = getLong(getColumnIndexOrThrow("id")),
                eventId = if (isNull(eventIdIdx)) null else getLong(eventIdIdx),
                title = getString(getColumnIndexOrThrow("title")),
                triggerTimeMillis = getLong(getColumnIndexOrThrow("trigger_time_millis")),
                isCompleted = getInt(getColumnIndexOrThrow("is_completed")) == 1,
                colorTag = EventColorTag.fromId(getString(getColumnIndexOrThrow("color_tag")))
            )
        }

        private fun Cursor.toDailyMetadata(): DailyMetadata {
            val stickerIdx = getColumnIndexOrThrow("sticker_id")
            return DailyMetadata(
                dateIso = getString(getColumnIndexOrThrow("date_iso")),
                stickerId = if (isNull(stickerIdx)) null else getString(stickerIdx),
                tempHighC = getInt(getColumnIndexOrThrow("temp_high_c")),
                tempLowC = getInt(getColumnIndexOrThrow("temp_low_c")),
                weatherCode = getInt(getColumnIndexOrThrow("weather_code")),
                weatherEmoji = getString(getColumnIndexOrThrow("weather_emoji")) ?: "☀️"
            )
        }
    }
}
