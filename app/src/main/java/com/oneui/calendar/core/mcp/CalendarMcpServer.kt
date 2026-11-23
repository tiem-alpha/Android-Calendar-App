package com.oneui.calendar.core.mcp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.oneui.calendar.core.notifications.ExactAlarmScheduler
import com.oneui.calendar.data.repository.CalendarRepository
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.EventColorTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Embedded Model Context Protocol (MCP) JSON-RPC 2.0 Server running over HTTP inside the Android App.
 *
 * Endpoint: http://127.0.0.1:8765/mcp  (reachable from PC via `adb forward tcp:8765 tcp:8765`)
 *
 * Exposed MCP Tools:
 * - list_events   : Load events in a date range (start_date, end_date in YYYY-MM-DD)
 * - get_event     : Read single event details by event_id
 * - create_event  : Add a new event (title, start_time, end_time, location, description, is_all_day, color_tag, has_alarm)
 * - update_event  : Update an existing event by event_id
 * - delete_event  : Delete an event by event_id
 * - search_events : Search events by keyword, sticker, or color
 */
class CalendarMcpServer(
    private val repository: CalendarRepository,
    private val alarmScheduler: ExactAlarmScheduler,
    val port: Int = 8765
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    var isRunning: Boolean = false
        private set

    fun start() {
        if (isRunning) return
        scope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                isRunning = true
                Log.i(TAG, "Calendar MCP Server started on port $port")
                while (isRunning && !ss.isClosed) {
                    val client = ss.accept()
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "MCP Server error: ${e.message}", e)
                }
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                val input = BufferedInputStream(client.getInputStream())
                val output = BufferedOutputStream(client.getOutputStream())

                // Read HTTP headers
                val headerBytes = ByteArrayOutputStream()
                var prev3 = 0
                var prev2 = 0
                var prev1 = 0
                while (true) {
                    val b = input.read()
                    if (b == -1) return
                    headerBytes.write(b)
                    if (prev3 == '\r'.code && prev2 == '\n'.code && prev1 == '\r'.code && b == '\n'.code) {
                        break
                    }
                    prev3 = prev2
                    prev2 = prev1
                    prev1 = b
                }

                val headerText = headerBytes.toString(StandardCharsets.UTF_8.name())
                val headerLines = headerText.split("\r\n")
                val requestLine = headerLines.firstOrNull().orEmpty()
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0).orEmpty()
                val path = parts.getOrNull(1).orEmpty()

                var contentLength = 0
                for (line in headerLines) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                // Read HTTP body if present
                val bodyBytes = ByteArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val r = input.read(bodyBytes, readTotal, contentLength - readTotal)
                    if (r == -1) break
                    readTotal += r
                }
                val bodyText = String(bodyBytes, 0, readTotal, StandardCharsets.UTF_8)

                if (method == "OPTIONS") {
                    writeHttpResponse(output, 204, "")
                    return
                }

                if (method == "GET" && (path == "/" || path == "/health" || path == "/mcp")) {
                    val info = JsonObject().apply {
                        addProperty("name", "oneui-calendar-mcp")
                        addProperty("version", "1.0.0")
                        addProperty("protocol", "MCP JSON-RPC 2.0")
                        addProperty("endpoint", "/mcp")
                        addProperty("status", "running")
                    }
                    writeHttpResponse(output, 200, gson.toJson(info))
                    return
                }

                if (method == "POST") {
                    val responseJson = handleJsonRpcRequest(bodyText)
                    writeHttpResponse(output, 200, responseJson)
                    return
                }

                writeHttpResponse(output, 405, """{"error":"Method not allowed"}""")
            } catch (e: Exception) {
                Log.e(TAG, "Client handling error: ${e.message}", e)
            }
        }
    }

    fun handleJsonRpcRequest(bodyText: String): String {
        return try {
            val req = JsonParser.parseString(bodyText).asJsonObject
            val id: JsonElement? = req.get("id")
            val method = req.get("method")?.asString.orEmpty()
            val params = req.getAsJsonObject("params") ?: JsonObject()

            when (method) {
                "initialize" -> {
                    val result = JsonObject().apply {
                        addProperty("protocolVersion", "2024-11-05")
                        add("capabilities", JsonObject().apply {
                            add("tools", JsonObject())
                        })
                        add("serverInfo", JsonObject().apply {
                            addProperty("name", "oneui-calendar-mcp")
                            addProperty("version", "1.0.0")
                        })
                    }
                    jsonRpcSuccess(id, result)
                }

                "notifications/initialized" -> {
                    jsonRpcSuccess(id, JsonObject())
                }

                "tools/list" -> {
                    val result = JsonObject().apply {
                        add("tools", buildToolDefinitions())
                    }
                    jsonRpcSuccess(id, result)
                }

                "tools/call" -> {
                    val toolName = params.get("name")?.asString.orEmpty()
                    val args = params.getAsJsonObject("arguments") ?: JsonObject()
                    val toolOutput = executeTool(toolName, args)
                    val contentArr = JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", toolOutput)
                        })
                    }
                    val result = JsonObject().apply {
                        add("content", contentArr)
                        addProperty("isError", false)
                    }
                    jsonRpcSuccess(id, result)
                }

                else -> jsonRpcError(id, -32601, "Method not found: $method")
            }
        } catch (e: Exception) {
            jsonRpcError(null, -32603, "Internal error: ${e.message}")
        }
    }

    private fun executeTool(name: String, args: JsonObject): String {
        val zone = ZoneId.systemDefault()
        return when (name) {
            "list_events" -> {
                val startStr = args.get("start_date")?.asString
                val endStr = args.get("end_date")?.asString
                val startDate = startStr?.let { LocalDate.parse(it) } ?: LocalDate.now().withDayOfMonth(1)
                val endDate = endStr?.let { LocalDate.parse(it) } ?: startDate.plusMonths(1)
                val instances = repository.getInstancesForRange(startDate, endDate)
                val arr = JsonArray()
                instances.forEach { inst -> arr.add(eventToJson(inst.event, zone)) }
                JsonObject().apply {
                    addProperty("count", instances.size)
                    addProperty("start_date", startDate.toString())
                    addProperty("end_date", endDate.toString())
                    add("events", arr)
                }.toString()
            }

            "get_event" -> {
                val eventId = args.get("event_id")?.asLong ?: return """{"error":"Missing event_id"}"""
                val event = repository.allEvents.value.find { it.id == eventId }
                    ?: return """{"error":"Event not found: $eventId"}"""
                eventToJson(event, zone).toString()
            }

            "create_event" -> {
                val title = args.get("title")?.asString?.trim().orEmpty()
                if (title.isEmpty()) return """{"error":"Missing title"}"""
                val isAllDay = args.get("is_all_day")?.asBoolean ?: false
                val startMillis = parseIsoDateTime(args.get("start_time")?.asString, isAllDay, zone)
                    ?: LocalDate.now().atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
                val endMillis = parseIsoDateTime(args.get("end_time")?.asString, isAllDay, zone)
                    ?: (startMillis + if (isAllDay) 86_400_000L else 3_600_000L)

                val newEvent = CalendarEvent(
                    id = 0L,
                    calendarId = args.get("calendar_id")?.asLong ?: 1L,
                    title = title,
                    location = args.get("location")?.asString.orEmpty(),
                    description = args.get("description")?.asString.orEmpty(),
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    timeZone = zone.id,
                    isAllDay = isAllDay,
                    isDDay = args.get("is_d_day")?.asBoolean ?: false,
                    isLunar = args.get("is_lunar")?.asBoolean ?: false,
                    rrule = args.get("rrule")?.takeIf { !it.isJsonNull }?.asString,
                    colorTag = EventColorTag.fromId(args.get("color_tag")?.asString),
                    stickerId = args.get("sticker_id")?.takeIf { !it.isJsonNull }?.asString,
                    hasAlarm = args.get("has_alarm")?.asBoolean ?: true,
                    reminderMinutesBefore = args.get("reminder_minutes_before")?.asInt ?: 10
                )

                val savedId = runBlocking { repository.upsertEvent(newEvent) }
                val savedEvent = newEvent.copy(id = savedId)
                if (savedEvent.hasAlarm) {
                    alarmScheduler.scheduleEventAlarm(savedEvent)
                }
                JsonObject().apply {
                    addProperty("status", "created")
                    addProperty("event_id", savedId)
                    add("event", eventToJson(savedEvent, zone))
                }.toString()
            }

            "update_event" -> {
                val eventId = args.get("event_id")?.asLong ?: return """{"error":"Missing event_id"}"""
                val existing = repository.allEvents.value.find { it.id == eventId }
                    ?: return """{"error":"Event not found: $eventId"}"""

                val isAllDay = args.get("is_all_day")?.asBoolean ?: existing.isAllDay
                val updated = existing.copy(
                    title = args.get("title")?.asString ?: existing.title,
                    location = args.get("location")?.asString ?: existing.location,
                    description = args.get("description")?.asString ?: existing.description,
                    startTimeMillis = parseIsoDateTime(args.get("start_time")?.asString, isAllDay, zone)
                        ?: existing.startTimeMillis,
                    endTimeMillis = parseIsoDateTime(args.get("end_time")?.asString, isAllDay, zone)
                        ?: existing.endTimeMillis,
                    isAllDay = isAllDay,
                    isDDay = args.get("is_d_day")?.asBoolean ?: existing.isDDay,
                    colorTag = args.get("color_tag")?.asString?.let { EventColorTag.fromId(it) }
                        ?: existing.colorTag,
                    hasAlarm = args.get("has_alarm")?.asBoolean ?: existing.hasAlarm
                )
                runBlocking { repository.upsertEvent(updated) }
                if (updated.hasAlarm) {
                    alarmScheduler.scheduleEventAlarm(updated)
                } else {
                    alarmScheduler.cancelEventAlarm(updated.id)
                }
                JsonObject().apply {
                    addProperty("status", "updated")
                    add("event", eventToJson(updated, zone))
                }.toString()
            }

            "delete_event" -> {
                val eventId = args.get("event_id")?.asLong ?: return """{"error":"Missing event_id"}"""
                runBlocking { repository.deleteEvent(eventId) }
                alarmScheduler.cancelEventAlarm(eventId)
                JsonObject().apply {
                    addProperty("status", "deleted")
                    addProperty("event_id", eventId)
                }.toString()
            }

            "search_events" -> {
                val query = args.get("query")?.asString
                val stickerId = args.get("sticker_id")?.asString
                val colorTag = args.get("color_tag")?.asString?.let { EventColorTag.fromId(it) }
                val matches = runBlocking { repository.searchEvents(query, stickerId, colorTag) }
                val arr = JsonArray()
                matches.forEach { arr.add(eventToJson(it, zone)) }
                JsonObject().apply {
                    addProperty("count", matches.size)
                    add("events", arr)
                }.toString()
            }

            else -> """{"error":"Unknown tool: $name"}"""
        }
    }

    private fun parseIsoDateTime(raw: String?, isAllDay: Boolean, zone: ZoneId): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            if (raw.length == 10) {
                LocalDate.parse(raw).atStartOfDay(zone).toInstant().toEpochMilli()
            } else {
                LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }
        }.getOrNull()
    }

    private fun eventToJson(event: CalendarEvent, zone: ZoneId): JsonObject {
        val startStr = Instant.ofEpochMilli(event.startTimeMillis).atZone(zone)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val endStr = Instant.ofEpochMilli(event.endTimeMillis).atZone(zone)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return JsonObject().apply {
            addProperty("id", event.id)
            addProperty("title", event.title)
            addProperty("location", event.location)
            addProperty("description", event.description)
            addProperty("start_time", startStr)
            addProperty("end_time", endStr)
            addProperty("is_all_day", event.isAllDay)
            addProperty("is_d_day", event.isDDay)
            addProperty("is_lunar", event.isLunar)
            addProperty("color_tag", event.colorTag.id)
            addProperty("has_alarm", event.hasAlarm)
        }
    }

    private fun buildToolDefinitions(): JsonArray = JsonArray().apply {
        add(toolDef("list_events", "Load calendar events within a date range (YYYY-MM-DD)", mapOf(
            "start_date" to "Start date in YYYY-MM-DD format",
            "end_date" to "End date in YYYY-MM-DD format"
        )))
        add(toolDef("get_event", "Read a single calendar event by ID", mapOf(
            "event_id" to "Numeric ID of the event"
        )))
        add(toolDef("create_event", "Create a new calendar event", mapOf(
            "title" to "Event title (required)",
            "start_time" to "ISO-8601 start time e.g. 2026-11-24T09:00:00 or 2026-11-24",
            "end_time" to "ISO-8601 end time e.g. 2026-11-24T10:00:00",
            "location" to "Event location",
            "description" to "Event notes/description",
            "is_all_day" to "true or false",
            "color_tag" to "PASTEL_BLUE, PASTEL_PINK, PASTEL_CORAL, PASTEL_MINT, PASTEL_LAVENDER, PASTEL_YELLOW",
            "has_alarm" to "true or false"
        )))
        add(toolDef("update_event", "Update an existing calendar event by ID", mapOf(
            "event_id" to "Numeric ID of the event to update (required)",
            "title" to "Updated title",
            "start_time" to "Updated ISO-8601 start time",
            "end_time" to "Updated ISO-8601 end time",
            "location" to "Updated location",
            "description" to "Updated notes",
            "is_all_day" to "true or false",
            "has_alarm" to "true or false"
        )))
        add(toolDef("delete_event", "Delete a calendar event by ID", mapOf(
            "event_id" to "Numeric ID of the event to delete"
        )))
        add(toolDef("search_events", "Search calendar events by keyword, sticker, or color", mapOf(
            "query" to "Search keyword",
            "sticker_id" to "Optional sticker ID (CAKE, PARTY, CAMP, etc.)",
            "color_tag" to "Optional pastel color tag ID"
        )))
    }

    private fun toolDef(name: String, description: String, props: Map<String, String>): JsonObject {
        val propertiesObj = JsonObject()
        props.forEach { (k, desc) ->
            propertiesObj.add(k, JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", desc)
            })
        }
        return JsonObject().apply {
            addProperty("name", name)
            addProperty("description", description)
            add("inputSchema", JsonObject().apply {
                addProperty("type", "object")
                add("properties", propertiesObj)
            })
        }
    }

    private fun jsonRpcSuccess(id: JsonElement?, result: JsonObject): String {
        val resp = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            if (id != null) add("id", id)
            add("result", result)
        }
        return gson.toJson(resp)
    }

    private fun jsonRpcError(id: JsonElement?, code: Int, message: String): String {
        val resp = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            if (id != null) add("id", id)
            add("error", JsonObject().apply {
                addProperty("code", code)
                addProperty("message", message)
            })
        }
        return gson.toJson(resp)
    }

    private fun writeHttpResponse(out: BufferedOutputStream, statusCode: Int, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            204 -> "No Content"
            else -> "Error"
        }
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 $statusCode $statusText\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Content-Type\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray(StandardCharsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
    }

    companion object {
        private const val TAG = "CalendarMcpServer"
    }
}
