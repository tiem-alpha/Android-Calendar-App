package com.oneui.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oneui.calendar.core.common.AppLanguage
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.common.appStringsFor
import com.oneui.calendar.core.designsystem.components.OneUiCalendarTopBar
import com.oneui.calendar.core.designsystem.components.OneUiStickerPickerSheet
import com.oneui.calendar.core.designsystem.theme.OneUiCalendarTheme
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.core.notifications.ExactAlarmScheduler
import com.oneui.calendar.data.repository.CalendarRepository
import com.oneui.calendar.domain.model.CalendarEvent
import com.oneui.calendar.domain.model.CalendarViewMode
import com.oneui.calendar.domain.model.ReminderItem
import com.oneui.calendar.feature.editor.EventEditorScreen
import com.oneui.calendar.feature.month.DayDetailPopupCarousel
import com.oneui.calendar.feature.month.MonthScreen
import com.oneui.calendar.feature.navigation.OneUiNavigationDrawer
import com.oneui.calendar.feature.reminder.ReminderListScreen
import com.oneui.calendar.feature.reminder.SettingsBottomSheetOrScreen
import com.oneui.calendar.feature.reminder.ThemePreference
import com.oneui.calendar.feature.search.SearchScreen
import com.oneui.calendar.feature.tools.DateToolsScreen
import com.oneui.calendar.feature.week.WeekDayScreen
import com.oneui.calendar.feature.year.YearScreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CalendarApplication
        setContent {
            OneUiCalendarApp(
                repository = app.repository,
                alarmScheduler = app.alarmScheduler
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OneUiCalendarApp(
    repository: CalendarRepository,
    alarmScheduler: ExactAlarmScheduler
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("oneui_calendar_prefs", android.content.Context.MODE_PRIVATE) }

    // Reactive data streams from CalendarRepository
    val accounts by repository.accounts.collectAsState()
    val allEvents by repository.allEvents.collectAsState()
    val reminders by repository.reminders.collectAsState()
    val recentSearches by repository.recentSearches.collectAsState()
    val dailyMetadataMap by repository.dailyMetadataMap.collectAsState()

    // App Preferences & States (Persisted in SharedPreferences)
    var appLanguage by remember {
        mutableStateOf(
            runCatching { AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.ENGLISH.name)!!) }
                .getOrDefault(AppLanguage.ENGLISH)
        )
    }
    var themePreference by remember {
        mutableStateOf(
            runCatching { ThemePreference.valueOf(prefs.getString("theme_pref", ThemePreference.SYSTEM.name)!!) }
                .getOrDefault(ThemePreference.SYSTEM)
        )
    }
    var startWeekOnMonday by remember { mutableStateOf(prefs.getBoolean("start_week_monday", false)) }
    var showLunarCalendar by remember { mutableStateOf(prefs.getBoolean("show_lunar", true)) }
    var showGlobalHolidays by remember { mutableStateOf(prefs.getBoolean("show_holidays", true)) }
    var selectedTimeZoneId by remember { mutableStateOf(prefs.getString("time_zone_id", "Asia/Ho_Chi_Minh") ?: "Asia/Ho_Chi_Minh") }
    var weatherCity by remember { mutableStateOf(prefs.getString("weather_city", "Hanoi") ?: "Hanoi") }
    var useFahrenheit by remember { mutableStateOf(prefs.getBoolean("use_fahrenheit", false)) }
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncStatus by remember { mutableStateOf<String?>("Synced with Local & Open-Meteo Cache") }

    // Default to real current device date and month
    val todayDate = remember { LocalDate.now() }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(todayDate)) }
    var selectedDate by remember { mutableStateOf(todayDate) }
    var currentViewMode by rememberSaveable { mutableStateOf(CalendarViewMode.MONTH) }
    var isSplitMonthMode by rememberSaveable { mutableStateOf(false) }

    // Modal / Overlay Navigation States
    var showDayPopupForDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStickerPickerForDate by remember { mutableStateOf<LocalDate?>(null) }
    var showMonthYearPickerSheet by remember { mutableStateOf(false) }
    var showSearchScreen by rememberSaveable { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showDateToolsScreen by rememberSaveable { mutableStateOf(false) }
    var activeEditorState by remember { mutableStateOf<EditorLaunchState?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Permission launcher for Android CalendarContract two-way sync & Notifications
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        scope.launch {
            isSyncing = true
            repository.refreshWeatherAndSystemSync()
            isSyncing = false
            lastSyncStatus = "Synced System Calendars & 16-day Open-Meteo Forecast"
        }
    }

    LaunchedEffect(Unit) {
        // Trigger background Open-Meteo weather fetch on startup
        repository.refreshWeatherAndSystemSync()
    }

    val isDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    // Compute visible EventInstances for current Month, Week, and Year
    val monthInstances = remember(currentYearMonth, allEvents, accounts) {
        val start = currentYearMonth.atDay(1).minusDays(7)
        val end = currentYearMonth.atEndOfMonth().plusDays(14)
        repository.getInstancesForRange(start, end)
    }

    val selectedDayInstances = remember(selectedDate, monthInstances, allEvents, accounts) {
        repository.getInstancesForRange(selectedDate, selectedDate)
    }

    val weekInstances = remember(selectedDate, allEvents, accounts) {
        val leading = selectedDate.dayOfWeek.value % 7
        val sunday = selectedDate.minusDays(leading.toLong())
        val saturday = sunday.plusDays(6)
        repository.getInstancesForRange(sunday, saturday)
    }

    val yearInstances = remember(currentYearMonth.year, allEvents, accounts) {
        val start = LocalDate.of(currentYearMonth.year, 1, 1)
        val end = LocalDate.of(currentYearMonth.year, 12, 31)
        repository.getInstancesForRange(start, end)
    }

    // Handle system Back button priority across overlays
    BackHandler(
        enabled = activeEditorState != null ||
            showSearchScreen ||
            showSettingsScreen ||
            showDateToolsScreen ||
            showDayPopupForDate != null ||
            drawerState.isOpen ||
            isSplitMonthMode
    ) {
        when {
            activeEditorState != null -> activeEditorState = null
            showSearchScreen -> showSearchScreen = false
            showSettingsScreen -> showSettingsScreen = false
            showDateToolsScreen -> showDateToolsScreen = false
            showDayPopupForDate != null -> showDayPopupForDate = null
            drawerState.isOpen -> scope.launch { drawerState.close() }
            isSplitMonthMode -> isSplitMonthMode = false
        }
    }

    val appStrings = remember(appLanguage) { appStringsFor(appLanguage) }

    CompositionLocalProvider(LocalAppStrings provides appStrings) {
    OneUiCalendarTheme(darkTheme = isDarkTheme) {
        val colors = OneUiTheme.colors

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvasBackground)
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = showDayPopupForDate == null &&
                    activeEditorState == null &&
                    !showSearchScreen &&
                    !showSettingsScreen,
                drawerContent = {
                    OneUiNavigationDrawer(
                        currentViewMode = currentViewMode,
                        accounts = accounts,
                        onSelectViewMode = { mode ->
                            currentViewMode = mode
                            scope.launch { drawerState.close() }
                        },
                        onToggleAccountVisibility = { accountId ->
                            scope.launch { repository.toggleAccountVisibility(accountId) }
                        },
                        onOpenSettings = {
                            scope.launch { drawerState.close() }
                            showSettingsScreen = true
                        },
                        onOpenDateTools = {
                            scope.launch { drawerState.close() }
                            showDateToolsScreen = true
                        },
                        onCloseDrawer = {
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (currentViewMode == CalendarViewMode.REMINDER) {
                                colors.canvasBackground
                            } else {
                                colors.calendarGridBackground
                            }
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Show the signature One UI Calendar Top Bar on Month, Week, and Day screens
                    if (currentViewMode == CalendarViewMode.MONTH ||
                        currentViewMode == CalendarViewMode.WEEK ||
                        currentViewMode == CalendarViewMode.DAY
                    ) {
                        val displayMonth = if (currentViewMode == CalendarViewMode.MONTH) {
                            currentYearMonth.month
                        } else {
                            selectedDate.month
                        }
                        val monthTitle = appStrings.formatMonthHeader(displayMonth)

                        OneUiCalendarTopBar(
                            monthTitle = monthTitle,
                            todayDayNumber = todayDate.dayOfMonth,
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            },
                            onSearchClick = {
                                showSearchScreen = true
                            },
                            onTodayClick = {
                                selectedDate = todayDate
                                currentYearMonth = YearMonth.from(todayDate)
                            },
                            onMonthTitleClick = {
                                showMonthYearPickerSheet = true
                            }
                        )
                    }

                    // Main View Switcher (Month, Week, Day, Year, Reminder)
                    AnimatedContent(
                        targetState = currentViewMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(180))
                        },
                        label = "CalendarViewModeContent",
                        modifier = Modifier.weight(1f)
                    ) { mode ->
                        when (mode) {
                            CalendarViewMode.MONTH -> {
                                MonthScreen(
                                    currentYearMonth = currentYearMonth,
                                    selectedDate = selectedDate,
                                    todayDate = todayDate,
                                    isSplitMode = isSplitMonthMode,
                                    monthInstances = monthInstances,
                                    selectedDayInstances = selectedDayInstances,
                                    dailyMetadataMap = dailyMetadataMap,
                                    onSelectDate = { date ->
                                        selectedDate = date
                                        if (YearMonth.from(date) != currentYearMonth) {
                                            currentYearMonth = YearMonth.from(date)
                                        }
                                        // Automatically switch to Split Agenda when selecting today (Oct 24)
                                        // or tapping when split mode is active
                                        if (date == todayDate && !isSplitMonthMode) {
                                            isSplitMonthMode = true
                                        }
                                    },
                                    onToggleSplitMode = { split ->
                                        isSplitMonthMode = split
                                    },
                                    onOpenDayDetailPopup = { date ->
                                        selectedDate = date
                                        showDayPopupForDate = date
                                    },
                                    onPrevMonth = {
                                        currentYearMonth = currentYearMonth.minusMonths(1)
                                        selectedDate = currentYearMonth.atDay(1)
                                    },
                                    onNextMonth = {
                                        currentYearMonth = currentYearMonth.plusMonths(1)
                                        selectedDate = currentYearMonth.atDay(1)
                                    },
                                    onEventClick = { event ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = event,
                                            targetDate = selectedDate
                                        )
                                    },
                                    onToggleEventAlarm = { eventId, enabled ->
                                        scope.launch {
                                            repository.toggleEventAlarm(eventId, enabled)
                                            allEvents.find { it.id == eventId }?.let { ev ->
                                                alarmScheduler.scheduleEventAlarm(ev.copy(hasAlarm = enabled))
                                            }
                                        }
                                    },
                                    onOpenStickerPickerForDate = { date ->
                                        showStickerPickerForDate = date
                                    },
                                    onQuickAddClick = { date ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = null,
                                            targetDate = date
                                        )
                                    },
                                    onFabAddClick = { date ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = null,
                                            targetDate = date
                                        )
                                    },
                                    showLunarInCells = showLunarCalendar,
                                    showGlobalHolidays = showGlobalHolidays,
                                    startWeekOnMonday = startWeekOnMonday
                                )
                            }

                            CalendarViewMode.WEEK, CalendarViewMode.DAY -> {
                                WeekDayScreen(
                                    isDayView = mode == CalendarViewMode.DAY,
                                    selectedDate = selectedDate,
                                    todayDate = todayDate,
                                    weekInstances = weekInstances,
                                    dailyMetadataMap = dailyMetadataMap,
                                    onSelectDate = { date ->
                                        selectedDate = date
                                        currentYearMonth = YearMonth.from(date)
                                    },
                                    onPrevPeriod = {
                                        selectedDate = if (mode == CalendarViewMode.DAY) {
                                            selectedDate.minusDays(1)
                                        } else {
                                            selectedDate.minusWeeks(1)
                                        }
                                        currentYearMonth = YearMonth.from(selectedDate)
                                    },
                                    onNextPeriod = {
                                        selectedDate = if (mode == CalendarViewMode.DAY) {
                                            selectedDate.plusDays(1)
                                        } else {
                                            selectedDate.plusWeeks(1)
                                        }
                                        currentYearMonth = YearMonth.from(selectedDate)
                                    },
                                    onEventClick = { event ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = event,
                                            targetDate = selectedDate
                                        )
                                    },
                                    onOpenDayDetailPopup = { date ->
                                        selectedDate = date
                                        showDayPopupForDate = date
                                    },
                                    onOpenStickerPickerForDate = { date ->
                                        showStickerPickerForDate = date
                                    },
                                    onQuickAddClick = { date ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = null,
                                            targetDate = date
                                        )
                                    },
                                    onFabAddClick = { date ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = null,
                                            targetDate = date
                                        )
                                    }
                                )
                            }

                            CalendarViewMode.YEAR -> {
                                YearScreen(
                                    currentYear = currentYearMonth.year,
                                    todayDate = todayDate,
                                    selectedDate = selectedDate,
                                    yearInstances = yearInstances,
                                    onPrevYear = {
                                        currentYearMonth = currentYearMonth.minusYears(1)
                                    },
                                    onNextYear = {
                                        currentYearMonth = currentYearMonth.plusYears(1)
                                    },
                                    onMonthClick = { ym ->
                                        currentYearMonth = ym
                                        selectedDate = ym.atDay(1)
                                        currentViewMode = CalendarViewMode.MONTH
                                    }
                                )
                            }

                            CalendarViewMode.REMINDER -> {
                                val dDayEvents = remember(allEvents) {
                                    allEvents.filter { it.isDDay }
                                }
                                ReminderListScreen(
                                    reminders = reminders,
                                    dDayEvents = dDayEvents,
                                    onAddReminder = { title, triggerMillis, colorTag ->
                                        scope.launch {
                                            repository.upsertReminder(
                                                ReminderItem(
                                                    title = title,
                                                    triggerTimeMillis = triggerMillis,
                                                    colorTag = colorTag
                                                )
                                            )
                                        }
                                    },
                                    onToggleReminderCompleted = { id ->
                                        scope.launch { repository.toggleReminderCompleted(id) }
                                    },
                                    onDeleteReminders = { ids ->
                                        scope.launch { repository.deleteReminders(ids) }
                                    },
                                    onOpenDrawer = {
                                        scope.launch { drawerState.open() }
                                    },
                                    onOpenEventEditor = { event ->
                                        activeEditorState = EditorLaunchState(
                                            initialEvent = event,
                                            targetDate = selectedDate
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating quick "Back to Today" Pill Button whenever user selects another date or navigates away from today
            val isAwayFromToday = selectedDate != todayDate || currentYearMonth != YearMonth.from(todayDate)
            AnimatedVisibility(
                visible = isAwayFromToday &&
                    (currentViewMode == CalendarViewMode.MONTH ||
                        currentViewMode == CalendarViewMode.WEEK ||
                        currentViewMode == CalendarViewMode.DAY) &&
                    showDayPopupForDate == null &&
                    activeEditorState == null &&
                    !showSearchScreen &&
                    !showSettingsScreen,
                enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.accentBlue,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button) {
                            selectedDate = todayDate
                            currentYearMonth = YearMonth.from(todayDate)
                        }
                ) {
                    val todayBtnLabel = when (appLanguage) {
                        AppLanguage.ENGLISH -> "Today (${todayDate.dayOfMonth})"
                        AppLanguage.VIETNAMESE -> "Hôm nay (${todayDate.dayOfMonth}/${todayDate.monthValue})"
                        AppLanguage.CHINESE -> "回到今天 (${todayDate.monthValue}/${todayDate.dayOfMonth})"
                    }
                    Text(
                        text = todayBtnLabel,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Screenshot 3: Floating Day Detail Popup Carousel Overlay
            val popupDate = showDayPopupForDate
            if (popupDate != null) {
                DayDetailPopupCarousel(
                    initialDate = popupDate,
                    todayDate = todayDate,
                    instancesByDate = { date ->
                        repository.getInstancesForRange(date, date)
                    },
                    dailyMetadataMap = dailyMetadataMap,
                    onDateChanged = { activeDate ->
                        selectedDate = activeDate
                        currentYearMonth = YearMonth.from(activeDate)
                    },
                    onDismiss = {
                        showDayPopupForDate = null
                    },
                    onSwitchToDayView = { date ->
                        showDayPopupForDate = null
                        selectedDate = date
                        currentViewMode = CalendarViewMode.DAY
                    },
                    onEventClick = { event ->
                        showDayPopupForDate = null
                        activeEditorState = EditorLaunchState(
                            initialEvent = event,
                            targetDate = selectedDate
                        )
                    },
                    onToggleEventAlarm = { eventId, enabled ->
                        scope.launch {
                            repository.toggleEventAlarm(eventId, enabled)
                            allEvents.find { it.id == eventId }?.let { ev ->
                                alarmScheduler.scheduleEventAlarm(ev.copy(hasAlarm = enabled))
                            }
                        }
                    },
                    onOpenStickerPickerForDate = { date ->
                        showStickerPickerForDate = date
                    },
                    onQuickAddClick = { date ->
                        showDayPopupForDate = null
                        activeEditorState = EditorLaunchState(
                            initialEvent = null,
                            targetDate = date
                        )
                    },
                    onFabAddClick = { date ->
                        showDayPopupForDate = null
                        activeEditorState = EditorLaunchState(
                            initialEvent = null,
                            targetDate = date
                        )
                    }
                )
            }

            // Screenshot 6: Search & Multi-Filter Screen Overlay
            AnimatedVisibility(
                visible = showSearchScreen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160))
            ) {
                SearchScreen(
                    allEvents = allEvents,
                    recentSearches = recentSearches,
                    onSearchSubmit = { query ->
                        scope.launch { repository.addSearchQuery(query) }
                    },
                    onRemoveRecentSearch = { query ->
                        scope.launch { repository.removeSearchQuery(query) }
                    },
                    onClearAllRecentSearches = {
                        scope.launch { repository.clearAllSearchHistory() }
                    },
                    onEventClick = { event ->
                        activeEditorState = EditorLaunchState(
                            initialEvent = event,
                            targetDate = selectedDate
                        )
                    },
                    onDeleteEvents = { ids ->
                        scope.launch {
                            ids.forEach { id ->
                                repository.deleteEvent(id)
                                alarmScheduler.cancelEventAlarm(id)
                            }
                        }
                    },
                    onDeactivateEvents = { ids ->
                        scope.launch {
                            ids.forEach { id ->
                                repository.toggleEventAlarm(id, false)
                                alarmScheduler.cancelEventAlarm(id)
                            }
                        }
                    },
                    onBack = {
                        showSearchScreen = false
                    }
                )
            }

            // Settings Screen Overlay
            AnimatedVisibility(
                visible = showSettingsScreen,
                enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(tween(180))
            ) {
                SettingsBottomSheetOrScreen(
                    appLanguage = appLanguage,
                    onAppLanguageChange = {
                        appLanguage = it
                        prefs.edit().putString("app_language", it.name).apply()
                    },
                    themePreference = themePreference,
                    startWeekOnMonday = startWeekOnMonday,
                    showLunarCalendar = showLunarCalendar,
                    showGlobalHolidays = showGlobalHolidays,
                    selectedTimeZoneId = selectedTimeZoneId,
                    weatherCity = weatherCity,
                    useFahrenheit = useFahrenheit,
                    isSyncing = isSyncing,
                    lastSyncStatus = lastSyncStatus,
                    onThemePreferenceChange = {
                        themePreference = it
                        prefs.edit().putString("theme_pref", it.name).apply()
                    },
                    onStartWeekOnMondayChange = {
                        startWeekOnMonday = it
                        prefs.edit().putBoolean("start_week_monday", it).apply()
                    },
                    onShowLunarCalendarChange = {
                        showLunarCalendar = it
                        prefs.edit().putBoolean("show_lunar", it).apply()
                    },
                    onShowGlobalHolidaysChange = {
                        showGlobalHolidays = it
                        prefs.edit().putBoolean("show_holidays", it).apply()
                    },
                    onTimeZoneChange = {
                        selectedTimeZoneId = it
                        prefs.edit().putString("time_zone_id", it).apply()
                    },
                    onWeatherCityChange = {
                        weatherCity = it
                        prefs.edit().putString("weather_city", it).apply()
                    },
                    onUseFahrenheitChange = {
                        useFahrenheit = it
                        prefs.edit().putBoolean("use_fahrenheit", it).apply()
                    },
                    onSyncSystemCalendarsNow = {
                        val hasRead = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasRead) {
                            scope.launch {
                                isSyncing = true
                                repository.refreshWeatherAndSystemSync()
                                isSyncing = false
                                lastSyncStatus = "Synced System Calendars (Google / Samsung)"
                            }
                        } else {
                            val perms = mutableListOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            calendarPermissionLauncher.launch(perms.toTypedArray())
                        }
                    },
                    onRefreshWeatherNow = {
                        scope.launch {
                            isSyncing = true
                            repository.refreshWeatherAndSystemSync()
                            isSyncing = false
                            lastSyncStatus = "Updated 16-day forecast from Open-Meteo"
                        }
                    },
                    onBack = {
                        showSettingsScreen = false
                    }
                )
            }

            // Jump to Date & Date Calculator Overlay
            AnimatedVisibility(
                visible = showDateToolsScreen,
                enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(tween(180))
            ) {
                DateToolsScreen(
                    initialDate = selectedDate,
                    onJumpToDate = { targetDate ->
                        selectedDate = targetDate
                        currentYearMonth = YearMonth.from(targetDate)
                        showDateToolsScreen = false
                    },
                    onBack = {
                        showDateToolsScreen = false
                    }
                )
            }

            // Screenshot 5: Smart Event Editor Screen with Floating NLP Conflict Card
            val editorState = activeEditorState
            AnimatedVisibility(
                visible = editorState != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(240)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
            ) {
                if (editorState != null) {
                    val targetDayInstances = remember(editorState.targetDate, allEvents, accounts) {
                        repository.getInstancesForRange(
                            editorState.targetDate,
                            editorState.targetDate
                        )
                    }
                    EventEditorScreen(
                        initialEvent = editorState.initialEvent,
                        initialDate = editorState.targetDate,
                        sameDayInstances = targetDayInstances,
                        accounts = accounts,
                        onSaveEvent = { savedEvent ->
                            scope.launch {
                                val id = repository.upsertEvent(savedEvent)
                                val finalEvent = savedEvent.copy(id = id)
                                if (finalEvent.hasAlarm) {
                                    alarmScheduler.scheduleEventAlarm(finalEvent)
                                } else {
                                    alarmScheduler.cancelEventAlarm(id)
                                }
                                // Jump selectedDate/currentYearMonth to the saved event's date so user sees it immediately
                                val eventDate = java.time.Instant.ofEpochMilli(finalEvent.startTimeMillis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                                selectedDate = eventDate
                                currentYearMonth = YearMonth.from(eventDate)
                                activeEditorState = null
                            }
                        },
                        onDeleteEvent = { eventId ->
                            scope.launch {
                                repository.deleteEvent(eventId)
                                alarmScheduler.cancelEventAlarm(eventId)
                                activeEditorState = null
                            }
                        },
                        onCancel = {
                            activeEditorState = null
                        }
                    )
                }
            }

            // Sticker Picker Bottom Sheet for Daily Sticker (😊)
            val stickerTargetDate = showStickerPickerForDate
            if (stickerTargetDate != null) {
                val currentStickerId = dailyMetadataMap[stickerTargetDate.toString()]?.stickerId
                OneUiStickerPickerSheet(
                    selectedStickerId = currentStickerId,
                    onSelectSticker = { sticker ->
                        scope.launch {
                            repository.setDaySticker(stickerTargetDate, sticker?.id)
                            showStickerPickerForDate = null
                        }
                    },
                    onDismiss = {
                        showStickerPickerForDate = null
                    }
                )
            }

            // Month / Year Quick Jump Bottom Sheet when tapping "OCT"
            if (showMonthYearPickerSheet) {
                OneUiMonthYearPickerSheet(
                    currentYearMonth = currentYearMonth,
                    onSelectYearMonth = { ym ->
                        currentYearMonth = ym
                        selectedDate = ym.atDay(1)
                        showMonthYearPickerSheet = false
                    },
                    onDismiss = {
                        showMonthYearPickerSheet = false
                    }
                )
            }
        }
    }
    }
}

private data class EditorLaunchState(
    val initialEvent: CalendarEvent?,
    val targetDate: LocalDate
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OneUiMonthYearPickerSheet(
    currentYearMonth: YearMonth,
    onSelectYearMonth: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = OneUiTheme.colors
    var pickerYear by remember(currentYearMonth) { mutableIntStateOf(currentYearMonth.year) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.elevatedCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pickerYear-- }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous Year",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = pickerYear.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                IconButton(onClick = { pickerYear++ }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next Year",
                        tint = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 4
            ) {
                Month.entries.forEach { month ->
                    val isSelected = pickerYear == currentYearMonth.year && month == currentYearMonth.month
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) {
                                onSelectYearMonth(YearMonth.of(pickerYear, month))
                            },
                        shape = CircleShape,
                        color = if (isSelected) colors.accentBlue else colors.controlSubtleBackground
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = month.getDisplayName(TextStyle.SHORT, Locale.US),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
