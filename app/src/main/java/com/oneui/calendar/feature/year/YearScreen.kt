package com.oneui.calendar.feature.year

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.MonthMultiDaySlotEngine
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.core.designsystem.theme.accentBarColor
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.EventInstance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs

private val MINI_WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun YearScreen(
    currentYear: Int,
    todayDate: LocalDate,
    selectedDate: LocalDate,
    yearInstances: List<EventInstance>,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 48.dp.toPx() }

    val months = remember(currentYear) {
        (1..12).map { month -> YearMonth.of(currentYear, month) }
    }

    // Build fast lookup map of LocalDate -> EventColorTag for event indicator dots
    val eventColorByDate = remember(yearInstances, currentYear) {
        val map = HashMap<LocalDate, EventColorTag>()
        val yearStart = LocalDate.of(currentYear, 1, 1)
        val yearEnd = LocalDate.of(currentYear, 12, 31)
        yearInstances.forEach { inst ->
            var cursor = if (inst.startDate.isBefore(yearStart)) yearStart else inst.startDate
            val end = if (inst.endDate.isAfter(yearEnd)) yearEnd else inst.endDate
            while (!cursor.isAfter(end)) {
                map.putIfAbsent(cursor, inst.event.colorTag)
                cursor = cursor.plusDays(1)
            }
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground)
            .pointerInput(currentYear) {
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
                            if (dragX > 0f) onPrevYear() else onNextYear()
                        }
                    }
                )
            }
    ) {
        // Year Selector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPrevYear()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous year",
                    tint = colors.textPrimary
                )
            }

            Text(
                text = currentYear.toString(),
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum")
            )

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNextYear()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next year",
                    tint = colors.textPrimary
                )
            }
        }

        // 3x4 Grid of 12 Mini-Month Focus Blocks
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = months,
                key = { it.toString() }
            ) { yearMonth ->
                MiniMonthFocusBlock(
                    yearMonth = yearMonth,
                    todayDate = todayDate,
                    selectedDate = selectedDate,
                    eventColorByDate = eventColorByDate,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMonthClick(yearMonth)
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniMonthFocusBlock(
    yearMonth: YearMonth,
    todayDate: LocalDate,
    selectedDate: LocalDate,
    eventColorByDate: Map<LocalDate, EventColorTag>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val isCurrentMonth = YearMonth.from(todayDate) == yearMonth
    val monthName = remember(yearMonth) {
        yearMonth.month.getDisplayName(JavaTextStyle.SHORT, Locale.US)
    }
    val weeks = remember(yearMonth) {
        MonthMultiDaySlotEngine.buildMonthWeeks(yearMonth, startWeekOnMonday = false)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = colors.focusBlockBackground,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = monthName,
                color = if (isCurrentMonth) colors.accentBlue else colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )

            // Mini weekday letters
            Row(modifier = Modifier.fillMaxWidth()) {
                MINI_WEEKDAYS.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (idx == 0) colors.sundayRed else colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 5 or 6 week rows (pad to 6 rows height consistency)
            for (rowIndex in 0 until 6) {
                val week = weeks.getOrNull(rowIndex)
                if (week == null) {
                    Spacer(modifier = Modifier.height(17.dp))
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        week.forEach { date ->
                            val inMonth = YearMonth.from(date) == yearMonth
                            val isToday = date == todayDate
                            val isSelected = date == selectedDate && !isToday
                            val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
                            val eventTag = if (inMonth) eventColorByDate[date] else null

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!inMonth) {
                                    Text(text = "", fontSize = 8.5.sp)
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .then(
                                                    when {
                                                        isToday -> Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colors.todayBadgeBg)
                                                        isSelected -> Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colors.accentBlue.copy(alpha = 0.16f))
                                                        else -> Modifier
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                fontSize = 8.5.sp,
                                                lineHeight = 9.sp,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                style = TextStyle(fontFeatureSettings = "tnum"),
                                                color = when {
                                                    isToday -> colors.todayBadgeText
                                                    isSunday -> colors.sundayRed
                                                    else -> colors.textPrimary
                                                }
                                            )
                                        }
                                        if (eventTag != null && !isToday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(eventTag.accentBarColor())
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
