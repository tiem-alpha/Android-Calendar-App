package com.oneui.calendar.feature.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.oneui.calendar.domain.model.CalendarSticker
import com.oneui.calendar.domain.model.EventColorTag
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SEARCH_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val SEARCH_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private val SEARCH_COLOR_DOT_ORDER = listOf(
    EventColorTag.PASTEL_BLUE,
    EventColorTag.PASTEL_LAVENDER,
    EventColorTag.PASTEL_PINK,
    EventColorTag.PASTEL_YELLOW,
    EventColorTag.PASTEL_CORAL,
    EventColorTag.PASTEL_MINT
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    allEvents: List<CalendarEvent>,
    recentSearches: List<String>,
    onSearchSubmit: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearAllRecentSearches: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onDeleteEvents: (Set<Long>) -> Unit = {},
    onDeactivateEvents: (Set<Long>) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var query by remember { mutableStateOf("") }
    var selectedSticker by remember { mutableStateOf<CalendarSticker?>(null) }
    var selectedColor by remember { mutableStateOf<EventColorTag?>(null) }
    var expandStickers by remember { mutableStateOf(false) }
    var expandColors by remember { mutableStateOf(false) }

    // One UI Multi-Select state for search results
    var selectedEventIds by remember { mutableStateOf(emptySet<Long>()) }
    val isMultiSelectMode = selectedEventIds.isNotEmpty()

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spokenText.isNullOrEmpty()) {
                query = spokenText
                onSearchSubmit(spokenText)
            }
        }
    }

    val isDefaultOverview = query.isBlank() && selectedSticker == null && selectedColor == null

    val filteredEvents = remember(allEvents, query, selectedSticker, selectedColor) {
        if (isDefaultOverview) {
            emptyList()
        } else {
            val trimmed = query.trim()
            allEvents.filter { event ->
                val matchesQuery = trimmed.isEmpty() ||
                    event.title.contains(trimmed, ignoreCase = true) ||
                    event.location.contains(trimmed, ignoreCase = true) ||
                    event.description.contains(trimmed, ignoreCase = true)
                val matchesSticker = selectedSticker == null ||
                    event.stickerId.equals(selectedSticker?.id, ignoreCase = true)
                val matchesColor = selectedColor == null || event.colorTag == selectedColor
                matchesQuery && matchesSticker && matchesColor
            }.sortedBy { it.startTimeMillis }
        }
    }

    val showGoToTop by remember {
        derivedStateOf {
            !isDefaultOverview && (listState.firstVisibleItemIndex > 1 || listState.firstVisibleItemScrollOffset > 280)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Search Bar (56.dp) matching Screenshot 6
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            fontSize = 22.sp,
                            color = colors.textTertiary
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(colors.accentBlue),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotBlank()) {
                                    onSearchSubmit(query.trim())
                                }
                            }
                        )
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search calendar events")
                        }
                        try {
                            voiceLauncher.launch(speechIntent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "Voice search unavailable; trying sample query",
                                Toast.LENGTH_SHORT
                            ).show()
                            query = "Design Review"
                            onSearchSubmit("Design Review")
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Voice search",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isDefaultOverview) {
                // Default View: 3 Focus Blocks matching Screenshot 6
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Focus Block 1: Recent Searches
                    item {
                        OneUiFocusBlock(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "Clear all",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (recentSearches.isNotEmpty()) colors.accentBlue else colors.textTertiary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(enabled = recentSearches.isNotEmpty()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onClearAllRecentSearches()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (recentSearches.isEmpty()) {
                                Text(
                                    text = "No recent searches",
                                    fontSize = 14.sp,
                                    color = colors.textTertiary
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    recentSearches.forEach { term ->
                                        Row(
                                            modifier = Modifier
                                                .height(36.dp)
                                                .clip(CircleShape)
                                                .background(colors.controlSubtleBackground)
                                                .clickable(role = Role.Button) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    query = term
                                                    onSearchSubmit(term)
                                                }
                                                .padding(start = 16.dp, end = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = term,
                                                fontSize = 14.5.sp,
                                                color = colors.textPrimary,
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove $term",
                                                tint = colors.textSecondary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .clickable(role = Role.Button) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onRemoveRecentSearch(term)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Focus Block 2: Sticker
                    item {
                        OneUiFocusBlock(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Text(
                                text = "Sticker",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val primaryStickers = remember { CalendarSticker.entries.take(5) }
                            val extraStickers = remember { CalendarSticker.entries.drop(5) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                primaryStickers.forEach { sticker ->
                                    StickerFilterCircle(
                                        sticker = sticker,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedSticker = sticker
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.controlSubtleBackground)
                                        .clickable(role = Role.Button) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            expandStickers = !expandStickers
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreHoriz,
                                        contentDescription = "More stickers",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = expandStickers && extraStickers.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    extraStickers.forEach { sticker ->
                                        StickerFilterCircle(
                                            sticker = sticker,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedSticker = sticker
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }

                    // Focus Block 3: Color
                    item {
                        OneUiFocusBlock(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Text(
                                text = "Color",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val primaryColors = remember { SEARCH_COLOR_DOT_ORDER.take(5) }
                            val extraColors = remember { SEARCH_COLOR_DOT_ORDER.drop(5) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                primaryColors.forEach { tag ->
                                    ColorDotFilterCircle(
                                        colorTag = tag,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedColor = tag
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.controlSubtleBackground)
                                        .clickable(role = Role.Button) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            expandColors = !expandColors
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreHoriz,
                                        contentDescription = "More colors",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = expandColors && extraColors.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    extraColors.forEach { tag ->
                                        ColorDotFilterCircle(
                                            colorTag = tag,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedColor = tag
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Active Filter / Search Results View
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (selectedSticker != null || selectedColor != null) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedSticker?.let { sticker ->
                                ActiveFilterPill(
                                    label = "Sticker: ${sticker.emoji} ${sticker.label}",
                                    onClear = { selectedSticker = null }
                                )
                            }
                            selectedColor?.let { colorTag ->
                                ActiveFilterPill(
                                    label = "Color: ${colorTag.displayName}",
                                    leadingDotColor = colorTag.accentBarColor(),
                                    onClear = { selectedColor = null }
                                )
                            }
                        }
                    }

                    if (filteredEvents.isEmpty()) {
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
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "No matching events",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Try a different keyword, sticker, or pastel color tag.",
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
                                top = 8.dp,
                                bottom = 72.dp
                            )
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 4.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isMultiSelectMode) {
                                            when (strings.language) {
                                                AppLanguage.ENGLISH -> "${selectedEventIds.size} selected"
                                                AppLanguage.VIETNAMESE -> "Đã chọn ${selectedEventIds.size}"
                                                AppLanguage.CHINESE -> "已选 ${selectedEventIds.size} 项"
                                            }
                                        } else {
                                            "${filteredEvents.size} result${if (filteredEvents.size == 1) "" else "s"}"
                                        },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isMultiSelectMode) colors.accentBlue else colors.textSecondary
                                    )

                                    if (isMultiSelectMode) {
                                        val allSelected = filteredEvents.isNotEmpty() &&
                                            filteredEvents.all { it.id in selectedEventIds }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (strings.language) {
                                                    AppLanguage.ENGLISH -> if (allSelected) "Deselect all" else "Select all"
                                                    AppLanguage.VIETNAMESE -> if (allSelected) "Bỏ chọn hết" else "Chọn tất cả"
                                                    AppLanguage.CHINESE -> if (allSelected) "取消全选" else "全选"
                                                },
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.accentBlue,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable(role = Role.Button) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedEventIds = if (allSelected) {
                                                            emptySet()
                                                        } else {
                                                            filteredEvents.map { it.id }.toSet()
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                            Text(
                                                text = strings.cancel,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textSecondary,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable(role = Role.Button) {
                                                        selectedEventIds = emptySet()
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                OneUiFocusBlock(
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    filteredEvents.forEachIndexed { index, event ->
                                        val isItemSelected = event.id in selectedEventIds
                                        SearchResultRow(
                                            event = event,
                                            highlightQuery = query.trim(),
                                            isMultiSelectMode = isMultiSelectMode,
                                            isSelected = isItemSelected,
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedEventIds = if (isItemSelected) {
                                                        selectedEventIds - event.id
                                                    } else {
                                                        selectedEventIds + event.id
                                                    }
                                                } else {
                                                    if (query.isNotBlank()) {
                                                        onSearchSubmit(query.trim())
                                                    }
                                                    onEventClick(event)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedEventIds = if (isItemSelected) {
                                                    selectedEventIds - event.id
                                                } else {
                                                    selectedEventIds + event.id
                                                }
                                            }
                                        )
                                        if (index < filteredEvents.lastIndex) {
                                            OneUiDottedDivider(
                                                modifier = Modifier.padding(horizontal = 20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // One UI Bottom Multi-Select Action Bar (Deactivate Alarm / Delete)
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.elevatedCardBackground,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deactivate Alarms button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val targets = selectedEventIds
                                selectedEventIds = emptySet()
                                onDeactivateEvents(targets)
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (strings.language) {
                                AppLanguage.ENGLISH -> "Deactivate"
                                AppLanguage.VIETNAMESE -> "Tắt nhắc"
                                AppLanguage.CHINESE -> "停用提醒"
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }

                    // Delete Selected Events button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val targets = selectedEventIds
                                selectedEventIds = emptySet()
                                onDeleteEvents(targets)
                            }
                            .padding(vertical = 10.dp),
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
                            text = when (strings.language) {
                                AppLanguage.ENGLISH -> "Delete (${selectedEventIds.size})"
                                AppLanguage.VIETNAMESE -> "Xóa (${selectedEventIds.size})"
                                AppLanguage.CHINESE -> "删除 (${selectedEventIds.size})"
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.sundayRed
                        )
                    }
                }
            }
        }

        OneUiGoToTopButton(
            visible = showGoToTop && !isMultiSelectMode,
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun StickerFilterCircle(
    sticker: CalendarSticker,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sticker.emoji,
            fontSize = 26.sp
        )
    }
}

@Composable
private fun ColorDotFilterCircle(
    colorTag: EventColorTag,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    val dotColor = colorTag.accentBarColor()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(
                    width = 1.dp,
                    color = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun ActiveFilterPill(
    label: String,
    leadingDotColor: Color? = null,
    onClear: () -> Unit
) {
    val colors = OneUiTheme.colors
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(colors.accentBlue.copy(alpha = 0.12f))
            .border(1.dp, colors.accentBlue.copy(alpha = 0.45f), CircleShape)
            .clickable(role = Role.Button, onClick = onClear)
            .padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(leadingDotColor)
            )
        }
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.accentBlue
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Clear filter",
            tint = colors.accentBlue,
            modifier = Modifier.size(15.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(
    event: CalendarEvent,
    highlightQuery: String,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val colors = OneUiTheme.colors
    val zone = remember(event.timeZone) {
        runCatching { ZoneId.of(event.timeZone) }.getOrDefault(ZoneId.systemDefault())
    }
    val startZdt = remember(event.startTimeMillis, zone) {
        Instant.ofEpochMilli(event.startTimeMillis).atZone(zone)
    }
    val endZdt = remember(event.endTimeMillis, zone) {
        Instant.ofEpochMilli(event.endTimeMillis).atZone(zone)
    }
    val dateText = remember(startZdt) { startZdt.toLocalDate().format(SEARCH_DATE_FORMATTER) }
    val timeText = remember(event.isAllDay, startZdt, endZdt) {
        if (event.isAllDay) {
            "All day"
        } else {
            "${startZdt.toLocalTime().format(SEARCH_TIME_FORMATTER)} - ${endZdt.toLocalTime().format(SEARCH_TIME_FORMATTER)}"
        }
    }
    val sticker = CalendarSticker.fromId(event.stickerId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.accentBlue.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.accentBlue)
                        } else {
                            Modifier.border(1.6.dp, colors.textTertiary, CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(CircleShape)
                .background(event.colorTag.accentBarColor())
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (sticker != null) {
                    Text(
                        text = sticker.emoji,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = highlightMatchingText(
                        text = event.title,
                        query = highlightQuery,
                        highlightColor = colors.accentBlue,
                        normalColor = colors.textPrimary
                    ),
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = buildString {
                    append(dateText)
                    append(" • ")
                    append(timeText)
                    if (event.location.isNotBlank()) {
                        append(" • ")
                        append(event.location)
                    }
                },
                fontSize = 13.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (event.isDDay) {
            Spacer(modifier = Modifier.width(10.dp))
            OneUiDDayBadge()
        }
    }
}

private fun highlightMatchingText(
    text: String,
    query: String,
    highlightColor: Color,
    normalColor: Color
): AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) { append(text) }
        }
    }
    val idx = text.indexOf(query, ignoreCase = true)
    if (idx < 0) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) { append(text) }
        }
    }
    return buildAnnotatedString {
        if (idx > 0) {
            withStyle(SpanStyle(color = normalColor)) {
                append(text.substring(0, idx))
            }
        }
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
            append(text.substring(idx, idx + query.length))
        }
        if (idx + query.length < text.length) {
            withStyle(SpanStyle(color = normalColor)) {
                append(text.substring(idx + query.length))
            }
        }
    }
}
