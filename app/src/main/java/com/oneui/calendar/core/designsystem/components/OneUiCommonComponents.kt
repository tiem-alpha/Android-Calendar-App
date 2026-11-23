package com.oneui.calendar.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.designsystem.theme.OneUiShapes
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.domain.model.CalendarSticker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val QUICK_ADD_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.US)

private val WEEKDAYS_SUNDAY_FIRST = listOf("S", "M", "T", "W", "T", "F", "S")
private val WEEKDAYS_MONDAY_FIRST = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun OneUiCalendarTopBar(
    monthTitle: String,
    todayDayNumber: Int,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTodayClick: () -> Unit,
    onMonthTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.calendarGridBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open navigation menu",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search events",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTodayClick()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(
                                width = 1.5.dp,
                                color = colors.textPrimary,
                                shape = RoundedCornerShape(7.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = todayDayNumber.toString(),
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(OneUiShapes.FullPillShape)
                .clickable(
                    role = Role.Button,
                    onClick = onMonthTitleClick
                )
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthTitle.uppercase(Locale.US),
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun OneUiWeekdayHeaderRow(
    startOnMonday: Boolean = false,
    leadingSpacerWidth: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val strings = LocalAppStrings.current
    val labels = if (startOnMonday) strings.weekdaysMonFirst else strings.weekdaysSunFirst
    val sundayIndex = if (startOnMonday) 6 else 0
    val saturdayIndex = if (startOnMonday) 5 else 6

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingSpacerWidth > 0.dp) {
            Spacer(modifier = Modifier.width(leadingSpacerWidth))
        }

        labels.forEachIndexed { index, label ->
            val isWeekend = index == sundayIndex || index == saturdayIndex
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isWeekend) colors.sundayRed else colors.textSecondary
            )
        }
    }
}

@Composable
fun OneUiQuickAddBar(
    selectedDate: LocalDate,
    onQuickTextClick: () -> Unit,
    onFabAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current

    val pillInteraction = remember { MutableInteractionSource() }
    val isPillPressed by pillInteraction.collectIsPressedAsState()
    val pillScale by animateFloatAsState(
        targetValue = if (isPillPressed) 0.97f else 1f,
        animationSpec = if (isPillPressed) {
            tween(durationMillis = 95)
        } else {
            spring(stiffness = 480f, dampingRatio = 0.85f)
        },
        label = "quickAddPillScale"
    )

    val fabInteraction = remember { MutableInteractionSource() }
    val isFabPressed by fabInteraction.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.94f else 1f,
        animationSpec = if (isFabPressed) {
            tween(durationMillis = 95)
        } else {
            spring(stiffness = 480f, dampingRatio = 0.85f)
        },
        label = "quickAddFabScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .graphicsLayer {
                    scaleX = pillScale
                    scaleY = pillScale
                }
                .clip(OneUiShapes.FullPillShape)
                .background(colors.controlSubtleBackground)
                .clickable(
                    interactionSource = pillInteraction,
                    indication = androidx.compose.material3.ripple(),
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onQuickTextClick()
                    }
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val strings = LocalAppStrings.current
            Text(
                text = strings.formatQuickAddPlaceholder(selectedDate),
                color = colors.textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val fabModifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = fabScale
                scaleY = fabScale
            }
            .let { base ->
                if (colors.isDark) {
                    base
                        .clip(CircleShape)
                        .background(colors.elevatedCardBackground)
                        .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                } else {
                    base
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.16f),
                            spotColor = Color.Black.copy(alpha = 0.18f)
                        )
                        .clip(CircleShape)
                        .background(colors.elevatedCardBackground)
                }
            }
            .clickable(
                interactionSource = fabInteraction,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFabAddClick()
                }
            )

        Box(
            modifier = fabModifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Create event",
                tint = colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun OneUiFocusBlock(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OneUiShapes.FocusBlockShape,
        color = OneUiTheme.colors.focusBlockBackground,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun OneUiDottedDivider(
    modifier: Modifier = Modifier
) {
    val dotColor = if (OneUiTheme.colors.isDark) {
        Color(0x2EFFFFFF)
    } else {
        Color(0x26000000)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = dotColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
        )
    }
}

@Composable
fun OneUiDDayBadge(
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val headerRed = Color(0xFFE53935)
    val bodyBg = if (colors.isDark) Color(0xFF1F1F23) else Color.White
    val borderShape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .size(width = 22.dp, height = 24.dp)
            .clip(borderShape)
            .border(width = 1.2.dp, color = headerRed, shape = borderShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(headerRed)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bodyBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "D",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
fun OneUiAlarmBellButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current
    val activeBlue = Color(0xFF4263EB)

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f),
        label = "alarmBellScale"
    )

    Box(
        modifier = modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (isActive) activeBlue else colors.controlSubtleBackground)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(bounded = true),
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isActive) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
            contentDescription = if (isActive) "Alarm enabled" else "Alarm muted",
            tint = if (isActive) Color.White else colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun OneUiDaySubHeaderBar(
    lunarDateText: String,
    tempHighC: Int,
    tempLowC: Int,
    weatherEmoji: String,
    stickerEmoji: String?,
    onStickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lunarDateText,
                fontSize = 13.sp,
                color = colors.textSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(colors.dividerHairline)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$tempHighC C / $tempLowC C",
                fontSize = 13.sp,
                color = colors.textSecondary,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = weatherEmoji,
                fontSize = 15.sp
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = onStickerClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!stickerEmoji.isNullOrBlank()) {
                Text(
                    text = stickerEmoji,
                    fontSize = 20.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.SentimentSatisfied,
                    contentDescription = "Choose daily sticker",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneUiStickerPickerSheet(
    selectedStickerId: String?,
    onSelectSticker: (CalendarSticker?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = OneUiTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val stickerRows = remember { CalendarSticker.entries.chunked(5) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.elevatedCardBackground,
        scrimColor = Color.Black.copy(alpha = if (colors.isDark) 0.60f else 0.35f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.textTertiary.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sticker",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSticker(null)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Clear",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedStickerId != null) colors.accentBlue else colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stickerRows.forEach { rowStickers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowStickers.forEach { sticker ->
                            val isSelected = sticker.id.equals(selectedStickerId, ignoreCase = true)
                            val itemShape = RoundedCornerShape(18.dp)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(itemShape)
                                    .background(
                                        if (isSelected) {
                                            colors.accentBlue.copy(alpha = 0.14f)
                                        } else {
                                            colors.controlSubtleBackground
                                        }
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.5.dp, colors.accentBlue, itemShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        role = Role.Button,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectSticker(sticker)
                                            onDismiss()
                                        }
                                    )
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = sticker.emoji,
                                    fontSize = 26.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sticker.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) colors.accentBlue else colors.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OneUiGoToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = 0.86f
            )
        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(durationMillis = 150)
        ) + fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        val interaction = remember { MutableInteractionSource() }
        val isPressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.93f else 1f,
            animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f),
            label = "goToTopScale"
        )

        val glassBg = if (colors.isDark) {
            Color(0xFF1C1C20).copy(alpha = 0.82f)
        } else {
            Color.White.copy(alpha = 0.84f)
        }
        val glassBorder = if (colors.isDark) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.72f)
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (colors.isDark) {
                        Modifier
                    } else {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.14f)
                        )
                    }
                )
                .clip(CircleShape)
                .background(glassBg)
                .border(width = 1.dp, color = glassBorder, shape = CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = "Go to top",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
