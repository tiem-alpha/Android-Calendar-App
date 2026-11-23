package com.oneui.calendar.feature.month

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.oneui.calendar.core.common.HolidayProvider
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.common.LunarCalendarConverter
import com.oneui.calendar.core.common.MonthMultiDaySlotEngine
import com.oneui.calendar.core.common.WeekEventRibbon
import com.oneui.calendar.core.common.WeekRowLayout
import com.oneui.calendar.core.designsystem.components.OneUiAlarmBellButton
import com.oneui.calendar.core.designsystem.components.OneUiDDayBadge
import com.oneui.calendar.core.designsystem.components.OneUiDaySubHeaderBar
import com.oneui.calendar.core.designsystem.components.OneUiQuickAddBar
import com.oneui.calendar.core.designsystem.components.OneUiWeekdayHeaderRow
import com.oneui.calendar.core.designsystem.theme.OneUiShapes
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.core.designsystem.theme.accentBarColor
import com.oneui.calendar.core.designsystem.theme.containerColor
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.CalendarSticker
import com.oneui.calendar.domain.model.DailyMetadata
import com.oneui.calendar.domain.model.EventInstance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun MonthScreen(
    currentYearMonth: YearMonth,
    selectedDate: LocalDate,
    todayDate: LocalDate,
    isSplitMode: Boolean,
    monthInstances: List<EventInstance>,
    selectedDayInstances: List<EventInstance>,
    dailyMetadataMap: Map<String, DailyMetadata>,
    onSelectDate: (LocalDate) -> Unit,
    onToggleSplitMode: (Boolean) -> Unit,
    onOpenDayDetailPopup: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onToggleEventAlarm: (Long, Boolean) -> Unit,
    onOpenStickerPickerForDate: (LocalDate) -> Unit,
    onQuickAddClick: (LocalDate) -> Unit,
    onFabAddClick: (LocalDate) -> Unit,
    showLunarInCells: Boolean = true,
    showGlobalHolidays: Boolean = true,
    startWeekOnMonday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 48.dp.toPx() }

    val weekDaysList = remember(currentYearMonth, startWeekOnMonday) {
        MonthMultiDaySlotEngine.buildMonthWeeks(currentYearMonth, startWeekOnMonday = startWeekOnMonday)
    }

    val weekLayouts = remember(weekDaysList, monthInstances) {
        weekDaysList.map { weekDays ->
            MonthMultiDaySlotEngine.layoutWeekRow(
                weekDays = weekDays,
                instances = monthInstances,
                maxSlots = 3
            )
        }
    }

    val gridHeightFraction by animateFloatAsState(
        targetValue = if (isSplitMode) 0.52f else 1f,
        animationSpec = spring(stiffness = 320f, dampingRatio = 0.85f),
        label = "monthGridHeightFraction"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.calendarGridBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            OneUiWeekdayHeaderRow(
                startOnMonday = startWeekOnMonday,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val totalHeight = maxHeight
                val fullBottomReserve = 76.dp
                val fullProgress = ((gridHeightFraction - 0.52f) / 0.48f).coerceIn(0f, 1f)
                val effectiveGridHeight = (totalHeight - fullBottomReserve * fullProgress) * gridHeightFraction
                val agendaHeight = (totalHeight - effectiveGridHeight).coerceAtLeast(0.dp)

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(effectiveGridHeight)
                            .pointerInput(currentYearMonth, isSplitMode) {
                                var dragX = 0f
                                var dragY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragX += dragAmount.x
                                        dragY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        if (abs(dragX) > abs(dragY) && abs(dragX) > swipeThresholdPx) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (dragX > 0f) onPrevMonth() else onNextMonth()
                                        } else if (abs(dragY) > abs(dragX) && abs(dragY) > swipeThresholdPx) {
                                            if (dragY < 0f && !isSplitMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToggleSplitMode(true)
                                            } else if (dragY > 0f && isSplitMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToggleSplitMode(false)
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        weekLayouts.forEach { weekLayout ->
                            MonthWeekRow(
                                weekLayout = weekLayout,
                                currentYearMonth = currentYearMonth,
                                selectedDate = selectedDate,
                                todayDate = todayDate,
                                isSplitMode = isSplitMode,
                                showLunarInCells = showLunarInCells,
                                showGlobalHolidays = showGlobalHolidays,
                                dailyMetadataMap = dailyMetadataMap,
                                onDayClick = { date ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (date == selectedDate) {
                                        onOpenDayDetailPopup(date)
                                    } else {
                                        onSelectDate(date)
                                    }
                                },
                                onEventClick = onEventClick,
                                onOpenStickerPickerForDate = onOpenStickerPickerForDate,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isSplitMode || agendaHeight > 40.dp,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        SplitAgendaPanel(
                            selectedDate = selectedDate,
                            isSplitMode = isSplitMode,
                            selectedDayInstances = selectedDayInstances,
                            dailyMetadata = dailyMetadataMap[selectedDate.toString()],
                            onToggleSplitMode = { onToggleSplitMode(!isSplitMode) },
                            onOpenStickerPicker = { onOpenStickerPickerForDate(selectedDate) },
                            onEventClick = onEventClick,
                            onToggleEventAlarm = onToggleEventAlarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(agendaHeight)
                        )
                    }
                }
            }
        }

        // Bottom subtle fade mask + quick-add bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (isSplitMode) {
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
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.calendarGridBackground.copy(alpha = if (isSplitMode) 0.96f else 1f))
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
private fun MonthWeekRow(
    weekLayout: WeekRowLayout,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate,
    todayDate: LocalDate,
    isSplitMode: Boolean,
    showLunarInCells: Boolean,
    showGlobalHolidays: Boolean,
    dailyMetadataMap: Map<String, DailyMetadata>,
    onDayClick: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onOpenStickerPickerForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors

    Box(
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // Top week hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.dividerHairline)
                .align(Alignment.TopCenter)
        )

        // 7 interactive day cells (background layer + day number + selection border + sticker)
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            weekLayout.days.forEachIndexed { colIndex, date ->
                val isSelected = date == selectedDate
                val isToday = date == todayDate
                val isInMonth = YearMonth.from(date) == currentYearMonth
                val isWeekend = date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY
                val strings = LocalAppStrings.current
                val holiday = remember(date, showGlobalHolidays, strings.language) {
                    if (showGlobalHolidays) HolidayProvider.getHolidayForDate(date, strings.language) else null
                }
                val isRedDay = isWeekend || holiday != null
                val meta = dailyMetadataMap[date.toString()]
                val daySticker = CalendarSticker.fromId(meta?.stickerId)
                val cellInteraction = remember(date) { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp, vertical = 2.dp)
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
                            interactionSource = cellInteraction,
                            indication = ripple(bounded = true),
                            role = Role.Button,
                            onClick = { onDayClick(date) }
                        )
                ) {
                    // Day number + optional mini lunar / holiday date at top center
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(OneUiShapes.TodaySquircleShape)
                                        .background(colors.todayBadgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        color = colors.todayBadgeText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            } else {
                                val numColor = (if (isRedDay) colors.sundayRed else colors.textPrimary)
                                    .copy(alpha = if (isInMonth) 1f else 0.35f)
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    color = numColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (holiday != null) FontWeight.SemiBold else FontWeight.Medium,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                        }

                        if (holiday != null) {
                            Text(
                                text = holiday.name,
                                color = colors.sundayRed.copy(alpha = if (isInMonth) 0.95f else 0.4f),
                                fontSize = 8.sp,
                                lineHeight = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (showLunarInCells) {
                            val lunarLabel = remember(date, strings.language) {
                                strings.formatMiniLunarCell(date)
                            }
                            Text(
                                text = lunarLabel,
                                color = colors.textTertiary.copy(alpha = if (isInMonth) 0.9f else 0.35f),
                                fontSize = 8.5.sp,
                                lineHeight = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }

                    // Bottom sticker / smiley icon in Full Month mode (matching cell 22 in Screenshot 1)
                    if (!isSplitMode && (isSelected || daySticker != null)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onOpenStickerPickerForDate(date) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (daySticker != null) {
                                Text(
                                    text = daySticker.emoji,
                                    fontSize = 15.sp
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.SentimentSatisfied,
                                    contentDescription = "Choose sticker for ${date.dayOfMonth}",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else if (!isSplitMode && weekLayout.overflowCountPerCol[colIndex] > 0) {
                        Text(
                            text = "+${weekLayout.overflowCountPerCol[colIndex]}",
                            color = colors.textTertiary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 3.dp, bottom = 2.dp)
                        )
                    }
                }
            }
        }

        // Event Ribbons overlay spanning columns startCol..endCol
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (showLunarInCells) 37.dp else 30.dp)
        ) {
            val colWidth = maxWidth / 7f

            if (!isSplitMode) {
                val slotHeight = 17.dp
                val slotSpacing = 2.dp
                weekLayout.ribbons.forEach { ribbon ->
                    val topOffset = (slotHeight + slotSpacing) * ribbon.slotIndex
                    val startX = colWidth * ribbon.startCol + 2.dp
                    val spanWidth = (colWidth * (ribbon.endCol - ribbon.startCol + 1) - 4.dp)
                        .coerceAtLeast(16.dp)
                    val isOutOfMonth = YearMonth.from(ribbon.instance.startDate) != currentYearMonth &&
                        YearMonth.from(ribbon.instance.endDate) != currentYearMonth

                    FullMonthRibbonItem(
                        ribbon = ribbon,
                        width = spanWidth,
                        height = slotHeight,
                        isOutOfMonth = isOutOfMonth,
                        onClick = { onEventClick(ribbon.instance.event) },
                        modifier = Modifier.offset(x = startX, y = topOffset)
                    )
                }
            } else {
                val barHeight = 4.dp
                val barSpacing = 3.dp
                weekLayout.ribbons.forEach { ribbon ->
                    val topOffset = (barHeight + barSpacing) * ribbon.slotIndex
                    val startX = colWidth * ribbon.startCol + 3.dp
                    val spanWidth = (colWidth * (ribbon.endCol - ribbon.startCol + 1) - 6.dp)
                        .coerceAtLeast(8.dp)

                    Box(
                        modifier = Modifier
                            .offset(x = startX, y = topOffset)
                            .width(spanWidth)
                            .height(barHeight)
                            .clip(CircleShape)
                            .background(ribbon.instance.event.colorTag.accentBarColor())
                    )
                }
            }
        }
    }
}

@Composable
private fun FullMonthRibbonItem(
    ribbon: WeekEventRibbon,
    width: Dp,
    height: Dp,
    isOutOfMonth: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val itemAlpha = if (isOutOfMonth) 0.45f else 1f

    if (ribbon.isBanner) {
        val isMultiColumn = ribbon.endCol > ribbon.startCol
        Box(
            modifier = modifier
                .width(width)
                .height(height)
                .alpha(itemAlpha)
                .clip(RoundedCornerShape(4.dp))
                .background(ribbon.instance.event.colorTag.containerColor())
                .clickable(
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = 3.dp),
            contentAlignment = if (isMultiColumn) Alignment.Center else Alignment.CenterStart
        ) {
            Text(
                text = ribbon.instance.event.title,
                color = colors.textPrimary,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isMultiColumn) TextAlign.Center else TextAlign.Start
            )
        }
    } else {
        Row(
            modifier = modifier
                .width(width)
                .height(height)
                .alpha(itemAlpha)
                .clip(RoundedCornerShape(3.dp))
                .clickable(
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(ribbon.instance.event.colorTag.accentBarColor())
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = ribbon.instance.event.title,
                color = colors.textPrimary,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SplitAgendaPanel(
    selectedDate: LocalDate,
    isSplitMode: Boolean,
    selectedDayInstances: List<EventInstance>,
    dailyMetadata: DailyMetadata?,
    onToggleSplitMode: () -> Unit,
    onOpenStickerPicker: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onToggleEventAlarm: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val strings = LocalAppStrings.current
    val lunarSubDate = remember(selectedDate, strings.language) {
        strings.formatLunarSubDate(selectedDate)
    }
    val stickerEmoji = remember(dailyMetadata?.stickerId) {
        CalendarSticker.fromId(dailyMetadata?.stickerId)?.emoji
    }
    val firstAllDayIndex = remember(selectedDayInstances) {
        selectedDayInstances.indexOfFirst { it.event.isAllDay }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.calendarGridBackground)
    ) {
        // Top divider & tactile split handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.dividerHairline)
        )

        val dragDownThresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(isSplitMode) {
                    var totalY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalY = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            totalY += dragAmount
                        },
                        onDragEnd = {
                            if (totalY > dragDownThresholdPx && isSplitMode) {
                                onToggleSplitMode()
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = onToggleSplitMode
                    )
                    .padding(top = 6.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSplitMode) {
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 3.5.dp)
                            .clip(CircleShape)
                            .background(colors.textTertiary.copy(alpha = 0.28f))
                    )
                }
            }

            OneUiDaySubHeaderBar(
                lunarDateText = lunarSubDate,
                tempHighC = dailyMetadata?.tempHighC ?: 12,
                tempLowC = dailyMetadata?.tempLowC ?: 8,
                weatherEmoji = dailyMetadata?.weatherEmoji ?: "☀️",
                stickerEmoji = stickerEmoji,
                onStickerClick = onOpenStickerPicker,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }

        if (selectedDayInstances.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 84.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events",
                    color = colors.textTertiary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
            ) {
                itemsIndexed(
                    items = selectedDayInstances,
                    key = { index, inst -> "${inst.event.id}_${inst.instanceStartMillis}_$index" }
                ) { index, instance ->
                    SplitAgendaEventRow(
                        instance = instance,
                        showAllDayIcon = instance.event.isAllDay && index == firstAllDayIndex,
                        onClick = { onEventClick(instance.event) },
                        onToggleAlarm = {
                            onToggleEventAlarm(instance.event.id, !instance.event.hasAlarm)
                        }
                    )

                    if (index < selectedDayInstances.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 76.dp, end = 20.dp)
                                .height(1.dp)
                                .background(colors.dividerHairline)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SplitAgendaEventRow(
    instance: EventInstance,
    showAllDayIcon: Boolean,
    onClick: () -> Unit,
    onToggleAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val event = instance.event
    val subtitle = event.location.ifBlank { instance.formattedTimeRange() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left time / all-day icon column
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (event.isAllDay) {
                if (showAllDayIcon) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = "All day event",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = instance.formattedStartTimeShort(),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1
                )
            }
        }

        // Vertical Pastel Pill
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(event.colorTag.accentBarColor())
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Event title & subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = event.title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Trailing badge: D-Day or Alarm Bell
        if (event.isDDay) {
            Spacer(modifier = Modifier.width(10.dp))
            OneUiDDayBadge()
        } else if (event.hasAlarm) {
            Spacer(modifier = Modifier.width(10.dp))
            OneUiAlarmBellButton(
                isActive = true,
                onClick = onToggleAlarm
            )
        }
    }
}
