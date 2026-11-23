package com.oneui.calendar.data.repository

import android.content.Context
import com.oneui.calendar.core.common.RRuleEvaluator
import com.oneui.calendar.data.local.CalendarDatabaseHelper
import com.oneui.calendar.data.provider.SystemCalendarSyncManager
import com.oneui.calendar.data.weather.OpenMeteoWeatherService
import com.oneui.calendar.domain.model.CalendarAccount
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.DailyMetadata
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.EventInstance
import com.oneui.calendar.domain.model.ReminderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CalendarRepository(
    context: Context,
    private val dbHelper: CalendarDatabaseHelper = CalendarDatabaseHelper(context.applicationContext),
    private val weatherService: OpenMeteoWeatherService = OpenMeteoWeatherService(),
    private val syncManager: SystemCalendarSyncManager = SystemCalendarSyncManager(context.applicationContext)
) {

    private val _accounts = MutableStateFlow(dbHelper.getAllAccounts())
    val accounts: StateFlow<List<CalendarAccount>> = _accounts.asStateFlow()

    private val _allEvents = MutableStateFlow(dbHelper.getAllEvents())
    val allEvents: StateFlow<List<CalendarEvent>> = _allEvents.asStateFlow()

    private val _reminders = MutableStateFlow(dbHelper.getAllReminders())
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    private val _recentSearches = MutableStateFlow(dbHelper.getRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _dailyMetadataMap = MutableStateFlow(dbHelper.getAllDailyMetadata())
    val dailyMetadataMap: StateFlow<Map<String, DailyMetadata>> = _dailyMetadataMap.asStateFlow()

    fun getInstancesForRange(startDate: LocalDate, endDate: LocalDate): List<EventInstance> {
        val visibleAccountIds = _accounts.value
            .asSequence()
            .filter { it.isVisible }
            .map { it.id }
            .toSet()

        val visibleEvents = _allEvents.value.filter { it.calendarId in visibleAccountIds }
        return RRuleEvaluator.expandEvents(
            events = visibleEvents,
            rangeStart = startDate,
            rangeEnd = endDate
        )
    }

    fun getMetadataForDate(date: LocalDate): DailyMetadata {
        val iso = date.toString()
        return _dailyMetadataMap.value[iso] ?: DailyMetadata(dateIso = iso)
    }

    suspend fun upsertEvent(event: CalendarEvent): Long = withContext(Dispatchers.IO) {
        val syncedExternalId = if (syncManager.hasWritePermission()) {
            syncManager.pushEventToSystemCalendar(event)
        } else {
            event.externalEventId
        }
        val eventToSave = event.copy(externalEventId = syncedExternalId ?: event.externalEventId)
        val savedId = dbHelper.upsertEvent(eventToSave)
        _allEvents.value = dbHelper.getAllEvents()
        savedId
    }

    suspend fun deleteEvent(eventId: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteEvent(eventId)
        _allEvents.value = dbHelper.getAllEvents()
    }

    suspend fun toggleEventAlarm(eventId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        dbHelper.updateEventAlarm(eventId, enabled)
        _allEvents.value = dbHelper.getAllEvents()
    }

    suspend fun toggleAccountVisibility(accountId: Long) = withContext(Dispatchers.IO) {
        dbHelper.toggleAccountVisibility(accountId)
        _accounts.value = dbHelper.getAllAccounts()
    }

    suspend fun setDaySticker(date: LocalDate, stickerId: String?) = withContext(Dispatchers.IO) {
        dbHelper.setDaySticker(date.toString(), stickerId)
        _dailyMetadataMap.value = dbHelper.getAllDailyMetadata()
    }

    suspend fun upsertReminder(reminder: ReminderItem) = withContext(Dispatchers.IO) {
        dbHelper.upsertReminder(reminder)
        _reminders.value = dbHelper.getAllReminders()
    }

    suspend fun toggleReminderCompleted(reminderId: Long) = withContext(Dispatchers.IO) {
        dbHelper.toggleReminderCompleted(reminderId)
        _reminders.value = dbHelper.getAllReminders()
    }

    suspend fun deleteReminders(ids: Set<Long>) = withContext(Dispatchers.IO) {
        dbHelper.deleteReminders(ids)
        _reminders.value = dbHelper.getAllReminders()
    }

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            dbHelper.addSearchQuery(trimmed)
            _recentSearches.value = dbHelper.getRecentSearches()
        }
    }

    suspend fun removeSearchQuery(query: String) = withContext(Dispatchers.IO) {
        dbHelper.removeSearchQuery(query)
        _recentSearches.value = dbHelper.getRecentSearches()
    }

    suspend fun clearAllSearchHistory() = withContext(Dispatchers.IO) {
        dbHelper.clearAllSearchHistory()
        _recentSearches.value = dbHelper.getRecentSearches()
    }

    suspend fun searchEvents(
        query: String?,
        stickerId: String?,
        colorTag: EventColorTag?
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        dbHelper.searchEvents(query = query, stickerId = stickerId, colorTag = colorTag)
    }

    suspend fun refreshWeatherAndSystemSync() = withContext(Dispatchers.IO) {
        weatherService.fetchAndCacheForecast(dbHelper)
        _dailyMetadataMap.value = dbHelper.getAllDailyMetadata()

        val synced = syncManager.syncFromSystemCalendars(dbHelper)
        if (synced > 0) {
            _accounts.value = dbHelper.getAllAccounts()
            _allEvents.value = dbHelper.getAllEvents()
        }
    }
}
