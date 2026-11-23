package com.oneui.calendar.core.common

import com.oneui.calendar.domain.model.EventInstance
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PositionedTimelineEvent(
    val instance: EventInstance,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val colIndex: Int,
    val totalCols: Int
)

data class CompressedSlotGroup(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val visibleEvents: List<EventInstance>,
    val hiddenEvents: List<EventInstance>
)

object WeekOverlapLayoutEngine {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MIN_VISUAL_DURATION_MINUTES = 30
    private const val COMPACT_CLUSTER_WINDOW_MINUTES = 90
    private const val COMPACT_VISIBLE_LIMIT = 2

    private data class TimedEntry(
        val instance: EventInstance,
        val startMinute: Int,
        val endMinute: Int,
        val effectiveEndMinute: Int
    )

    fun layoutDayTimeline(
        date: LocalDate,
        instances: List<EventInstance>,
        isCompactWeekColumn: Boolean
    ): Pair<List<PositionedTimelineEvent>, List<CompressedSlotGroup>> {
        val zoneId = ZoneId.systemDefault()
        val entries = instances.mapNotNull { inst ->
            if (inst.event.isAllDay) return@mapNotNull null
            if (inst.endDate.isBefore(date) || inst.startDate.isAfter(date)) return@mapNotNull null

            val startMin = if (inst.startDate.isBefore(date)) {
                0
            } else {
                val zdt = Instant.ofEpochMilli(inst.instanceStartMillis).atZone(zoneId)
                (zdt.hour * 60 + zdt.minute).coerceIn(0, MINUTES_PER_DAY - 1)
            }

            val rawEndMin = if (inst.endDate.isAfter(date)) {
                MINUTES_PER_DAY
            } else {
                val zdt = Instant.ofEpochMilli(inst.instanceEndMillis).atZone(zoneId)
                val min = zdt.hour * 60 + zdt.minute
                if (min == 0 && inst.instanceEndMillis > inst.instanceStartMillis && inst.endDate == date) {
                    MINUTES_PER_DAY
                } else {
                    min.coerceIn(startMin, MINUTES_PER_DAY)
                }
            }

            val effectiveEnd = maxOf(rawEndMin, startMin + MIN_VISUAL_DURATION_MINUTES)
                .coerceAtMost(MINUTES_PER_DAY)

            TimedEntry(
                instance = inst,
                startMinute = startMin,
                endMinute = maxOf(rawEndMin, startMin),
                effectiveEndMinute = effectiveEnd
            )
        }.sortedWith(
            compareBy<TimedEntry> { it.startMinute }
                .thenByDescending { it.effectiveEndMinute }
                .thenBy { it.instance.instanceStartMillis }
                .thenBy { it.instance.event.title }
        )

        if (entries.isEmpty()) {
            return emptyList<PositionedTimelineEvent>() to emptyList()
        }

        val clusters = mutableListOf<MutableList<TimedEntry>>()
        var currentCluster = mutableListOf(entries.first())
        var clusterMaxEffectiveEnd = entries.first().effectiveEndMinute

        for (i in 1 until entries.size) {
            val item = entries[i]
            val overlapsCluster = item.startMinute < clusterMaxEffectiveEnd
            val withinCompactWindow = isCompactWeekColumn &&
                (item.startMinute - currentCluster.first().startMinute) <= COMPACT_CLUSTER_WINDOW_MINUTES &&
                item.startMinute <= clusterMaxEffectiveEnd

            if (overlapsCluster || withinCompactWindow) {
                currentCluster += item
                clusterMaxEffectiveEnd = maxOf(clusterMaxEffectiveEnd, item.effectiveEndMinute)
            } else {
                clusters += currentCluster
                currentCluster = mutableListOf(item)
                clusterMaxEffectiveEnd = item.effectiveEndMinute
            }
        }
        clusters += currentCluster

        val positionedEvents = mutableListOf<PositionedTimelineEvent>()
        val compressedGroups = mutableListOf<CompressedSlotGroup>()

        for (cluster in clusters) {
            val clusterStart = cluster.minOf { it.startMinute }
            val clusterEnd = cluster.maxOf { maxOf(it.endMinute, it.effectiveEndMinute) }
            val hasDense90MinWindow = cluster.indices.any { i ->
                i + 2 < cluster.size &&
                    (cluster[i + 2].startMinute - cluster[i].startMinute) <= COMPACT_CLUSTER_WINDOW_MINUTES
            }

            if (isCompactWeekColumn && cluster.size >= 3 && hasDense90MinWindow) {
                compressedGroups += CompressedSlotGroup(
                    startMinuteOfDay = clusterStart,
                    endMinuteOfDay = clusterEnd,
                    visibleEvents = cluster.take(COMPACT_VISIBLE_LIMIT).map { it.instance },
                    hiddenEvents = cluster.drop(COMPACT_VISIBLE_LIMIT).map { it.instance }
                )
            } else {
                val colEndMinutes = mutableListOf<Int>()
                val assignments = mutableListOf<Pair<TimedEntry, Int>>()
                for (item in cluster) {
                    var placedCol = -1
                    for (col in colEndMinutes.indices) {
                        if (colEndMinutes[col] <= item.startMinute) {
                            placedCol = col
                            colEndMinutes[col] = item.effectiveEndMinute
                            break
                        }
                    }
                    if (placedCol == -1) {
                        placedCol = colEndMinutes.size
                        colEndMinutes += item.effectiveEndMinute
                    }
                    assignments += item to placedCol
                }
                val totalCols = colEndMinutes.size.coerceAtLeast(1)
                for ((item, col) in assignments) {
                    positionedEvents += PositionedTimelineEvent(
                        instance = item.instance,
                        startMinuteOfDay = item.startMinute,
                        endMinuteOfDay = item.endMinute,
                        colIndex = col,
                        totalCols = totalCols
                    )
                }
            }
        }

        return positionedEvents to compressedGroups
    }
}
