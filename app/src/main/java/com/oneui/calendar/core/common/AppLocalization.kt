package com.oneui.calendar.core.common

import androidx.compose.runtime.compositionLocalOf
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class AppLanguage(val code: String, val nativeLabel: String, val locale: Locale) {
    ENGLISH("en", "English", Locale.US),
    VIETNAMESE("vi", "Tiếng Việt", Locale("vi", "VN")),
    CHINESE("zh", "中文", Locale.SIMPLIFIED_CHINESE)
}

data class AppStrings(
    val language: AppLanguage,
    // View Modes
    val viewYear: String,
    val viewMonth: String,
    val viewWeek: String,
    val viewDay: String,
    val viewReminder: String,
    // Quick Add
    val addEventOnPrefix: String,
    // Drawer
    val myPhone: String,
    val myCalendars: String,
    // Search
    val searchPlaceholder: String,
    val recentSearches: String,
    val clearAll: String,
    val sticker: String,
    val color: String,
    val noMatchingEvents: String,
    // Editor
    val titlePlaceholder: String,
    val allDay: String,
    val dDayLabel: String,
    val lunarLabel: String,
    val locationPlaceholder: String,
    val reminderLabel: String,
    val repeatLabel: String,
    val notesPlaceholder: String,
    val cancel: String,
    val save: String,
    val deleteEvent: String,
    // Settings
    val settingsTitle: String,
    val appearanceSection: String,
    val languageLabel: String,
    val themeModeLabel: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val startWeekOnMonday: String,
    val weekBeginsMonday: String,
    val weekBeginsSunday: String,
    val showLunarCalendar: String,
    val showLunarSubtitle: String,
    val weatherSection: String,
    val weatherCityLabel: String,
    val refreshWeatherBtn: String,
    val accountsSyncSection: String,
    val syncSystemCalendarsBtn: String,
    // Reminders
    val remindersTitle: String,
    val addReminderPlaceholder: String,
    val filterAll: String,
    val filterPending: String,
    val filterCompleted: String,
    val filterDDay: String,
    // Weekdays (Sun..Sat)
    val weekdaysSunFirst: List<String>,
    val weekdaysMonFirst: List<String>,
    val weekShortPrefix: String
) {
    fun formatMonthHeader(month: Month): String {
        return when (language) {
            AppLanguage.ENGLISH -> month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US)
            AppLanguage.VIETNAMESE -> "THG ${month.value}"
            AppLanguage.CHINESE -> "${month.value}月"
        }
    }

    fun formatQuickAddPlaceholder(date: LocalDate): String {
        return when (language) {
            AppLanguage.ENGLISH -> "Add event on ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}"
            AppLanguage.VIETNAMESE -> "Thêm sự kiện ngày ${date.dayOfMonth}/${date.monthValue}"
            AppLanguage.CHINESE -> "在${date.monthValue}月${date.dayOfMonth}日添加日程"
        }
    }

    fun formatLunarSubDate(date: LocalDate): String {
        val lunar = LunarCalendarConverter.solarToLunar(date)
        return when (language) {
            AppLanguage.ENGLISH -> LunarCalendarConverter.formatShortLunarSubDate(date)
            AppLanguage.VIETNAMESE -> {
                val leapSuffix = if (lunar.isLeapMonth) " Nhuận" else ""
                "${lunar.day} Thg ${lunar.month}$leapSuffix ÂL"
            }
            AppLanguage.CHINESE -> {
                val months = arrayOf("", "正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
                val days = arrayOf(
                    "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
                    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
                )
                val leapPrefix = if (lunar.isLeapMonth) "闰" else ""
                "农历 $leapPrefix${months.getOrNull(lunar.month) ?: "${lunar.month}月"}${days.getOrNull(lunar.day) ?: "${lunar.day}"}"
            }
        }
    }

    fun formatMiniLunarCell(date: LocalDate): String {
        val lunar = LunarCalendarConverter.solarToLunar(date)
        return when (language) {
            AppLanguage.CHINESE -> formatChineseLunarCell(lunar)
            AppLanguage.VIETNAMESE -> if (lunar.day == 1) "${lunar.day}/${lunar.month}" else "${lunar.day}"
            AppLanguage.ENGLISH -> if (lunar.day == 1) "${lunar.day}/${lunar.month}" else "${lunar.day}"
        }
    }

    private fun formatChineseLunarCell(lunar: LunarDate): String {
        val months = arrayOf("", "正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
        val days = arrayOf(
            "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
        return if (lunar.day == 1) {
            val prefix = if (lunar.isLeapMonth) "闰" else ""
            prefix + (months.getOrNull(lunar.month) ?: "${lunar.month}月")
        } else {
            days.getOrNull(lunar.day) ?: "${lunar.day}"
        }
    }
}

fun appStringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ENGLISH -> AppStrings(
        language = language,
        viewYear = "Year",
        viewMonth = "Month",
        viewWeek = "Week",
        viewDay = "Day",
        viewReminder = "Reminder",
        addEventOnPrefix = "Add event on",
        myPhone = "My phone",
        myCalendars = "My calendars",
        searchPlaceholder = "Search",
        recentSearches = "Recent Searches",
        clearAll = "Clear all",
        sticker = "Sticker",
        color = "Color",
        noMatchingEvents = "No matching events",
        titlePlaceholder = "Title",
        allDay = "All-day",
        dDayLabel = "D-Day countdown",
        lunarLabel = "Lunar calendar",
        locationPlaceholder = "Location",
        reminderLabel = "Reminder",
        repeatLabel = "Repeat",
        notesPlaceholder = "Notes",
        cancel = "Cancel",
        save = "Save",
        deleteEvent = "Delete event",
        settingsTitle = "Calendar settings",
        appearanceSection = "Appearance & Calendar",
        languageLabel = "Language",
        themeModeLabel = "Theme Mode",
        themeSystem = "System",
        themeLight = "Light Pastel",
        themeDark = "OLED Dark",
        startWeekOnMonday = "Start week on Monday",
        weekBeginsMonday = "Week begins on Monday",
        weekBeginsSunday = "Week begins on Sunday",
        showLunarCalendar = "Show Lunar Calendar",
        showLunarSubtitle = "Show mini lunar dates inside calendar cells",
        weatherSection = "Weather",
        weatherCityLabel = "Weather City",
        refreshWeatherBtn = "Refresh Open-Meteo Weather",
        accountsSyncSection = "Accounts & Sync",
        syncSystemCalendarsBtn = "Sync System Calendars Now (Google)",
        remindersTitle = "Reminders",
        addReminderPlaceholder = "Add a reminder...",
        filterAll = "All",
        filterPending = "Pending",
        filterCompleted = "Completed",
        filterDDay = "D-Day Events",
        weekdaysSunFirst = listOf("S", "M", "T", "W", "T", "F", "S"),
        weekdaysMonFirst = listOf("M", "T", "W", "T", "F", "S", "S"),
        weekShortPrefix = "WK"
    )

    AppLanguage.VIETNAMESE -> AppStrings(
        language = language,
        viewYear = "Năm",
        viewMonth = "Tháng",
        viewWeek = "Tuần",
        viewDay = "Ngày",
        viewReminder = "Nhắc nhở",
        addEventOnPrefix = "Thêm sự kiện ngày",
        myPhone = "Điện thoại",
        myCalendars = "Lịch của tôi",
        searchPlaceholder = "Tìm kiếm",
        recentSearches = "Tìm kiếm gần đây",
        clearAll = "Xóa tất cả",
        sticker = "Nhãn dán",
        color = "Màu sắc",
        noMatchingEvents = "Không tìm thấy sự kiện phù hợp",
        titlePlaceholder = "Tiêu đề",
        allDay = "Cả ngày",
        dDayLabel = "Đếm ngược D-Day",
        lunarLabel = "Lịch Âm",
        locationPlaceholder = "Địa điểm",
        reminderLabel = "Nhắc nhở",
        repeatLabel = "Lặp lại",
        notesPlaceholder = "Ghi chú",
        cancel = "Hủy",
        save = "Lưu",
        deleteEvent = "Xóa sự kiện",
        settingsTitle = "Cài đặt lịch",
        appearanceSection = "Giao diện & Lịch",
        languageLabel = "Ngôn ngữ",
        themeModeLabel = "Chế độ giao diện",
        themeSystem = "Hệ thống",
        themeLight = "Sáng Pastel",
        themeDark = "Tối OLED",
        startWeekOnMonday = "Bắt đầu tuần từ Thứ Hai",
        weekBeginsMonday = "Tuần bắt đầu từ Thứ Hai",
        weekBeginsSunday = "Tuần bắt đầu từ Chủ Nhật",
        showLunarCalendar = "Hiển thị Lịch Âm",
        showLunarSubtitle = "Hiện ngày âm lịch nhỏ trong các ô ngày",
        weatherSection = "Thời tiết",
        weatherCityLabel = "Thành phố",
        refreshWeatherBtn = "Cập nhật thời tiết Open-Meteo",
        accountsSyncSection = "Tài khoản & Đồng bộ",
        syncSystemCalendarsBtn = "Đồng bộ Lịch Hệ Thống (Google)",
        remindersTitle = "Nhắc nhở",
        addReminderPlaceholder = "Thêm nhắc nhở mới...",
        filterAll = "Tất cả",
        filterPending = "Chưa xong",
        filterCompleted = "Đã xong",
        filterDDay = "Sự kiện D-Day",
        weekdaysSunFirst = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7"),
        weekdaysMonFirst = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"),
        weekShortPrefix = "T"
    )

    AppLanguage.CHINESE -> AppStrings(
        language = language,
        viewYear = "年",
        viewMonth = "月",
        viewWeek = "周",
        viewDay = "日",
        viewReminder = "提醒",
        addEventOnPrefix = "添加日程",
        myPhone = "我的手机",
        myCalendars = "我的日历",
        searchPlaceholder = "搜索",
        recentSearches = "最近搜索",
        clearAll = "全部清除",
        sticker = "贴纸",
        color = "颜色",
        noMatchingEvents = "没有匹配的日程",
        titlePlaceholder = "标题",
        allDay = "全天",
        dDayLabel = "倒数日 D-Day",
        lunarLabel = "农历",
        locationPlaceholder = "地点",
        reminderLabel = "提醒",
        repeatLabel = "重复",
        notesPlaceholder = "备注",
        cancel = "取消",
        save = "保存",
        deleteEvent = "删除日程",
        settingsTitle = "日历设置",
        appearanceSection = "外观与日历",
        languageLabel = "语言 / Language",
        themeModeLabel = "主题模式",
        themeSystem = "跟随系统",
        themeLight = "浅色柔彩",
        themeDark = "深色 OLED",
        startWeekOnMonday = "周一作为每周第一天",
        weekBeginsMonday = "每周从周一开始",
        weekBeginsSunday = "每周从周日开始",
        showLunarCalendar = "显示农历",
        showLunarSubtitle = "在日历单元格中显示小号农历日期",
        weatherSection = "天气",
        weatherCityLabel = "天气城市",
        refreshWeatherBtn = "刷新 Open-Meteo 天气",
        accountsSyncSection = "账户与同步",
        syncSystemCalendarsBtn = "同步系统日历 (Google)",
        remindersTitle = "提醒事项",
        addReminderPlaceholder = "添加提醒...",
        filterAll = "全部",
        filterPending = "待办",
        filterCompleted = "已完成",
        filterDDay = "倒数日",
        weekdaysSunFirst = listOf("日", "一", "二", "三", "四", "五", "六"),
        weekdaysMonFirst = listOf("一", "二", "三", "四", "五", "六", "日"),
        weekShortPrefix = "周"
    )
}

val LocalAppStrings = compositionLocalOf { appStringsFor(AppLanguage.ENGLISH) }
