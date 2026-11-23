package com.oneui.calendar.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.oneui.calendar.core.common.AppLanguage
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.common.LunarCalendarConverter
import com.oneui.calendar.core.common.NlpTimeAndConflictDetector
import com.oneui.calendar.core.designsystem.components.OneUiDDayBadge
import com.oneui.calendar.core.designsystem.components.OneUiDottedDivider
import com.oneui.calendar.core.designsystem.components.OneUiFocusBlock
import com.oneui.calendar.core.designsystem.components.OneUiStickerPickerSheet
import com.oneui.calendar.core.designsystem.theme.OneUiShapes
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.core.designsystem.theme.accentBarColor
import com.oneui.calendar.core.designsystem.theme.containerColor
import com.oneui.calendar.domain.model.CalendarAccount
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.CalendarSticker
import com.oneui.calendar.domain.model.EventColorTag
import com.oneui.calendar.domain.model.EventInstance
import com.oneui.calendar.domain.model.NlpConflictPreview
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EDITOR_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
private val EDITOR_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val CONFLICT_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mma", Locale.US)

private val EDITOR_COLOR_OPTIONS = listOf(
    EventColorTag.PASTEL_PINK,
    EventColorTag.PASTEL_CORAL,
    EventColorTag.PASTEL_MINT,
    EventColorTag.PASTEL_BLUE,
    EventColorTag.PASTEL_LAVENDER,
    EventColorTag.PASTEL_YELLOW
)

private val REMINDER_OPTIONS = listOf(
    -1 to "None",
    0 to "At time of event",
    10 to "10 mins before",
    60 to "1 hour before",
    1440 to "1 day before"
)

private val REPEAT_OPTIONS = listOf(
    null to "Does not repeat",
    "FREQ=DAILY" to "Daily",
    "FREQ=WEEKLY" to "Weekly",
    "FREQ=MONTHLY" to "Monthly",
    "FREQ=YEARLY" to "Yearly"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    initialEvent: CalendarEvent?,
    initialDate: LocalDate,
    sameDayInstances: List<EventInstance>,
    accounts: List<CalendarAccount>,
    onSaveEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: ((Long) -> Unit)? = null,
    onCancel: () -> Unit
) {
    val colors = OneUiTheme.colors
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val zoneId = remember(initialEvent?.timeZone) {
        runCatching {
            initialEvent?.timeZone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        }.getOrDefault(ZoneId.systemDefault())
    }

    val initialStartZdt = remember(initialEvent, initialDate) {
        if (initialEvent != null) {
            Instant.ofEpochMilli(initialEvent.startTimeMillis).atZone(zoneId)
        } else {
            initialDate.atTime(9, 0).atZone(zoneId)
        }
    }
    val initialEndZdt = remember(initialEvent, initialDate) {
        if (initialEvent != null) {
            Instant.ofEpochMilli(initialEvent.endTimeMillis).atZone(zoneId)
        } else {
            initialDate.atTime(10, 0).atZone(zoneId)
        }
    }

    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var location by remember(initialEvent?.id) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var notes by remember(initialEvent?.id) { mutableStateOf(initialEvent?.description.orEmpty()) }
    var isAllDay by remember(initialEvent?.id) { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var isDDay by remember(initialEvent?.id) { mutableStateOf(initialEvent?.isDDay ?: false) }
    var isLunar by remember(initialEvent?.id) { mutableStateOf(initialEvent?.isLunar ?: false) }
    var startDate by remember(initialEvent?.id, initialDate) { mutableStateOf(initialStartZdt.toLocalDate()) }
    var startTime by remember(initialEvent?.id, initialDate) { mutableStateOf(initialStartZdt.toLocalTime()) }
    var endDate by remember(initialEvent?.id, initialDate) { mutableStateOf(initialEndZdt.toLocalDate()) }
    var endTime by remember(initialEvent?.id, initialDate) { mutableStateOf(initialEndZdt.toLocalTime()) }
    var selectedColorTag by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.colorTag ?: EventColorTag.PASTEL_PINK)
    }
    var selectedStickerId by remember(initialEvent?.id) { mutableStateOf(initialEvent?.stickerId) }
    var hasAlarm by remember(initialEvent?.id) { mutableStateOf(initialEvent?.hasAlarm ?: true) }
    var reminderMinutesBefore by remember(initialEvent?.id) {
        mutableIntStateOf(initialEvent?.reminderMinutesBefore ?: 10)
    }
    var selectedRRule by remember(initialEvent?.id) { mutableStateOf(initialEvent?.rrule) }
    var selectedCalendarId by remember(initialEvent?.id, accounts) {
        mutableLongStateOf(
            initialEvent?.calendarId
                ?: accounts.firstOrNull { it.isVisible }?.id
                ?: accounts.firstOrNull()?.id
                ?: 1L
        )
    }

    var showStickerSheet by remember { mutableStateOf(false) }
    var showColorRow by remember { mutableStateOf(false) }
    var showTitleError by remember { mutableStateOf(false) }
    var dismissConflictCardForTitle by remember { mutableStateOf<String?>(null) }
    var appliedNlpPhrase by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var pendingConflictEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var conflictingEventsForDialog by remember { mutableStateOf<List<EventInstance>>(emptyList()) }

    var editingStartDatePicker by remember { mutableStateOf(false) }
    var editingEndDatePicker by remember { mutableStateOf(false) }
    var editingStartTimePicker by remember { mutableStateOf(false) }
    var editingEndTimePicker by remember { mutableStateOf(false) }

    val nlpPreview = remember(title, startDate, sameDayInstances) {
        NlpTimeAndConflictDetector.analyzeTitleInput(
            rawTitle = title,
            targetDate = startDate,
            sameDayInstances = sameDayInstances,
            zoneId = zoneId
        )
    }
    val showFloatingConflictCard = nlpPreview != null && dismissConflictCardForTitle != title

    fun applySuggestedTime(preview: NlpConflictPreview) {
        val suggestedStart = Instant.ofEpochMilli(preview.suggestedStartMillis).atZone(zoneId)
        val suggestedEnd = Instant.ofEpochMilli(preview.suggestedEndMillis).atZone(zoneId)
        isAllDay = false
        startDate = suggestedStart.toLocalDate()
        startTime = suggestedStart.toLocalTime()
        endDate = suggestedEnd.toLocalDate()
        endTime = suggestedEnd.toLocalTime()
        appliedNlpPhrase = preview.detectedTimeRangeText
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun buildAndSaveEvent() {
        val cleanedTitle = title.trim()
        if (cleanedTitle.isEmpty()) {
            showTitleError = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }

        val startMillis = if (isAllDay) {
            startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        } else {
            startDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
        }
        val rawEndMillis = if (isAllDay) {
            val safeEndDate = if (endDate.isBefore(startDate)) startDate else endDate
            safeEndDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        } else {
            val safeEndDate = if (endDate.isBefore(startDate)) startDate else endDate
            safeEndDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
        }
        val finalEndMillis = if (rawEndMillis <= startMillis) {
            if (isAllDay) startMillis + 86_400_000L else startMillis + 3_600_000L
        } else {
            rawEndMillis
        }

        val candidateEvent = CalendarEvent(
            id = initialEvent?.id ?: 0L,
            calendarId = selectedCalendarId,
            title = cleanedTitle,
            location = location.trim(),
            description = notes.trim(),
            startTimeMillis = startMillis,
            endTimeMillis = finalEndMillis,
            timeZone = zoneId.id,
            isAllDay = isAllDay,
            isDDay = isDDay,
            isLunar = isLunar,
            rrule = selectedRRule,
            colorTag = selectedColorTag,
            stickerId = selectedStickerId,
            hasAlarm = hasAlarm,
            reminderMinutesBefore = if (hasAlarm) reminderMinutesBefore else 10,
            externalEventId = initialEvent?.externalEventId
        )

        // Check for overlapping existing events (exclude self when editing)
        val overlapping = sameDayInstances.filter { existing ->
            existing.event.id != candidateEvent.id &&
                existing.instanceStartMillis < candidateEvent.endTimeMillis &&
                existing.instanceEndMillis > candidateEvent.startTimeMillis
        }

        if (overlapping.isNotEmpty()) {
            conflictingEventsForDialog = overlapping
            pendingConflictEvent = candidateEvent
        } else {
            onSaveEvent(candidateEvent)
        }
    }

    Scaffold(
        containerColor = colors.canvasBackground,
        bottomBar = {
            EditorBottomActionBar(
                onCancel = onCancel,
                onSave = { buildAndSaveEvent() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Title Card matching Screenshot 5
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.focusBlockBackground,
                shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (title.isEmpty()) {
                                Text(
                                    text = strings.titlePlaceholder,
                                    fontSize = 22.sp,
                                    color = colors.textTertiary
                                )
                            }
                            BasicTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    showTitleError = false
                                    if (dismissConflictCardForTitle != it) {
                                        dismissConflictCardForTitle = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(colors.accentBlue),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Sticker picker trigger
                        val activeSticker = CalendarSticker.fromId(selectedStickerId)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = { showStickerSheet = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeSticker != null) {
                                Text(
                                    text = activeSticker.emoji,
                                    fontSize = 22.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.SentimentSatisfied,
                                    contentDescription = "Select event sticker",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Circular Pastel Color Dot (26.dp)
                        val dotColor = selectedColorTag.containerColor()
                        val dotBorderColor = selectedColorTag.accentBarColor()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showColorRow = !showColorRow
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(1.dp, dotBorderColor.copy(alpha = 0.45f), CircleShape)
                            )
                        }
                    }

                    if (showTitleError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please enter an event title",
                            color = colors.sundayRed,
                            fontSize = 13.sp
                        )
                    }

                    // Quick-test suggestion chip for Screenshot 5 ("3pm Movie")
                    AnimatedVisibility(
                        visible = initialEvent == null && title.isBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.accentBlue.copy(alpha = 0.10f))
                                    .clickable(role = Role.Button) {
                                        title = "3pm Movie"
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.accentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Try: 3pm Movie",
                                    color = colors.accentBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Expandable 6 Pastel Color Picker Row
                    AnimatedVisibility(
                        visible = showColorRow,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EDITOR_COLOR_OPTIONS.forEach { tag ->
                                val isSelected = tag == selectedColorTag
                                val swatchColor = tag.containerColor()
                                val ringColor = tag.accentBarColor()
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable(role = Role.Button) {
                                            selectedColorTag = tag
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) ringColor else ringColor.copy(alpha = 0.45f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = tag.displayName,
                                                tint = ringColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // All-day & Time Focus Block WITH Overlapping Floating NLP Conflict Card (Screenshot 5)
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OneUiFocusBlock(
                    modifier = Modifier.padding(horizontal = 0.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // All-day Switch Row (right edge intentionally aligns so toggle peeks out on right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.allDay,
                                fontSize = 16.5.sp,
                                color = colors.textPrimary
                            )
                        }
                        OneUiSwitch(
                            checked = isAllDay,
                            onCheckedChange = {
                                isAllDay = it
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Start Date & Time Row
                    val localizedDateFormatter = remember(strings.language) {
                        when (strings.language) {
                            AppLanguage.ENGLISH -> DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
                            AppLanguage.VIETNAMESE -> DateTimeFormatter.ofPattern("EEE, d 'Thg' M, yyyy", Locale("vi", "VN"))
                            AppLanguage.CHINESE -> DateTimeFormatter.ofPattern("yyyy年M月d日 EEE", Locale.SIMPLIFIED_CHINESE)
                        }
                    }
                    val startLabel = when (strings.language) {
                        AppLanguage.ENGLISH -> "Start"
                        AppLanguage.VIETNAMESE -> "Bắt đầu"
                        AppLanguage.CHINESE -> "开始"
                    }
                    val endLabel = when (strings.language) {
                        AppLanguage.ENGLISH -> "End"
                        AppLanguage.VIETNAMESE -> "Kết thúc"
                        AppLanguage.CHINESE -> "结束"
                    }
                    DateTimeFormRow(
                        label = startLabel,
                        dateText = startDate.format(localizedDateFormatter),
                        timeText = if (isAllDay) null else startTime.format(EDITOR_TIME_FORMATTER),
                        onDateClick = { editingStartDatePicker = true },
                        onTimeClick = { editingStartTimePicker = true }
                    )

                    OneUiDottedDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // End Date & Time Row
                    DateTimeFormRow(
                        label = endLabel,
                        dateText = endDate.format(localizedDateFormatter),
                        timeText = if (isAllDay) null else endTime.format(EDITOR_TIME_FORMATTER),
                        onDateClick = { editingEndDatePicker = true },
                        onTimeClick = { editingEndTimePicker = true }
                    )

                    if (appliedNlpPhrase != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Smart time applied: $appliedNlpPhrase",
                            color = colors.accentBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OneUiDottedDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // D-Day Countdown Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OneUiDDayBadge()
                            Column {
                                Text(
                                    text = strings.dDayLabel,
                                    fontSize = 16.sp,
                                    color = colors.textPrimary
                                )
                                val dDaySub = when (strings.language) {
                                    AppLanguage.ENGLISH -> "Show countdown badge on calendar"
                                    AppLanguage.VIETNAMESE -> "Hiện huy hiệu đếm ngược trên lịch"
                                    AppLanguage.CHINESE -> "在日历上显示倒数日徽章"
                                }
                                Text(
                                    text = dDaySub,
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        OneUiSwitch(
                            checked = isDDay,
                            onCheckedChange = {
                                isDDay = it
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }

                    OneUiDottedDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Lunar Calendar Toggle Row
                    val lunarPreviewText = remember(startDate, strings.language) {
                        strings.formatLunarSubDate(startDate)
                    }
                    val lunarPrefix = when (strings.language) {
                        AppLanguage.ENGLISH -> "Lunar date: "
                        AppLanguage.VIETNAMESE -> "Ngày âm lịch: "
                        AppLanguage.CHINESE -> "农历日期: "
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.lunarLabel,
                                fontSize = 16.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "$lunarPrefix$lunarPreviewText",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OneUiSwitch(
                            checked = isLunar,
                            onCheckedChange = {
                                isLunar = it
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }

                // Floating NLP Conflict Card overlapping above All-day switch section (Screenshot 5)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showFloatingConflictCard,
                    modifier = Modifier
                        .zIndex(4f)
                        .padding(start = 12.dp, end = 48.dp, top = 2.dp),
                    enter = scaleIn(
                        initialScale = 0.94f,
                        animationSpec = spring(stiffness = 400f, dampingRatio = 0.86f)
                    ) + fadeIn(tween(180)),
                    exit = scaleOut(targetScale = 0.94f, animationSpec = tween(150)) + fadeOut(tween(150))
                ) {
                    nlpPreview?.let { preview ->
                        FloatingNlpConflictCard(
                            preview = preview,
                            rawQuery = title,
                            onApplyTime = {
                                applySuggestedTime(preview)
                                dismissConflictCardForTitle = title
                            },
                            onDismissCard = {
                                dismissConflictCardForTitle = title
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location Focus Block (matches Location pin row right below card in Screenshot 5)
            OneUiFocusBlock(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (location.isEmpty()) {
                            Text(
                                text = strings.locationPlaceholder,
                                fontSize = 16.5.sp,
                                color = colors.textTertiary
                            )
                        }
                        BasicTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 16.5.sp
                            ),
                            cursorBrush = SolidColor(colors.accentBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminder & Repeat Focus Block
            OneUiFocusBlock(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.reminderLabel,
                        fontSize = 16.5.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val localizedReminderOptions = remember(strings.language) {
                        when (strings.language) {
                            AppLanguage.ENGLISH -> REMINDER_OPTIONS
                            AppLanguage.VIETNAMESE -> listOf(
                                -1 to "Không",
                                0 to "Đúng giờ",
                                10 to "Trước 10 phút",
                                60 to "Trước 1 giờ",
                                1440 to "Trước 1 ngày"
                            )
                            AppLanguage.CHINESE -> listOf(
                                -1 to "无",
                                0 to "准时",
                                10 to "提前10分钟",
                                60 to "提前1小时",
                                1440 to "提前1天"
                            )
                        }
                    }
                    localizedReminderOptions.forEach { (minutes, label) ->
                        val isSelected = if (minutes < 0) !hasAlarm else (hasAlarm && reminderMinutesBefore == minutes)
                        EditorPillChip(
                            label = label,
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (minutes < 0) {
                                    hasAlarm = false
                                } else {
                                    hasAlarm = true
                                    reminderMinutesBefore = minutes
                                }
                            }
                        )
                    }
                }

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.repeatLabel,
                        fontSize = 16.5.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val localizedRepeatOptions = remember(strings.language) {
                        when (strings.language) {
                            AppLanguage.ENGLISH -> REPEAT_OPTIONS
                            AppLanguage.VIETNAMESE -> listOf(
                                null to "Không lặp lại",
                                "FREQ=DAILY" to "Hàng ngày",
                                "FREQ=WEEKLY" to "Hàng tuần",
                                "FREQ=MONTHLY" to "Hàng tháng",
                                "FREQ=YEARLY" to "Hàng năm"
                            )
                            AppLanguage.CHINESE -> listOf(
                                null to "不重复",
                                "FREQ=DAILY" to "每天",
                                "FREQ=WEEKLY" to "每周",
                                "FREQ=MONTHLY" to "每月",
                                "FREQ=YEARLY" to "每年"
                            )
                        }
                    }
                    localizedRepeatOptions.forEach { (rruleValue, label) ->
                        EditorPillChip(
                            label = label,
                            selected = selectedRRule == rruleValue,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedRRule = rruleValue
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar Account & Notes Focus Block
            OneUiFocusBlock(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.myCalendars,
                        fontSize = 16.5.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val accountList = accounts
                    .filter { it.groupType != com.oneui.calendar.domain.model.AccountGroupType.SAMSUNG }
                    .ifEmpty {
                        listOf(
                            CalendarAccount(
                                id = 1L,
                                accountName = strings.myPhone,
                                displayName = strings.myCalendars,
                                groupType = com.oneui.calendar.domain.model.AccountGroupType.MY_PHONE,
                                colorHex = 0xFFB39DFF,
                                isVisible = true
                            )
                        )
                    }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accountList.forEach { account ->
                        val isSelected = account.id == selectedCalendarId
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) colors.accentBlue.copy(alpha = 0.14f)
                                    else colors.controlSubtleBackground
                                )
                                .then(
                                    if (isSelected) Modifier.border(1.dp, colors.accentBlue, CircleShape)
                                    else Modifier
                                )
                                .clickable(role = Role.Button) {
                                    selectedCalendarId = account.id
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(account.colorHex))
                            )
                            Text(
                                text = account.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) colors.accentBlue else colors.textPrimary
                            )
                        }
                    }
                }

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (notes.isEmpty()) {
                            Text(
                                text = strings.notesPlaceholder,
                                fontSize = 16.sp,
                                color = colors.textTertiary
                            )
                        }
                        BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(colors.accentBlue),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )
                    }
                }
            }

            if (initialEvent != null && onDeleteEvent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.sundayRed.copy(alpha = 0.10f))
                            .clickable(role = Role.Button) {
                                showDeleteConfirmation = true
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = colors.sundayRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.deleteEvent,
                            color = colors.sundayRed,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val conflictCandidate = pendingConflictEvent
    if (conflictCandidate != null) {
        AlertDialog(
            onDismissRequest = { pendingConflictEvent = null },
            title = {
                Text(
                    text = "Trùng khung giờ / Schedule Conflict",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Khung giờ này đã có ${conflictingEventsForDialog.size} lịch khác:",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    conflictingEventsForDialog.take(3).forEach { inst ->
                        Text(
                            text = "• ${inst.event.title} (${inst.formattedTimeRange()})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bạn vẫn muốn lưu lịch này chứ?",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toSave = conflictCandidate
                        pendingConflictEvent = null
                        onSaveEvent(toSave)
                    }
                ) {
                    Text(
                        text = "Vẫn lưu / Save anyway",
                        color = colors.accentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConflictEvent = null }) {
                    Text(text = "Hủy / Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.elevatedCardBackground,
            shape = OneUiShapes.PopupCardShape
        )
    }

    if (showStickerSheet) {
        OneUiStickerPickerSheet(
            selectedStickerId = selectedStickerId,
            onSelectSticker = { sticker -> selectedStickerId = sticker?.id },
            onDismiss = { showStickerSheet = false }
        )
    }

    if (editingStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { editingStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            startDate = picked
                            if (endDate.isBefore(picked)) {
                                endDate = picked
                            }
                        }
                        editingStartDatePicker = false
                    }
                ) { Text("Done", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { editingStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (editingEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { editingEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            endDate = if (picked.isBefore(startDate)) startDate else picked
                        }
                        editingEndDatePicker = false
                    }
                ) { Text("Done", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { editingEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (editingStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { editingStartTimePicker = false },
            containerColor = colors.elevatedCardBackground,
            shape = OneUiShapes.PopupCardShape,
            title = { Text("Start time", fontWeight = FontWeight.SemiBold, color = colors.textPrimary) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        startTime = picked
                        if (endDate == startDate && !endTime.isAfter(picked)) {
                            endTime = picked.plusHours(1)
                        }
                        editingStartTimePicker = false
                    }
                ) { Text("Done", fontWeight = FontWeight.SemiBold, color = colors.accentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { editingStartTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (editingEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { editingEndTimePicker = false },
            containerColor = colors.elevatedCardBackground,
            shape = OneUiShapes.PopupCardShape,
            title = { Text("End time", fontWeight = FontWeight.SemiBold, color = colors.textPrimary) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        editingEndTimePicker = false
                    }
                ) { Text("Done", fontWeight = FontWeight.SemiBold, color = colors.accentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { editingEndTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (showDeleteConfirmation && initialEvent != null && onDeleteEvent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = colors.elevatedCardBackground,
            shape = OneUiShapes.PopupCardShape,
            title = {
                Text(
                    text = "Delete event?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "\"${initialEvent.title}\" will be permanently removed from your calendar.",
                    fontSize = 14.5.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteEvent(initialEvent.id)
                    }
                ) {
                    Text("Delete", color = colors.sundayRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = colors.textPrimary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
private fun FloatingNlpConflictCard(
    preview: NlpConflictPreview,
    rawQuery: String,
    onApplyTime: () -> Unit,
    onDismissCard: () -> Unit
) {
    val colors = OneUiTheme.colors
    val cardModifier = if (colors.isDark) {
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22FFFFFF), OneUiShapes.FloatingConflictCardShape)
    } else {
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = OneUiShapes.FloatingConflictCardShape,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
    }

    Surface(
        modifier = cardModifier,
        shape = OneUiShapes.FloatingConflictCardShape,
        color = colors.elevatedCardBackground,
        shadowElevation = if (colors.isDark) 0.dp else 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Header row: Clock icon + detectedTimeRangeText ("3:00PM ~ 4:00PM")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onApplyTime)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Apply detected time",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = preview.detectedTimeRangeText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = "Apply",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentBlue
                )
            }

            if (preview.conflictingEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    preview.conflictingEvents.take(4).forEach { conflict ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = onDismissCard),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(conflict.event.colorTag.accentBarColor())
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildHighlightedConflictTitle(
                                        eventTitle = conflict.event.title,
                                        rawQuery = rawQuery,
                                        matchedPhrase = preview.matchedPhrase,
                                        highlightColor = colors.accentBlue,
                                        normalColor = colors.textPrimary
                                    ),
                                    fontSize = 16.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatConflictSubtitle(conflict),
                                    fontSize = 13.5.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatConflictSubtitle(instance: EventInstance): String {
    if (instance.event.isAllDay) return "All day"
    val zone = runCatching { ZoneId.of(instance.event.timeZone) }.getOrDefault(ZoneId.systemDefault())
    val start = Instant.ofEpochMilli(instance.instanceStartMillis).atZone(zone).toLocalTime()
    val end = Instant.ofEpochMilli(instance.instanceEndMillis).atZone(zone).toLocalTime()
    return "${CONFLICT_TIME_FORMATTER.format(start)} ~ ${CONFLICT_TIME_FORMATTER.format(end)}"
}

private fun buildHighlightedConflictTitle(
    eventTitle: String,
    rawQuery: String,
    matchedPhrase: String,
    highlightColor: Color,
    normalColor: Color
): AnnotatedString {
    val cleanedQuery = rawQuery.trim()
    val targetPhrase = when {
        cleanedQuery.length >= 2 && eventTitle.contains(cleanedQuery, ignoreCase = true) -> cleanedQuery
        matchedPhrase.isNotBlank() && eventTitle.contains(matchedPhrase, ignoreCase = true) -> matchedPhrase
        else -> null
    }

    if (targetPhrase == null) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) { append(eventTitle) }
        }
    }

    val startIndex = eventTitle.indexOf(targetPhrase, ignoreCase = true)
    if (startIndex < 0) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) { append(eventTitle) }
        }
    }

    val endIndex = startIndex + targetPhrase.length
    return buildAnnotatedString {
        if (startIndex > 0) {
            withStyle(SpanStyle(color = normalColor)) {
                append(eventTitle.substring(0, startIndex))
            }
        }
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
            append(eventTitle.substring(startIndex, endIndex))
        }
        if (endIndex < eventTitle.length) {
            withStyle(SpanStyle(color = normalColor)) {
                append(eventTitle.substring(endIndex))
            }
        }
    }
}

@Composable
private fun DateTimeFormRow(
    label: String,
    dateText: String,
    timeText: String?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(52.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateText,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onDateClick)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
            if (timeText != null) {
                Text(
                    text = timeText,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accentBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onTimeClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    val bgColor = if (selected) colors.accentBlue.copy(alpha = 0.14f) else colors.controlSubtleBackground
    val textColor = if (selected) colors.accentBlue else colors.textPrimary

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (selected) Modifier.border(1.dp, colors.accentBlue, CircleShape) else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun OneUiSwitch(
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
        animationSpec = tween(durationMillis = 180),
        label = "oneUiSwitchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f),
        label = "oneUiSwitchThumb"
    )

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            ),
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

@Composable
private fun EditorBottomActionBar(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val colors = OneUiTheme.colors
    val barBg = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF4F4F6)
    val cancelInteraction = remember { MutableInteractionSource() }
    val saveInteraction = remember { MutableInteractionSource() }
    val cancelPressed by cancelInteraction.collectIsPressedAsState()
    val savePressed by saveInteraction.collectIsPressedAsState()
    val cancelScale by animateFloatAsState(if (cancelPressed) 0.96f else 1f, label = "cancelScale")
    val saveScale by animateFloatAsState(if (savePressed) 0.96f else 1f, label = "saveScale")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(barBg)
            .imePadding()
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 0.75.dp,
            color = colors.dividerHairline
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = cancelScale
                        scaleY = cancelScale
                    }
                    .clickable(
                        interactionSource = cancelInteraction,
                        indication = androidx.compose.material3.ripple(),
                        role = Role.Button,
                        onClick = onCancel
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocalAppStrings.current.cancel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(colors.dividerHairline)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = saveScale
                        scaleY = saveScale
                    }
                    .clickable(
                        interactionSource = saveInteraction,
                        indication = androidx.compose.material3.ripple(),
                        role = Role.Button,
                        onClick = onSave
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocalAppStrings.current.save,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        }
    }
}
