package com.oneui.calendar.feature.week

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.CompressedSlotGroup
import com.oneui.calendar.core.common.HolidayProvider
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.common.LunarCalendarConverter
import com.oneui.calendar.core.common.PositionedTimelineEvent
import com.oneui.calendar.core.common.WeekOverlapLayoutEngine
import com.oneui.calendar.core.designsystem.components.OneUiDaySubHeaderBar
import com.oneui.calendar.core.designsystem.components.OneUiQuickAddBar
import com.oneui.calendar.core.designsystem.components.OneUiWeekdayHeaderRow
import com.oneui.calendar.core.designsystem.theme.OneUiShapes
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.core.designsystem.theme.containerColor
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.CalendarSticker
import com.oneui.calendar.domain.model.DailyMetadata
import com.oneui.calendar.domain.model.EventInstance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

private val HOUR_HEIGHT = 64.dp
private val LEFT_GUTTER_WIDTH = 46.dp
private val CURRENT_TIME_RED = Color(0xFFE53935)
private val TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("h:mm", Locale.US)

@Composable
fun WeekDayScreen(
    isDayView: Boolean,
    selectedDate: LocalDate,
    todayDate: LocalDate,
    weekInstances: List<EventInstance>,
    dailyMetadataMap: Map<String, DailyMetadata>,
    onSelectDate: (LocalDate) -> Unit,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onOpenDayDetailPopup: (LocalDate) -> Unit,
    onOpenStickerPickerForDate: (LocalDate) -> Unit,
    onQuickAddClick: (LocalDate) -> Unit,
    onFabAddClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 48.dp.toPx() }

    val weekDays = remember(selectedDate) {
        val leading = selectedDate.dayOfWeek.value % 7
        val sunday = selectedDate.minusDays(leading.toLong())
        List(7) { offset -> sunday.plusDays(offset.toLong()) }
    }

    val weekNumber = remember(selectedDate) {
        selectedDate.get(WeekFields.SUNDAY_START.weekOfWeekBasedYear())
    }

    val displayedColumns = remember(isDayView, weekDays, selectedDate) {
        if (isDayView) listOf(selectedDate) else weekDays
    }

    // Use 11:30 when showing the seeded demo date so the red current-time marker matches Screenshot 4,
    // otherwise track the live wall-clock time.
    val indicatorTime = remember(todayDate) {
        if (todayDate != LocalDate.now()) {
            LocalTime.of(11, 30)
        } else {
            LocalTime.now()
        }
    }
    val indicatorMinutes = indicatorTime.hour * 60 + indicatorTime.minute

    val scrollState = rememberScrollState()
    LaunchedEffect(isDayView) {
        val targetMinute = (indicatorMinutes - 105).coerceAtLeast(0)
        val targetPx = with(density) { ((targetMinute / 60f) * HOUR_HEIGHT.toPx()).toInt() }
        scrollState.scrollTo(targetPx)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.calendarGridBackground)
            .pointerInput(isDayView, selectedDate) {
                var dragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount
                    },
                    onDragEnd = {
                        if (abs(dragX) > swipeThresholdPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (dragX > 0f) onPrevPeriod() else onNextPeriod()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            OneUiWeekdayHeaderRow(
                startOnMonday = false,
                leadingSpacerWidth = LEFT_GUTTER_WIDTH
            )

            // Fixed Week / Day Header below weekday letters
            WeekDayHeaderStrip(
                isDayView = isDayView,
                weekNumber = weekNumber,
                weekDays = weekDays,
                selectedDate = selectedDate,
                todayDate = todayDate,
                weekInstances = weekInstances,
                dailyMetadataMap = dailyMetadataMap,
                onSelectDate = onSelectDate,
                onEventClick = onEventClick,
                onOpenStickerPickerForDate = onOpenStickerPickerForDate,
                onQuickAddClick = onQuickAddClick
            )

            if (isDayView) {
                val meta = dailyMetadataMap[selectedDate.toString()]
                val strings = LocalAppStrings.current
                OneUiDaySubHeaderBar(
                    lunarDateText = strings.formatLunarSubDate(selectedDate),
                    tempHighC = meta?.tempHighC ?: 12,
                    tempLowC = meta?.tempLowC ?: 8,
                    weatherEmoji = meta?.weatherEmoji ?: "☀️",
                    stickerEmoji = CalendarSticker.fromId(meta?.stickerId)?.emoji,
                    onStickerClick = { onOpenStickerPickerForDate(selectedDate) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.dividerHairline)
            )

            // 24-Hour Scrollable Timeline Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                val totalTimelineHeight = HOUR_HEIGHT * 24

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalTimelineHeight + 84.dp)
                ) {
                    // Left Hour Gutter + Red Current Time Label
                    Box(
                        modifier = Modifier
                            .width(LEFT_GUTTER_WIDTH)
                            .height(totalTimelineHeight)
                    ) {
                        for (hour in 1..23) {
                            val labelTop = HOUR_HEIGHT * hour - 8.dp
                            val labelText = when {
                                hour < 12 -> "$hour"
                                hour == 12 -> "12\nPM"
                                else -> "${hour - 12}"
                            }
                            Text(
                                text = labelText,
                                color = colors.textTertiary,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                textAlign = TextAlign.End,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = labelTop)
                                    .padding(end = 8.dp)
                            )
                        }

                        if (displayedColumns.contains(todayDate)) {
                            val redY = HOUR_HEIGHT * (indicatorMinutes / 60f) - 7.dp
                            Text(
                                text = indicatorTime.format(TIME_LABEL_FORMATTER),
                                color = CURRENT_TIME_RED,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = redY)
                                    .padding(end = 6.dp)
                            )
                        }
                    }

                    // Right Grid & Event Columns
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(totalTimelineHeight)
                    ) {
                        val gridWidth = maxWidth
                        val colCount = displayedColumns.size
                        val colWidth = gridWidth / colCount.toFloat()

                        // Hour solid dividers, half-hour dotted lines, vertical column hairlines, and red time marker
                        val hairlineColor = colors.dividerHairline
                        val halfHourColor = colors.dividerHairline.copy(alpha = 0.65f)
                        val todayColIndex = displayedColumns.indexOf(todayDate)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val hourPx = HOUR_HEIGHT.toPx()
                            val colPx = size.width / colCount.toFloat()

                            // Vertical column dividers
                            for (c in 0 until colCount) {
                                val x = c * colPx
                                drawLine(
                                    color = hairlineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Horizontal hour & dotted half-hour lines
                            for (h in 0 until 24) {
                                val y = h * hourPx
                                if (h > 0) {
                                    drawLine(
                                        color = hairlineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                val halfY = y + hourPx / 2f
                                drawLine(
                                    color = halfHourColor,
                                    start = Offset(0f, halfY),
                                    end = Offset(size.width, halfY),
                                    strokeWidth = 0.8.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                                )
                            }

                            // Red Current Time Indicator (►────) on Today's column
                            if (todayColIndex >= 0) {
                                val indicatorY = (indicatorMinutes / 60f) * hourPx
                                val colStartX = todayColIndex * colPx
                                val colEndX = colStartX + colPx
                                val triSize = 6.dp.toPx()

                                val triangle = Path().apply {
                                    moveTo(colStartX, indicatorY - triSize * 0.7f)
                                    lineTo(colStartX + triSize, indicatorY)
                                    lineTo(colStartX, indicatorY + triSize * 0.7f)
                                    close()
                                }
                                drawPath(path = triangle, color = CURRENT_TIME_RED)
                                drawLine(
                                    color = CURRENT_TIME_RED,
                                    start = Offset(colStartX + triSize * 0.5f, indicatorY),
                                    end = Offset(colEndX, indicatorY),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }

                        // Positioned Timed Events & Compressed "+N" Clusters
                        displayedColumns.forEachIndexed { colIndex, day ->
                            val (positioned, compressed) = remember(day, weekInstances, isDayView) {
                                WeekOverlapLayoutEngine.layoutDayTimeline(
                                    date = day,
                                    instances = weekInstances,
                                    isCompactWeekColumn = !isDayView
                                )
                            }

                            val colStartX = colWidth * colIndex

                            positioned.forEach { item ->
                                TimelineEventBlock(
                                    item = item,
                                    colStartX = colStartX,
                                    colWidth = colWidth,
                                    isDayView = isDayView,
                                    onClick = { onEventClick(item.instance.event) }
                                )
                            }

                            compressed.forEach { group ->
                                CompressedClusterBlock(
                                    group = group,
                                    colStartX = colStartX,
                                    colWidth = colWidth,
                                    onEventClick = onEventClick,
                                    onExpandCluster = { onOpenDayDetailPopup(day) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom gradient fade mask + Quick Add Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.calendarGridBackground.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.calendarGridBackground.copy(alpha = 0.96f))
            ) {
                OneUiQuickAddBar(
                    selectedDate = selectedDate,
                    onQuickTextClick = { onQuickAddClick(selectedDate) },
                    onFabAddClick = { onFabAddClick(selectedDate) }
                )
            }
        }
    }
}

@Composable
private fun WeekDayHeaderStrip(
    isDayView: Boolean,
    weekNumber: Int,
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    todayDate: LocalDate,
    weekInstances: List<EventInstance>,
    dailyMetadataMap: Map<String, DailyMetadata>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onOpenStickerPickerForDate: (LocalDate) -> Unit,
    onQuickAddClick: (LocalDate) -> Unit
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = 4.dp)
    ) {
        // Left WK gutter
        Box(
            modifier = Modifier
                .width(LEFT_GUTTER_WIDTH)
                .fillMaxHeight()
                .padding(top = 4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "WK\n$weekNumber",
                color = colors.textSecondary,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }

        // 7 Day Columns
        weekDays.forEach { day ->
            val isSelected = day == selectedDate
            val isToday = day == todayDate
            val strings = LocalAppStrings.current
            val isWeekend = day.dayOfWeek == DayOfWeek.SUNDAY || day.dayOfWeek == DayOfWeek.SATURDAY
            val isHoliday = HolidayProvider.getHolidayForDate(day, strings.language) != null
            val isRedDay = isWeekend || isHoliday
            val allDayEvents = remember(day, weekInstances) {
                weekInstances.filter { inst ->
                    inst.isBannerEvent && !inst.endDate.isBefore(day) && !inst.startDate.isAfter(day)
                }
            }
            val meta = dailyMetadataMap[day.toString()]
            val daySticker = CalendarSticker.fromId(meta?.stickerId)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .clip(OneUiShapes.DayCellSelectionShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.2.dp,
                                color = colors.selectedCellBorder,
                                shape = OneUiShapes.DayCellSelectionShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectDate(day)
                        }
                    )
                    .padding(top = 3.dp, bottom = 5.dp, start = 2.dp, end = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day number
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(OneUiShapes.TodaySquircleShape)
                                .background(colors.todayBadgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = colors.todayBadgeText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    } else {
                        Text(
                            text = day.dayOfMonth.toString(),
                            color = if (isRedDay) colors.sundayRed else colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                // Always render All-Day event pills for every day (including the selected day!)
                if (allDayEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    allDayEvents.take(2).forEach { inst ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .height(17.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(inst.event.colorTag.containerColor())
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onEventClick(inst.event) }
                                )
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = inst.event.title,
                                color = colors.textPrimary,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (allDayEvents.size > 2) {
                        Text(
                            text = "+${allDayEvents.size - 2}",
                            color = colors.textTertiary,
                            fontSize = 9.5.sp
                        )
                    }
                }

                if (isSelected && !isDayView) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Pastel Blue [ + ] pill button (matching Monday 22 in Screenshot 4)
                    val addBg = if (colors.isDark) Color(0xFF1A3A5C) else Color(0xFFAED4FF)
                    val addBorder = if (colors.isDark) Color(0xFF5DA5FA) else Color(0xFF4A90E2)
                    val addTint = if (colors.isDark) Color(0xFF9AC8FF) else Color(0xFF195DB8)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(addBg)
                            .border(1.dp, addBorder, RoundedCornerShape(6.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQuickAddClick(day)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Quick add on ${day.dayOfMonth}",
                            tint = addTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Smiley sticker button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = { onOpenStickerPickerForDate(day) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (daySticker != null) {
                            Text(text = daySticker.emoji, fontSize = 16.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.SentimentSatisfied,
                                contentDescription = "Choose sticker",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventBlock(
    item: PositionedTimelineEvent,
    colStartX: Dp,
    colWidth: Dp,
    isDayView: Boolean,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    val event = item.instance.event
    val topY = HOUR_HEIGHT * (item.startMinuteOfDay / 60f)
    val durationMinutes = (item.endMinuteOfDay - item.startMinuteOfDay).coerceAtLeast(30)
    val blockHeight = (HOUR_HEIGHT * (durationMinutes / 60f) - 2.dp).coerceAtLeast(22.dp)

    val subColWidth = colWidth / item.totalCols.toFloat()
    val xOffset = colStartX + subColWidth * item.colIndex + 2.dp
    val cardWidth = (subColWidth - 4.dp).coerceAtLeast(16.dp)

    Column(
        modifier = Modifier
            .offset(x = xOffset, y = topY + 1.dp)
            .width(cardWidth)
            .height(blockHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(event.colorTag.containerColor())
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = if (isDayView) 10.dp else 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = event.title,
            color = colors.textPrimary,
            fontSize = if (isDayView) 14.sp else 11.sp,
            lineHeight = if (isDayView) 17.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (event.location.isNotBlank() && blockHeight >= 42.dp) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.location,
                color = colors.textSecondary,
                fontSize = if (isDayView) 12.sp else 10.sp,
                lineHeight = if (isDayView) 15.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompressedClusterBlock(
    group: CompressedSlotGroup,
    colStartX: Dp,
    colWidth: Dp,
    onEventClick: (CalendarEvent) -> Unit,
    onExpandCluster: () -> Unit
) {
    val colors = OneUiTheme.colors
    val topY = HOUR_HEIGHT * (group.startMinuteOfDay / 60f)
    val xOffset = colStartX + 2.dp
    val cardWidth = (colWidth - 4.dp).coerceAtLeast(20.dp)

    Column(
        modifier = Modifier
            .offset(x = xOffset, y = topY + 1.dp)
            .width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        group.visibleEvents.forEach { inst ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(inst.event.colorTag.containerColor())
                    .clickable(
                        role = Role.Button,
                        onClick = { onEventClick(inst.event) }
                    )
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (inst.event.hasAlarm) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = inst.event.title,
                    color = colors.textPrimary,
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (group.hiddenEvents.isNotEmpty()) {
            val overflowBg = if (colors.isDark) colors.controlSubtleBackground else Color(0xFFEAECEF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(overflowBg)
                    .clickable(
                        role = Role.Button,
                        onClick = onExpandCluster
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${group.hiddenEvents.size}",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}
