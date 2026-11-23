package com.oneui.calendar.feature.month

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.common.LunarCalendarConverter
import com.oneui.calendar.core.designsystem.components.OneUiDaySubHeaderBar
import com.oneui.calendar.core.designsystem.components.OneUiDottedDivider
import com.oneui.calendar.core.designsystem.components.OneUiQuickAddBar
import com.oneui.calendar.core.designsystem.theme.OneUiShapes
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.CalendarSticker
import com.oneui.calendar.domain.model.DailyMetadata
import com.oneui.calendar.domain.model.EventInstance
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private const val POPUP_CAROUSEL_PAGES = 730
private const val POPUP_CENTER_PAGE = POPUP_CAROUSEL_PAGES / 2

@Composable
fun DayDetailPopupCarousel(
    initialDate: LocalDate,
    todayDate: LocalDate,
    instancesByDate: (LocalDate) -> List<EventInstance>,
    dailyMetadataMap: Map<String, DailyMetadata>,
    onDateChanged: (LocalDate) -> Unit = {},
    onDismiss: () -> Unit,
    onSwitchToDayView: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onToggleEventAlarm: (Long, Boolean) -> Unit,
    onOpenStickerPickerForDate: (LocalDate) -> Unit,
    onQuickAddClick: (LocalDate) -> Unit,
    onFabAddClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val scrimColor = Color.Black.copy(alpha = if (colors.isDark) 0.58f else 0.32f)
    val pagerState = rememberPagerState(
        initialPage = POPUP_CENTER_PAGE,
        pageCount = { POPUP_CAROUSEL_PAGES }
    )

    LaunchedEffect(pagerState, initialDate) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val activeDate = initialDate.plusDays((page - POPUP_CENTER_PAGE).toLong())
                onDateChanged(activeDate)
            }
    }

    val scrimInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scrimColor)
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 26.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageDate = initialDate.plusDays((page - POPUP_CENTER_PAGE).toLong())
            val dayInstances = instancesByDate(pageDate)
            val meta = dailyMetadataMap[pageDate.toString()]

            DayDetailPopupCard(
                date = pageDate,
                isToday = pageDate == todayDate,
                dayInstances = dayInstances,
                dailyMetadata = meta,
                onSwitchToDayView = { onSwitchToDayView(pageDate) },
                onEventClick = onEventClick,
                onToggleEventAlarm = onToggleEventAlarm,
                onOpenStickerPicker = { onOpenStickerPickerForDate(pageDate) },
                onQuickAddClick = { onQuickAddClick(pageDate) },
                onFabAddClick = { onFabAddClick(pageDate) }
            )
        }
    }
}

@Composable
private fun DayDetailPopupCard(
    date: LocalDate,
    isToday: Boolean,
    dayInstances: List<EventInstance>,
    dailyMetadata: DailyMetadata?,
    onSwitchToDayView: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onToggleEventAlarm: (Long, Boolean) -> Unit,
    onOpenStickerPicker: () -> Unit,
    onQuickAddClick: () -> Unit,
    onFabAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current
    val dayOfWeekName = remember(date) {
        date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.US)
    }
    val strings = LocalAppStrings.current
    val lunarSubDate = remember(date, strings.language) {
        strings.formatLunarSubDate(date)
    }
    val stickerEmoji = remember(dailyMetadata?.stickerId) {
        CalendarSticker.fromId(dailyMetadata?.stickerId)?.emoji
    }
    val firstAllDayIndex = remember(dayInstances) {
        dayInstances.indexOfFirst { it.event.isAllDay }
    }
    val cardInteraction = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.73f)
            .then(
                if (colors.isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = OneUiShapes.PopupCardShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = {} // Consume clicks inside popup card so scrim doesn't dismiss
            ),
        shape = OneUiShapes.PopupCardShape,
        color = colors.elevatedCardBackground,
        shadowElevation = if (colors.isDark) 0.dp else 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Card Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeBg = if (isToday) colors.todayBadgeBg else colors.controlSubtleBackground
                    val badgeText = when {
                        isToday -> colors.todayBadgeText
                        date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sundayRed
                        else -> colors.textPrimary
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = badgeText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = dayOfWeekName,
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSwitchToDayView()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    TimelineScheduleGlyph(
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            OneUiDottedDivider(
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            OneUiDaySubHeaderBar(
                lunarDateText = lunarSubDate,
                tempHighC = dailyMetadata?.tempHighC ?: 12,
                tempLowC = dailyMetadata?.tempLowC ?: 8,
                weatherEmoji = dailyMetadata?.weatherEmoji ?: "☀️",
                stickerEmoji = stickerEmoji,
                onStickerClick = onOpenStickerPicker,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )

            if (dayInstances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events",
                        color = colors.textTertiary,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = dayInstances,
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
                    }
                }
            }

            OneUiQuickAddBar(
                selectedDate = date,
                onQuickTextClick = onQuickAddClick,
                onFabAddClick = onFabAddClick,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

/**
 * Draws the One UI timeline-with-clock glyph (≡🕒) matching the top-right button in Screenshot 3.
 */
@Composable
private fun TimelineScheduleGlyph(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height

        // Three horizontal schedule lines
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.25f),
            end = Offset(w * 0.86f, h * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.48f),
            end = Offset(w * 0.56f, h * 0.48f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.71f),
            end = Offset(w * 0.44f, h * 0.71f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Small clock circle at bottom-right
        val center = Offset(w * 0.72f, h * 0.66f)
        val radius = w * 0.22f
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius * 0.45f, center.y + radius * 0.15f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}
