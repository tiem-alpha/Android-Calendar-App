package com.oneui.calendar

import android.app.Application
import com.oneui.calendar.core.mcp.CalendarMcpServer
import com.oneui.calendar.core.notifications.ExactAlarmScheduler
import com.oneui.calendar.data.repository.CalendarRepository

class CalendarApplication : Application() {

    lateinit var repository: CalendarRepository
        private set

    lateinit var alarmScheduler: ExactAlarmScheduler
        private set

    lateinit var mcpServer: CalendarMcpServer
        private set

    override fun onCreate() {
        super.onCreate()
        repository = CalendarRepository(this)
        alarmScheduler = ExactAlarmScheduler(this)
        mcpServer = CalendarMcpServer(repository, alarmScheduler, port = 8765)
        mcpServer.start()
    }
}
