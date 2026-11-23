package com.oneui.calendar.core.common

import com.oneui.calendar.domain.model.EventInstance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class WeekEventRibbon(
    val instance: EventInstance,
    val startCol: Int,
    val endCol: Int,
    val slotIndex: Int,
    val isBanner: Boolean,
    val continuesFromPrevWeek: Boolean,
    val continuesToNextWeek: Boolean
)

data class WeekRowLayout(
    val weekStartDate: LocalDate,
    val days: List<LocalDate>,
    val ribbons: List<WeekEventRibbon>,
    val overflowCountPerCol: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeekRowLayout) return false
        return weekStartDate == other.weekStartDate &&
            days == other.days &&
            ribbons == other.ribbons &&
            overflowCountPerCol.contentEquals(other.overflowCountPerCol)
    }

    override fun hashCode(): Int {
        var result = weekStartDate.hashCode()
        result = 31 * result + days.hashCode()
        result = 31 * result + ribbons.hashCode()
        result = 31 * result + overflowCountPerCol.contentHashCode()
        return result
    }
}

object MonthMultiDaySlotEngine {

    fun buildMonthWeeks(
        yearMonth: YearMonth,
        startWeekOnMonday: Boolean = false
    ): List<List<LocalDate>> {
        val firstOfMonth = yearMonth.atDay(1)
        val firstDayOfWeek = if (startWeekOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        val leadingDays = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val gridStart = firstOfMonth.minusDays(leadingDays.toLong())
        val totalCells = leadingDays + yearMonth.lengthOfMonth()
        val weekCount = ((totalCells + 6) / 7).coerceIn(5, 6)

        return List(weekCount) { weekIndex ->
            List(7) { dayIndex ->
                gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
            }
        }
    }

    fun layoutWeekRow(
        weekDays: List<LocalDate>,
        instances: List<EventInstance>,
        maxSlots: Int = 3
    ): WeekRowLayout {
        require(weekDays.size == 7) { "weekDays must contain exactly 7 days" }
        val weekStart = weekDays.first()
        val weekEnd = weekDays.last()
        val overflowCountPerCol = IntArray(7)

        if (maxSlots <= 0) {
            for (inst in instances) {
                if (!inst.endDate.isBefore(weekStart) && !inst.startDate.isAfter(weekEnd)) {
                    val startCol = ChronoUnit.DAYS.between(weekStart, maxDate(weekStart, inst.startDate)).toInt().coerceIn(0, 6)
                    val endCol = ChronoUnit.DAYS.between(weekStart, minDate(weekEnd, inst.endDate)).toInt().coerceIn(startCol, 6)
                    for (col in startCol..endCol) {
                        overflowCountPerCol[col]++
                    }
                }
            }
            return WeekRowLayout(weekStart, weekDays, emptyList(), overflowCountPerCol)
        }

        data class WeekCandidate(
            val instance: EventInstance,
            val startCol: Int,
            val endCol: Int,
            val continuesFromPrevWeek: Boolean,
            val continuesToNextWeek: Boolean
        )

        val candidates = instances.mapNotNull { inst ->
            if (inst.endDate.isBefore(weekStart) || inst.startDate.isAfter(weekEnd)) {
                null
            } else {
                val startCol = ChronoUnit.DAYS.between(weekStart, maxDate(weekStart, inst.startDate))
                    .toInt()
                    .coerceIn(0, 6)
                val endCol = ChronoUnit.DAYS.between(weekStart, minDate(weekEnd, inst.endDate))
                    .toInt()
                    .coerceIn(startCol, 6)
                WeekCandidate(
                    instance = inst,
                    startCol = startCol,
                    endCol = endCol,
                    continuesFromPrevWeek = inst.startDate.isBefore(weekStart),
                    continuesToNextWeek = inst.endDate.isAfter(weekEnd)
                )
            }
        }.sortedWith(
            compareBy<WeekCandidate> { it.startCol }
                .thenByDescending { it.instance.isBannerEvent }
                .thenByDescending { it.endCol - it.startCol }
                .thenBy { it.instance.instanceStartMillis }
                .thenBy { it.instance.event.title }
        )

        val occupied = Array(maxSlots) { BooleanArray(7) }
        val ribbons = mutableListOf<WeekEventRibbon>()

        for (candidate in candidates) {
            var assignedSlot = -1
            for (slot in 0 until maxSlots) {
                var free = true
                for (col in candidate.startCol..candidate.endCol) {
                    if (occupied[slot][col]) {
                        free = false
                        break
                    }
                }
                if (free) {
                    assignedSlot = slot
                    break
                }
            }

            if (assignedSlot >= 0) {
                for (col in candidate.startCol..candidate.endCol) {
                    occupied[assignedSlot][col] = true
                }
                ribbons += WeekEventRibbon(
                    instance = candidate.instance,
                    startCol = candidate.startCol,
                    endCol = candidate.endCol,
                    slotIndex = assignedSlot,
                    isBanner = candidate.instance.isBannerEvent,
                    continuesFromPrevWeek = candidate.continuesFromPrevWeek,
                    continuesToNextWeek = candidate.continuesToNextWeek
                )
            } else {
                for (col in candidate.startCol..candidate.endCol) {
                    overflowCountPerCol[col]++
                }
            }
        }

        return WeekRowLayout(
            weekStartDate = weekStart,
            days = weekDays,
            ribbons = ribbons,
            overflowCountPerCol = overflowCountPerCol
        )
    }

    private fun maxDate(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b
    private fun minDate(a: LocalDate, b: LocalDate): LocalDate = if (a.isBefore(b)) a else b
}
