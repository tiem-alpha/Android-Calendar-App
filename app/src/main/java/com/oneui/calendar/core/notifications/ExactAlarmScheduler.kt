package com.oneui.calendar.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.oneui.calendar.MainActivity
import com.oneui.calendar.domain.model.CalendarEvent
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExactAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleEventAlarm(event: CalendarEvent) {
        if (!event.hasAlarm || event.id <= 0L) {
            if (event.id > 0L) cancelEventAlarm(event.id)
            return
        }

        val triggerAtMillis = computeTriggerTimeMillis(event)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            removePersistedAlarm(event.id)
            return
        }

        val formattedTime = formatEventTimeSubtitle(event)
        persistAlarm(
            ScheduledAlarmRecord(
                eventId = event.id,
                title = event.title,
                subtitle = formattedTime,
                location = event.location,
                triggerAtMillis = triggerAtMillis,
                colorHex = event.colorTag.lightBarHex
            )
        )

        scheduleExactInternal(
            eventId = event.id,
            title = event.title,
            subtitle = formattedTime,
            location = event.location,
            triggerAtMillis = triggerAtMillis,
            colorHex = event.colorTag.lightBarHex
        )
    }

    fun scheduleReminderAlarm(
        reminderId: Long,
        title: String,
        triggerAtMillis: Long,
        colorHex: Long = 0xFF0381FE
    ) {
        if (reminderId <= 0L || triggerAtMillis <= System.currentTimeMillis()) return
        val requestCode = REMINDER_REQUEST_OFFSET + reminderId.toInt()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, -reminderId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_SUBTITLE, "Reminder")
            putExtra(AlarmReceiver.EXTRA_LOCATION, "")
            putExtra(AlarmReceiver.EXTRA_COLOR_HEX, colorHex)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactWithSafeFallback(triggerAtMillis, pendingIntent)
    }

    fun cancelEventAlarm(eventId: Long) {
        val pendingIntent = buildTriggerPendingIntent(
            eventId = eventId,
            title = "",
            subtitle = "",
            location = "",
            colorHex = 0xFF0381FE
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
        removePersistedAlarm(eventId)
        NotificationManagerCompat.from(context).cancel(notificationIdForEvent(eventId))
    }

    internal fun scheduleSnoozeAlarm(
        eventId: Long,
        notificationId: Int,
        title: String,
        subtitle: String,
        location: String,
        colorHex: Long,
        snoozeMinutes: Int = 10
    ) {
        val triggerAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(AlarmReceiver.EXTRA_LOCATION, location)
            putExtra(AlarmReceiver.EXTRA_COLOR_HEX, colorHex)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactWithSafeFallback(triggerAtMillis, pendingIntent)
    }

    internal fun rescheduleAllPersistedAlarms() {
        val now = System.currentTimeMillis()
        val records = loadPersistedAlarms()
        val remaining = mutableListOf<ScheduledAlarmRecord>()
        for (record in records) {
            if (record.triggerAtMillis > now) {
                scheduleExactInternal(
                    eventId = record.eventId,
                    title = record.title,
                    subtitle = record.subtitle,
                    location = record.location,
                    triggerAtMillis = record.triggerAtMillis,
                    colorHex = record.colorHex
                )
                remaining += record
            }
        }
        savePersistedAlarms(remaining)
    }

    private fun scheduleExactInternal(
        eventId: Long,
        title: String,
        subtitle: String,
        location: String,
        triggerAtMillis: Long,
        colorHex: Long
    ) {
        val pendingIntent = buildTriggerPendingIntent(
            eventId = eventId,
            title = title,
            subtitle = subtitle,
            location = location,
            colorHex = colorHex
        )
        setExactWithSafeFallback(triggerAtMillis, pendingIntent)
    }

    private fun setExactWithSafeFallback(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager ?: return
        try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                manager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canScheduleExact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun buildTriggerPendingIntent(
        eventId: Long,
        title: String,
        subtitle: String,
        location: String,
        colorHex: Long
    ): PendingIntent {
        val notifId = notificationIdForEvent(eventId)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(AlarmReceiver.EXTRA_LOCATION, location)
            putExtra(AlarmReceiver.EXTRA_COLOR_HEX, colorHex)
        }
        return PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeTriggerTimeMillis(event: CalendarEvent): Long {
        val offsetMillis = event.reminderMinutesBefore.coerceAtLeast(0) * 60_000L
        return event.startTimeMillis - offsetMillis
    }

    private fun formatEventTimeSubtitle(event: CalendarEvent): String {
        if (event.isAllDay) return "All day"
        val zone = runCatching { ZoneId.of(event.timeZone) }.getOrDefault(ZoneId.systemDefault())
        val start = Instant.ofEpochMilli(event.startTimeMillis).atZone(zone).toLocalTime()
        val end = Instant.ofEpochMilli(event.endTimeMillis).atZone(zone).toLocalTime()
        val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        return "${fmt.format(start)} ~ ${fmt.format(end)}"
    }

    private data class ScheduledAlarmRecord(
        val eventId: Long,
        val title: String,
        val subtitle: String,
        val location: String,
        val triggerAtMillis: Long,
        val colorHex: Long
    )

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun persistAlarm(record: ScheduledAlarmRecord) {
        val current = loadPersistedAlarms().filterNot { it.eventId == record.eventId }.toMutableList()
        current += record
        savePersistedAlarms(current)
    }

    private fun removePersistedAlarm(eventId: Long) {
        val updated = loadPersistedAlarms().filterNot { it.eventId == eventId }
        savePersistedAlarms(updated)
    }

    private fun loadPersistedAlarms(): List<ScheduledAlarmRecord> {
        val raw = prefs().getString(KEY_ALARMS_JSON, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        ScheduledAlarmRecord(
                            eventId = obj.getLong("eventId"),
                            title = obj.optString("title", "Calendar Event"),
                            subtitle = obj.optString("subtitle", ""),
                            location = obj.optString("location", ""),
                            triggerAtMillis = obj.getLong("triggerAtMillis"),
                            colorHex = obj.optLong("colorHex", 0xFF0381FE)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun savePersistedAlarms(records: List<ScheduledAlarmRecord>) {
        val array = JSONArray()
        for (record in records) {
            val obj = JSONObject().apply {
                put("eventId", record.eventId)
                put("title", record.title)
                put("subtitle", record.subtitle)
                put("location", record.location)
                put("triggerAtMillis", record.triggerAtMillis)
                put("colorHex", record.colorHex)
            }
            array.put(obj)
        }
        prefs().edit().putString(KEY_ALARMS_JSON, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "oneui_exact_alarms"
        private const val KEY_ALARMS_JSON = "persisted_alarms"
        private const val REMINDER_REQUEST_OFFSET = 100_000

        fun notificationIdForEvent(eventId: Long): Int {
            return (eventId and 0x7FFF_FFFF).toInt().coerceAtLeast(1)
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureNotificationChannel(context)

        when (intent.action) {
            ACTION_TRIGGER_ALARM -> handleTriggerAlarm(context, intent)
            ACTION_SNOOZE_ALARM -> handleSnoozeAlarm(context, intent)
            ACTION_DISMISS_ALARM -> handleDismissAlarm(context, intent)
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                ExactAlarmScheduler(context).rescheduleAllPersistedAlarms()
            }
        }
    }

    private fun handleTriggerAlarm(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            if (eventId > 0L) ExactAlarmScheduler.notificationIdForEvent(eventId) else 1001
        )
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: "Calendar Event"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val location = intent.getStringExtra(EXTRA_LOCATION).orEmpty()
        val colorHex = intent.getLongExtra(EXTRA_COLOR_HEX, 0xFF0381FE)

        val contentLine = buildString {
            if (subtitle.isNotBlank()) append(subtitle)
            if (location.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(location)
            }
            if (isEmpty()) append("Upcoming schedule")
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_SUBTITLE, subtitle)
            putExtra(EXTRA_LOCATION, location)
            putExtra(EXTRA_COLOR_HEX, colorHex)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 50_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 60_000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(contentLine)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(contentLine)
            )
            .setColor(colorHex.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze 10m",
                snoozePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
            .build()

        if (hasNotificationPermission(context)) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun handleSnoozeAlarm(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            if (eventId > 0L) ExactAlarmScheduler.notificationIdForEvent(eventId) else 1001
        )
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: "Calendar Event"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val location = intent.getStringExtra(EXTRA_LOCATION).orEmpty()
        val colorHex = intent.getLongExtra(EXTRA_COLOR_HEX, 0xFF0381FE)

        NotificationManagerCompat.from(context).cancel(notificationId)
        ExactAlarmScheduler(context).scheduleSnoozeAlarm(
            eventId = eventId,
            notificationId = notificationId,
            title = title,
            subtitle = subtitle,
            location = location,
            colorHex = colorHex,
            snoozeMinutes = 10
        )
    }

    private fun handleDismissAlarm(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            if (eventId > 0L) ExactAlarmScheduler.notificationIdForEvent(eventId) else 1001
        )
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Calendar Alarms & Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High-priority exact event alarms and reminders"
                    enableVibration(true)
                    enableLights(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val CHANNEL_ID = "oneui_calendar_alarms"
        const val ACTION_TRIGGER_ALARM = "com.oneui.calendar.ACTION_TRIGGER_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.oneui.calendar.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.oneui.calendar.ACTION_DISMISS_ALARM"

        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_COLOR_HEX = "extra_color_hex"
    }
}
