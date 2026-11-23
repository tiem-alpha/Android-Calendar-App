# Calender — Samsung One UI Android Calendar & MCP Server

A modern, offline-first **Android Calendar & Reminder** application crafted with **Jetpack Compose** and **Samsung One UI Design System (One UI 6/7/8)**, featuring **Astronomical Lunar Calendar**, **Global Holidays**, **Real-time NLP Conflict Detection**, **Date Calculator**, **Multi-language Support (English / Tiếng Việt / 中文)**, and an **Embedded Model Context Protocol (MCP) Server** for AI Agent integration.

---

## Key Features

### 1. Ergonomic Samsung One UI Experience
- **5 Calendar Views:**
  - **Expanded Month View & Split Month Agenda:** Smooth spring-physics morphing between full-screen month grid (with continuous multi-day pastel ribbons) and split agenda panel with Lunar date & Open-Meteo weather forecast.
  - **Day Detail Popup Carousel:** Floating `26dp` squircle cards in a `HorizontalPager` with adjacent days peeking from both edges.
  - **Week & Day Timeline Views:** 24-hour scrollable grid with live red current-time indicator (`►────`), all-day pastel banners, overlap column packing, and `+N` cluster badges.
  - **Year Overview:** 3×4 mini-month grid with event indicator dots.
  - **Reminder Manager:** One UI Expandable Collapsible Toolbar, D-Day filter, and long-press Multi-Select mode.
- **One-Handed Quick Add & Back-to-Today:** Bottom-anchored quick-add pill + elevated FAB (`+`) and floating **Back to Today** pill whenever browsing other dates.
- **Light Soft Pastel & OLED Dark Mode (`#000000`):** Ambient Pastel Glow headers and high-contrast OLED dark surfaces.

### 2. Smart Productivity & Localization
- **Smart NLP & Schedule Conflict Warning:** Automatically extracts times from natural language titles (e.g. `"3pm Movie"` $\rightarrow$ `3:00PM ~ 4:00PM`), previews conflicting events in real time, and prompts a confirmation dialog before saving overlapping schedules.
- **Astronomical Lunar Calendar (100% Offline):** Ho Ngoc Duc / Jean Meeus algorithm displaying mini lunar dates directly inside calendar cells and supporting lunar recurring events.
- **Global Public Holidays:** Automatically highlights Saturdays, Sundays, and country-specific holidays in red based on selected language (**Vietnam**, **China**, **US/UK**).
- **Jump to Date & Date Calculator:** Jump directly to any date and calculate exact duration (total days, weeks, remaining days, approximate months) between any start and end date.
- **Multi-Filter Search with Multi-Select:** Full-text search (`SQLite FTS4`), voice search, recent search chips, Sticker & Color filters, plus long-press Multi-Select (`Select all`, `Deactivate`, `Delete`).
- **Multi-Language & Persistent Settings:** Instant switching between **English**, **Tiếng Việt**, and **中文**, with Time Zone, Lunar Calendar, Global Holidays, Weather City, and Theme persisted via `SharedPreferences`.

### 3. Embedded MCP Server (AI Agent Integration)
The app embeds a lightweight **Model Context Protocol (MCP) JSON-RPC 2.0 HTTP Server** on port `8765` (`POST /mcp`).

#### Connect from PC via ADB
```bash
adb forward tcp:8765 tcp:8765
```

#### Exposed MCP Tools (`http://127.0.0.1:8765/mcp`)
| Tool | Description |
| :--- | :--- |
| `list_events` | Load events in a date range (`start_date`, `end_date` in `YYYY-MM-DD`) |
| `get_event` | Read a single event by `event_id` |
| `create_event` | Create a new event & automatically schedule exact alarm |
| `update_event` | Update an existing event by `event_id` |
| `delete_event` | Delete an event & cancel its alarm |
| `search_events` | Search events by `query`, `sticker_id`, or `color_tag` |

---

## Tech Stack & Architecture

- **Language & UI:** Kotlin `2.2.10`, Jetpack Compose (`BOM 2024.10.01`), Material 3 + Custom One UI Design System
- **Build System:** Gradle `9.5.0`, Android Gradle Plugin `9.3.3` (Built-in Kotlin), Compile/Target SDK `35`, Min SDK `26`
- **Storage & Search:** Offline-first `SQLiteOpenHelper` + `FTS4` Virtual Table + Reactive `StateFlow`
- **Sync & Weather:** Two-way Android `CalendarContract` sync + `Open-Meteo API` (16-day forecast, no API key required)
- **Alarms:** `AlarmManager.setExactAndAllowWhileIdle` + `BroadcastReceiver` (Snooze 10m / Dismiss / Boot reschedule)

### Project Structure
```text
app/src/main/java/com/oneui/calendar/
├── core/
│   ├── common/          # AppLocalization, LunarCalendarConverter, HolidayProvider, RRuleEvaluator, NLP & Slot Engines
│   ├── designsystem/    # OneUiTheme (Light Pastel / OLED Dark) & OneUiCommonComponents
│   ├── mcp/             # CalendarMcpServer (JSON-RPC 2.0 over HTTP :8765)
│   └── notifications/   # ExactAlarmScheduler & AlarmReceiver
├── data/                # CalendarDatabaseHelper (SQLite+FTS4), OpenMeteoWeatherService, SystemCalendarSyncManager
├── domain/              # CalendarModels
└── feature/             # month, week, year, editor, search, navigation, reminder, tools (DateToolsScreen)
```

---

## Build & Installation

### Build from Source (Offline-ready)
```bash
./gradlew assembleDebug
# or for release APK:
./gradlew assembleRelease
```

### Download Pre-built APK
Grab the latest APK directly from the [GitHub Releases](https://github.com/tiem-alpha/Android-Calendar-App/releases) page.
