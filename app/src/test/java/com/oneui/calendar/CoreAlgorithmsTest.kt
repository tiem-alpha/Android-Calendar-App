package com.oneui.calendar

import com.oneui.calendar.core.common.LunarCalendarConverter
import com.oneui.calendar.core.common.LunarDate
import com.oneui.calendar.core.common.MonthMultiDaySlotEngine
import com.oneui.calendar.core.common.NlpTimeAndConflictDetector
import com.oneui.calendar.core.common.RRuleEvaluator
import com.oneui.calendar.core.common.WeekOverlapLayoutEngine
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.CalendarSticker
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class CoreAlgorithmsTest {

    private val testZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun millis(date: LocalDate, hour: Int, minute: Int = 0, zone: ZoneId = testZone): Long {
        return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun calendarModels_helpersWorkCorrectly() {
        assertEquals(EventColorTag.PASTEL_CORAL, EventColorTag.fromId("pastel_coral"))
        assertEquals(EventColorTag.PASTEL_BLUE, EventColorTag.fromId("UNKNOWN"))
        assertEquals(CalendarSticker.CAKE, CalendarSticker.fromId("CAKE"))
        assertNull(CalendarSticker.fromId(null))

        val date = LocalDate.of(2026, 10, 24)
        val timedEvent = CalendarEvent(
            id = 1L,
            title = "Design Sync",
            startTimeMillis = millis(date, 10, 0),
            endTimeMillis = millis(date, 10, 30),
            timeZone = "Asia/Ho_Chi_Minh",
            isDDay = true
        )
        val instance = EventInstance(
            event = timedEvent,
            instanceStartMillis = timedEvent.startTimeMillis,
            instanceEndMillis = timedEvent.endTimeMillis,
            startDate = date,
            endDate = date
        )
        assertEquals("10:00 AM - 10:30 AM", instance.formattedTimeRange())
        assertEquals("10:00", instance.formattedStartTimeShort())
        assertEquals("D-Day", instance.dDayBadgeText(date))
        assertEquals("D-3", instance.dDayBadgeText(date.minusDays(3)))
        assertEquals("D+2", instance.dDayBadgeText(date.plusDays(2)))
        assertFalse(instance.isMultiDay)
        assertFalse(instance.isBannerEvent)

        val allDayEvent = timedEvent.copy(isAllDay = true)
        val allDayInstance = instance.copy(event = allDayEvent)
        assertEquals("All day", allDayInstance.formattedTimeRange())
        assertEquals("", allDayInstance.formattedStartTimeShort())
        assertTrue(allDayInstance.isBannerEvent)
    }

    @Test
    fun lunarCalendarConverter_solarAndLunarRoundTripAndKnownDates() {
        assertEquals(LunarDate(1, 1, 2024, false), LunarCalendarConverter.solarToLunar(LocalDate.of(2024, 2, 10)))
        assertEquals(LunarDate(1, 1, 2025, false), LunarCalendarConverter.solarToLunar(LocalDate.of(2025, 1, 29)))
        assertEquals(LunarDate(1, 1, 2026, false), LunarCalendarConverter.solarToLunar(LocalDate.of(2026, 2, 17)))
        assertEquals(LunarDate(15, 8, 2026, false), LunarCalendarConverter.solarToLunar(LocalDate.of(2026, 9, 25)))

        assertEquals("19 Sep", LunarCalendarConverter.formatShortLunarSubDate(LocalDate.of(2018, 10, 27)))
        assertEquals("15 Sep", LunarCalendarConverter.formatShortLunarSubDate(LocalDate.of(2026, 10, 24)))

        // 2025 has a leap 6th lunar month (starts 2025-07-25)
        val leapSolar = LocalDate.of(2025, 7, 25)
        val leapLunar = LunarCalendarConverter.solarToLunar(leapSolar)
        assertEquals(LunarDate(1, 6, 2025, true), leapLunar)
        assertEquals(leapSolar, LunarCalendarConverter.lunarToSolar(1, 6, 2025, isLeapMonth = true))

        // Full year round-trip check across 2025 and 2026
        var cursor = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2026, 12, 31)
        while (!cursor.isAfter(end)) {
            val lunar = LunarCalendarConverter.solarToLunar(cursor)
            val roundTrip = LunarCalendarConverter.lunarToSolar(
                lunarDay = lunar.day,
                lunarMonth = lunar.month,
                lunarYear = lunar.year,
                isLeapMonth = lunar.isLeapMonth
            )
            assertEquals("Round-trip mismatch for $cursor -> $lunar", cursor, roundTrip)
            cursor = cursor.plusDays(1)
        }
    }

    @Test
    fun rruleEvaluator_expandsMultiDayDailyWeeklyMonthlyAndLunarYearly() {
        val camping = CalendarEvent(
            id = 1L,
            title = "Camping",
            startTimeMillis = millis(LocalDate.of(2026, 10, 11), 0, 0),
            endTimeMillis = millis(LocalDate.of(2026, 10, 13), 23, 59),
            isAllDay = true
        )
        val daily = CalendarEvent(
            id = 2L,
            title = "Standup",
            startTimeMillis = millis(LocalDate.of(2026, 10, 1), 9, 0),
            endTimeMillis = millis(LocalDate.of(2026, 10, 1), 9, 30),
            rrule = "FREQ=DAILY;INTERVAL=2;COUNT=3"
        )
        val weekly = CalendarEvent(
            id = 3L,
            title = "Yoga Class",
            startTimeMillis = millis(LocalDate.of(2026, 10, 5), 18, 0),
            endTimeMillis = millis(LocalDate.of(2026, 10, 5), 19, 0),
            rrule = "FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20261014"
        )
        val monthly = CalendarEvent(
            id = 4L,
            title = "Month End Review",
            startTimeMillis = millis(LocalDate.of(2026, 1, 31), 17, 0),
            endTimeMillis = millis(LocalDate.of(2026, 1, 31), 18, 0),
            rrule = "FREQ=MONTHLY;INTERVAL=1"
        )
        // Lunar Mid-Autumn Festival: 2025-10-06 (15/8/2025) -> 2026-09-25 (15/8/2026)
        val lunarYearly = CalendarEvent(
            id = 5L,
            title = "Mid-Autumn",
            startTimeMillis = millis(LocalDate.of(2025, 10, 6), 0, 0),
            endTimeMillis = millis(LocalDate.of(2025, 10, 6), 23, 59),
            isAllDay = true,
            isLunar = true,
            rrule = "FREQ=YEARLY"
        )

        val octExpanded = RRuleEvaluator.expandEvents(
            events = listOf(camping, daily, weekly),
            rangeStart = LocalDate.of(2026, 10, 1),
            rangeEnd = LocalDate.of(2026, 10, 31),
            zoneId = testZone
        )

        val campingInstances = octExpanded.filter { it.event.id == 1L }
        assertEquals(1, campingInstances.size)
        assertEquals(LocalDate.of(2026, 10, 11), campingInstances.first().startDate)
        assertEquals(LocalDate.of(2026, 10, 13), campingInstances.first().endDate)
        assertTrue(campingInstances.first().isMultiDay)

        val dailyDates = octExpanded.filter { it.event.id == 2L }.map { it.startDate }
        assertEquals(
            listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5)),
            dailyDates
        )

        val weeklyDates = octExpanded.filter { it.event.id == 3L }.map { it.startDate }
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 14)
            ),
            weeklyDates
        )

        val monthlyExpanded = RRuleEvaluator.expandEvents(
            events = listOf(monthly),
            rangeStart = LocalDate.of(2026, 1, 1),
            rangeEnd = LocalDate.of(2026, 4, 30),
            zoneId = testZone
        )
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31)),
            monthlyExpanded.map { it.startDate }
        )

        val lunarExpanded = RRuleEvaluator.expandEvents(
            events = listOf(lunarYearly),
            rangeStart = LocalDate.of(2026, 1, 1),
            rangeEnd = LocalDate.of(2026, 12, 31),
            zoneId = testZone
        )
        assertEquals(listOf(LocalDate.of(2026, 9, 25)), lunarExpanded.map { it.startDate })
    }

    @Test
    fun monthMultiDaySlotEngine_packsContinuousBannerAndTracksOverflow() {
        val weeks = MonthMultiDaySlotEngine.buildMonthWeeks(YearMonth.of(2026, 10))
        assertEquals(5, weeks.size)
        assertEquals(DayOfWeek.SUNDAY, weeks.first().first().dayOfWeek)

        val weekOct11 = weeks.first { it.contains(LocalDate.of(2026, 10, 11)) }
        val campingEvent = CalendarEvent(
            id = 10L,
            title = "Camping",
            startTimeMillis = millis(LocalDate.of(2026, 10, 11), 0, 0),
            endTimeMillis = millis(LocalDate.of(2026, 10, 13), 23, 59),
            isAllDay = true
        )
        val extraEvents = (1..3).map { idx ->
            val day = LocalDate.of(2026, 10, 12)
            CalendarEvent(
                id = 20L + idx,
                title = "Task $idx",
                startTimeMillis = millis(day, 9 + idx, 0),
                endTimeMillis = millis(day, 10 + idx, 0)
            )
        }
        val instances = RRuleEvaluator.expandEvents(
            events = listOf(campingEvent) + extraEvents,
            rangeStart = weekOct11.first(),
            rangeEnd = weekOct11.last(),
            zoneId = testZone
        )

        val rowLayout = MonthMultiDaySlotEngine.layoutWeekRow(weekOct11, instances, maxSlots = 3)
        val campingRibbon = rowLayout.ribbons.first { it.instance.event.id == 10L }
        assertEquals(0, campingRibbon.startCol)
        assertEquals(2, campingRibbon.endCol)
        assertEquals(0, campingRibbon.slotIndex)
        assertTrue(campingRibbon.isBanner)
        assertFalse(campingRibbon.continuesFromPrevWeek)
        assertFalse(campingRibbon.continuesToNextWeek)

        // Monday Oct 12 (col 1) has Camping (slot 0) + 3 extra events (slots 1, 2, overflow 1)
        assertEquals(1, rowLayout.overflowCountPerCol[1])
        assertEquals(0, rowLayout.overflowCountPerCol[0])
    }

    @Test
    fun weekOverlapLayoutEngine_positionsSideBySideAndCompressesClusterInWeekColumn() {
        val date = LocalDate.of(2026, 10, 24)
        val defaultZone = ZoneId.systemDefault()
        val events = (0 until 5).map { idx ->
            val totalMinutes = 14 * 60 + idx * 15
            val startHour = totalMinutes / 60
            val startMin = totalMinutes % 60
            val event = CalendarEvent(
                id = 100L + idx,
                title = "Event $idx",
                startTimeMillis = millis(date, startHour, startMin, defaultZone),
                endTimeMillis = millis(date, startHour + 1, startMin, defaultZone)
            )
            EventInstance(
                event = event,
                instanceStartMillis = event.startTimeMillis,
                instanceEndMillis = event.endTimeMillis,
                startDate = date,
                endDate = date
            )
        }

        val (compactPositioned, compactGroups) = WeekOverlapLayoutEngine.layoutDayTimeline(
            date = date,
            instances = events,
            isCompactWeekColumn = true
        )
        assertTrue(compactPositioned.isEmpty())
        assertEquals(1, compactGroups.size)
        assertEquals(2, compactGroups.first().visibleEvents.size)
        assertEquals(3, compactGroups.first().hiddenEvents.size)

        val (dayPositioned, dayGroups) = WeekOverlapLayoutEngine.layoutDayTimeline(
            date = date,
            instances = events,
            isCompactWeekColumn = false
        )
        assertTrue(dayGroups.isEmpty())
        assertEquals(5, dayPositioned.size)
        assertTrue(dayPositioned.all { it.totalCols >= 3 })
    }

    @Test
    fun nlpTimeAndConflictDetector_detectsTimeRangesAndPrioritizesConflicts() {
        val targetDate = LocalDate.of(2026, 10, 24)
        val jakeAllDay = CalendarEvent(
            id = 1L,
            title = "3pm Movie with Jake",
            startTimeMillis = millis(targetDate, 0, 0),
            endTimeMillis = millis(targetDate, 23, 59),
            isAllDay = true
        )
        val emilySameTime = CalendarEvent(
            id = 2L,
            title = "3pm Movie with Emily",
            startTimeMillis = millis(targetDate, 15, 0),
            endTimeMillis = millis(targetDate, 16, 0)
        )
        val dentistMorning = CalendarEvent(
            id = 3L,
            title = "9am Dentist",
            startTimeMillis = millis(targetDate, 9, 0),
            endTimeMillis = millis(targetDate, 10, 0)
        )
        val sameDayInstances = RRuleEvaluator.expandEvents(
            events = listOf(jakeAllDay, emilySameTime, dentistMorning),
            rangeStart = targetDate,
            rangeEnd = targetDate,
            zoneId = testZone
        )

        val preview = NlpTimeAndConflictDetector.analyzeTitleInput(
            rawTitle = "3pm Movie",
            targetDate = targetDate,
            sameDayInstances = sameDayInstances,
            zoneId = testZone
        )
        assertNotNull(preview)
        preview!!
        assertEquals("3:00PM ~ 4:00PM", preview.detectedTimeRangeText)
        assertEquals("3pm", preview.matchedPhrase)
        assertEquals(millis(targetDate, 15, 0), preview.suggestedStartMillis)
        assertEquals(millis(targetDate, 16, 0), preview.suggestedEndMillis)
        assertEquals(2, preview.conflictingEvents.size)
        assertEquals(emilySameTime.id, preview.conflictingEvents.first().event.id)
        assertTrue(preview.conflictingEvents.any { it.event.id == jakeAllDay.id })

        val rangePreview = NlpTimeAndConflictDetector.analyzeTitleInput(
            rawTitle = "3pm-5pm Workshop",
            targetDate = targetDate,
            sameDayInstances = emptyList(),
            zoneId = testZone
        )
        assertNotNull(rangePreview)
        assertEquals("3:00PM ~ 5:00PM", rangePreview!!.detectedTimeRangeText)

        val twentyFourHourPreview = NlpTimeAndConflictDetector.analyzeTitleInput(
            rawTitle = "14:30 Dentist",
            targetDate = targetDate,
            sameDayInstances = emptyList(),
            zoneId = testZone
        )
        assertNotNull(twentyFourHourPreview)
        assertEquals("2:30PM ~ 3:30PM", twentyFourHourPreview!!.detectedTimeRangeText)

        assertNull(
            NlpTimeAndConflictDetector.analyzeTitleInput(
                rawTitle = "Buy groceries",
                targetDate = targetDate,
                sameDayInstances = sameDayInstances,
                zoneId = testZone
            )
        )
    }
}
