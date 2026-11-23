package com.oneui.calendar.feature.reminder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.AppLanguage
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.designsystem.components.OneUiDDayBadge
import com.oneui.calendar.core.designsystem.components.OneUiDottedDivider
import com.oneui.calendar.core.designsystem.components.OneUiFocusBlock
import com.oneui.calendar.core.designsystem.components.OneUiGoToTopButton
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.core.designsystem.theme.accentBarColor
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.ReminderItem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val REMINDER_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

enum class ReminderFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    D_DAY("D-Day Events")
}

enum class ThemePreference(val label: String) {
    SYSTEM("System"),
    LIGHT("Light Pastel"),
    DARK("OLED Dark")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    reminders: List<ReminderItem>,
    dDayEvents: List<CalendarEvent> = emptyList(),
    onAddReminder: (title: String, triggerTimeMillis: Long, colorTag: EventColorTag) -> Unit,
    onToggleReminderCompleted: (Long) -> Unit,
    onDeleteReminders: (Set<Long>) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenEventEditor: ((CalendarEvent) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeFilter by remember { mutableStateOf(ReminderFilter.ALL) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var quickAddTitle by remember { mutableStateOf("") }
    var quickAddColor by remember { mutableStateOf(EventColorTag.PASTEL_BLUE) }

    val pendingCount = remember(reminders) { reminders.count { !it.isCompleted } }

    val filteredReminders = remember(reminders, activeFilter) {
        when (activeFilter) {
            ReminderFilter.ALL -> reminders.sortedWith(
                compareBy<ReminderItem> { it.isCompleted }.thenBy { it.triggerTimeMillis }
            )
            ReminderFilter.PENDING -> reminders.filter { !it.isCompleted }.sortedBy { it.triggerTimeMillis }
            ReminderFilter.COMPLETED -> reminders.filter { it.isCompleted }.sortedByDescending { it.triggerTimeMillis }
            ReminderFilter.D_DAY -> emptyList()
        }
    }

    val collapseProgress by remember {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / 240f).coerceIn(0f, 1f)
            }
        }
    }
    val expandedAlpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
    val collapsedAlpha = ((collapseProgress - 0.5f) * 2f).coerceIn(0f, 1f)
    val showGoToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 || listState.firstVisibleItemScrollOffset > 300
        }
    }

    fun exitMultiSelect() {
        multiSelectMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) {
            multiSelectMode = false
        }
    }

    fun submitQuickAdd() {
        val trimmed = quickAddTitle.trim()
        if (trimmed.isEmpty()) return
        val triggerMillis = System.currentTimeMillis() + 3_600_000L
        onAddReminder(trimmed, triggerMillis, quickAddColor)
        quickAddTitle = ""
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val ambientBrush = if (colors.isDark) {
        Brush.verticalGradient(
            listOf(Color(0xFF141E2B).copy(alpha = 0.35f), colors.canvasBackground)
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFEAF3FF), colors.canvasBackground)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Viewing Area / Expandable Collapsible Toolbar (Top ~32% when at top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ambientBrush)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val headerHeight by animateDpAsState(
                        targetValue = if (collapseProgress > 0.65f || multiSelectMode) 0.dp else 132.dp,
                        animationSpec = tween(220),
                        label = "reminderExpandedHeight"
                    )
                    if (headerHeight > 0.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerHeight)
                                .alpha(expandedAlpha)
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Reminders",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = (-0.5).sp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$pendingCount pending reminder${if (pendingCount == 1) "" else "s"}",
                                fontSize = 14.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Bottom Toolbar Row (Collapsed Bar / Multi-select Bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (multiSelectMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val allSelected = filteredReminders.isNotEmpty() &&
                                    selectedIds.size == filteredReminders.size
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(role = Role.Checkbox) {
                                            selectedIds = if (allSelected) {
                                                emptySet()
                                            } else {
                                                filteredReminders.map { it.id }.toSet()
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularCheckIndicator(
                                        checked = allSelected,
                                        accentColor = colors.accentBlue
                                    )
                                    Text(
                                        text = "All",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = "${selectedIds.size} selected",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }

                            Text(
                                text = "Done",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentBlue,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { exitMultiSelect() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onOpenDrawer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = "Open navigation menu",
                                        tint = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = "Reminders",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.alpha(collapsedAlpha)
                                )
                            }

                            if (filteredReminders.isNotEmpty()) {
                                Text(
                                    text = "Select",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentBlue,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(role = Role.Button) {
                                            multiSelectMode = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderFilter.entries.forEach { filter ->
                    val isSelected = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) colors.accentBlue
                                else colors.controlSubtleBackground
                            )
                            .clickable(role = Role.Tab) {
                                activeFilter = filter
                                exitMultiSelect()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else colors.textPrimary
                        )
                    }
                }
            }

            // Main Content List
            Box(modifier = Modifier.weight(1f)) {
                val showEmpty = when (activeFilter) {
                    ReminderFilter.D_DAY -> dDayEvents.isEmpty()
                    ReminderFilter.ALL -> reminders.isEmpty() && dDayEvents.isEmpty()
                    else -> filteredReminders.isEmpty()
                }

                if (showEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "No ${activeFilter.label.lowercase(Locale.US)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Use the quick reminder bar below to add a timed reminder.",
                                fontSize = 14.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 84.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (activeFilter != ReminderFilter.D_DAY && filteredReminders.isNotEmpty()) {
                            item {
                                OneUiFocusBlock(
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    filteredReminders.forEachIndexed { index, reminder ->
                                        val isSelected = reminder.id in selectedIds
                                        ReminderRow(
                                            reminder = reminder,
                                            multiSelectMode = multiSelectMode,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (multiSelectMode) {
                                                    toggleSelection(reminder.id)
                                                } else {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onToggleReminderCompleted(reminder.id)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                multiSelectMode = true
                                                selectedIds = selectedIds + reminder.id
                                            }
                                        )
                                        if (index < filteredReminders.lastIndex) {
                                            OneUiDottedDivider(modifier = Modifier.padding(horizontal = 20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if ((activeFilter == ReminderFilter.ALL || activeFilter == ReminderFilter.D_DAY) &&
                            dDayEvents.isNotEmpty()
                        ) {
                            item {
                                Text(
                                    text = "D-Day Countdown Events",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            item {
                                OneUiFocusBlock(
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    dDayEvents.forEachIndexed { index, event ->
                                        DDayEventRow(
                                            event = event,
                                            onClick = { onOpenEventEditor?.invoke(event) }
                                        )
                                        if (index < dDayEvents.lastIndex) {
                                            OneUiDottedDivider(modifier = Modifier.padding(horizontal = 20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OneUiGoToTopButton(
                    visible = showGoToTop && !multiSelectMode,
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }

            // Bottom Quick-Add Bar OR Multi-Select Action Bar
            AnimatedVisibility(
                visible = multiSelectMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.elevatedCardBackground,
                    shadowElevation = if (colors.isDark) 0.dp else 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = selectedIds.isNotEmpty()) {
                                    selectedIds.forEach { id -> onToggleReminderCompleted(id) }
                                    exitMultiSelect()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Complete selected",
                                tint = colors.accentBlue
                            )
                            Text(
                                text = "Complete",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentBlue
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = selectedIds.isNotEmpty()) {
                                    onDeleteReminders(selectedIds)
                                    exitMultiSelect()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete selected",
                                tint = colors.sundayRed
                            )
                            Text(
                                text = "Delete",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.sundayRed
                            )
                        }
                    }
                }
            }

            if (!multiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick color cycler dot
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.focusBlockBackground)
                            .clickable(role = Role.Button) {
                                val entries = EventColorTag.entries
                                quickAddColor = entries[(quickAddColor.ordinal + 1) % entries.size]
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(quickAddColor.accentBarColor())
                        )
                    }

                    // Pill Quick Add Text Input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.controlSubtleBackground)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (quickAddTitle.isEmpty()) {
                            Text(
                                text = "Add reminder...",
                                fontSize = 15.sp,
                                color = colors.textTertiary
                            )
                        }
                        BasicTextField(
                            value = quickAddTitle,
                            onValueChange = { quickAddTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(colors.accentBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitQuickAdd() })
                        )
                    }

                    // Circular Add Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.accentBlue)
                            .clickable(role = Role.Button, onClick = { submitQuickAdd() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add reminder",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(
    reminder: ReminderItem,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    val triggerText = remember(reminder.triggerTimeMillis) {
        val zdt = Instant.ofEpochMilli(reminder.triggerTimeMillis).atZone(ZoneId.systemDefault())
        REMINDER_TIME_FORMATTER.format(zdt)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multiSelectMode) {
            CircularCheckIndicator(
                checked = isSelected,
                accentColor = colors.accentBlue
            )
            Spacer(modifier = Modifier.width(14.dp))
        } else {
            CircularCheckIndicator(
                checked = reminder.isCompleted,
                accentColor = reminder.colorTag.accentBarColor()
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(reminder.colorTag.accentBarColor())
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (reminder.isCompleted) colors.textTertiary else colors.textPrimary,
                textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = triggerText,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun DDayEventRow(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    val today = remember { LocalDate.now() }
    val eventDate = remember(event.startTimeMillis, event.timeZone) {
        val zone = runCatching { ZoneId.of(event.timeZone) }.getOrDefault(ZoneId.systemDefault())
        Instant.ofEpochMilli(event.startTimeMillis).atZone(zone).toLocalDate()
    }
    val dDayLabel = remember(today, eventDate) {
        val diff = ChronoUnit.DAYS.between(today, eventDate)
        when {
            diff == 0L -> "D-Day"
            diff > 0L -> "D-$diff"
            else -> "D+${-diff}"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(event.colorTag.accentBarColor())
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = eventDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)),
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = dDayLabel,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentBlue,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.accentBlue.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OneUiDDayBadge()
    }
}

@Composable
private fun CircularCheckIndicator(
    checked: Boolean,
    accentColor: Color
) {
    val colors = OneUiTheme.colors
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(accentColor)
                } else {
                    Modifier.border(1.6.dp, colors.textTertiary, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun SettingsBottomSheetOrScreen(
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    themePreference: ThemePreference,
    startWeekOnMonday: Boolean,
    showLunarCalendar: Boolean,
    showGlobalHolidays: Boolean = true,
    selectedTimeZoneId: String = "Asia/Ho_Chi_Minh",
    weatherCity: String,
    useFahrenheit: Boolean,
    isSyncing: Boolean = false,
    lastSyncStatus: String? = null,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onStartWeekOnMondayChange: (Boolean) -> Unit,
    onShowLunarCalendarChange: (Boolean) -> Unit,
    onShowGlobalHolidaysChange: (Boolean) -> Unit = {},
    onTimeZoneChange: (String) -> Unit = {},
    onWeatherCityChange: (String) -> Unit,
    onUseFahrenheitChange: (Boolean) -> Unit,
    onSyncSystemCalendarsNow: () -> Unit,
    onRefreshWeatherNow: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    var cityInput by remember(weatherCity) { mutableStateOf(weatherCity) }

    val ambientSettingsBg = if (colors.isDark) {
        Brush.verticalGradient(listOf(Color(0xFF141E2B).copy(alpha = 0.45f), colors.canvasBackground))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEAF3FF), colors.canvasBackground))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ambientSettingsBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = strings.settingsTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.appearanceSection,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            OneUiFocusBlock(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = strings.languageLabel,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == appLanguage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) colors.accentBlue
                                    else colors.controlSubtleBackground
                                )
                                .clickable(role = Role.RadioButton) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAppLanguageChange(lang)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.nativeLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                Text(
                    text = strings.themeModeLabel,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreference.entries.forEach { pref ->
                        val isSelected = pref == themePreference
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) colors.accentBlue
                                    else colors.controlSubtleBackground
                                )
                                .clickable(role = Role.RadioButton) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onThemePreferenceChange(pref)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val prefLabel = when (pref) {
                                ThemePreference.SYSTEM -> strings.themeSystem
                                ThemePreference.LIGHT -> strings.themeLight
                                ThemePreference.DARK -> strings.themeDark
                            }
                            Text(
                                text = prefLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                SettingsSwitchRow(
                    title = strings.startWeekOnMonday,
                    subtitle = if (startWeekOnMonday) strings.weekBeginsMonday else strings.weekBeginsSunday,
                    checked = startWeekOnMonday,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStartWeekOnMondayChange(it)
                    }
                )

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                SettingsSwitchRow(
                    title = strings.showLunarCalendar,
                    subtitle = strings.showLunarSubtitle,
                    checked = showLunarCalendar,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShowLunarCalendarChange(it)
                    }
                )

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                SettingsSwitchRow(
                    title = when (strings.language) {
                        AppLanguage.ENGLISH -> "Global Holidays (US/UK)"
                        AppLanguage.VIETNAMESE -> "Ngày nghỉ lễ Quốc gia (Việt Nam)"
                        AppLanguage.CHINESE -> "法定节假日 (中国)"
                    },
                    subtitle = when (strings.language) {
                        AppLanguage.ENGLISH -> "Highlight public holidays in red"
                        AppLanguage.VIETNAMESE -> "Tô đỏ ngày lễ Tết & Quốc khánh trên lịch"
                        AppLanguage.CHINESE -> "在日历中用红色标记节假日"
                    },
                    checked = showGlobalHolidays,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShowGlobalHolidaysChange(it)
                    }
                )

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                Text(
                    text = when (strings.language) {
                        AppLanguage.ENGLISH -> "Time Zone"
                        AppLanguage.VIETNAMESE -> "Múi giờ (Time Zone)"
                        AppLanguage.CHINESE -> "时区 (Time Zone)"
                    },
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                val timeZones = listOf(
                    "Asia/Ho_Chi_Minh" to "UTC+7 (VN)",
                    "Asia/Shanghai" to "UTC+8 (CN)",
                    "America/New_York" to "UTC-5 (US)",
                    "Europe/London" to "UTC+0 (UK)"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeZones.forEach { (tzId, tzLabel) ->
                        val isTzSelected = selectedTimeZoneId == tzId
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTzSelected) colors.accentBlue
                                    else colors.controlSubtleBackground
                                )
                                .clickable(role = Role.RadioButton) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTimeZoneChange(tzId)
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tzLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTzSelected) Color.White else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Text(
                text = strings.weatherSection,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            OneUiFocusBlock(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = strings.weatherCityLabel,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.controlSubtleBackground)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (cityInput.isEmpty()) {
                            Text(
                                text = "City name",
                                fontSize = 15.sp,
                                color = colors.textTertiary
                            )
                        }
                        BasicTextField(
                            value = cityInput,
                            onValueChange = {
                                cityInput = it
                                onWeatherCityChange(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(colors.accentBlue),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(colors.controlSubtleBackground)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TempUnitPill(
                            label = "°C",
                            selected = !useFahrenheit,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUseFahrenheitChange(false)
                            }
                        )
                        TempUnitPill(
                            label = "°F",
                            selected = useFahrenheit,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUseFahrenheitChange(true)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(colors.controlSubtleBackground)
                        .clickable(enabled = !isSyncing, role = Role.Button) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRefreshWeatherNow()
                        }
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.refreshWeatherBtn,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }

            Text(
                text = strings.accountsSyncSection,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            OneUiFocusBlock(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.accentBlue)
                        .clickable(enabled = !isSyncing, role = Role.Button) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSyncSystemCalendarsNow()
                        }
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.syncSystemCalendarsBtn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!lastSyncStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = lastSyncStatus,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = OneUiTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            colors.accentBlue
        } else {
            if (colors.isDark) Color(0xFF525256) else Color(0xFFB8B8BC)
        },
        animationSpec = tween(180),
        label = "settingsSwitchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f),
        label = "settingsSwitchThumb"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.5.sp,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun TempUnitPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accentBlue else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else colors.textSecondary
        )
    }
}
