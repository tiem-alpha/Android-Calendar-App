package com.oneui.calendar.feature.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.designsystem.components.OneUiDottedDivider
import com.oneui.calendar.core.designsystem.theme.OneUiShapes
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import com.oneui.calendar.domain.model.AccountGroupType
import com.oneui.calendar.domain.model.CalendarAccount
import com.oneui.calendar.domain.model.CalendarViewMode

@Composable
fun OneUiNavigationDrawer(
    currentViewMode: CalendarViewMode,
    accounts: List<CalendarAccount>,
    onSelectViewMode: (CalendarViewMode) -> Unit,
    onToggleAccountVisibility: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDateTools: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    OneUiNavigationDrawerContent(
        currentViewMode = currentViewMode,
        accounts = accounts,
        onSelectViewMode = onSelectViewMode,
        onToggleAccountVisibility = onToggleAccountVisibility,
        onOpenSettings = onOpenSettings,
        onOpenDateTools = onOpenDateTools,
        onCloseDrawer = onCloseDrawer,
        modifier = modifier
    )
}

@Composable
fun OneUiNavigationDrawerContent(
    currentViewMode: CalendarViewMode,
    accounts: List<CalendarAccount>,
    onSelectViewMode: (CalendarViewMode) -> Unit,
    onToggleAccountVisibility: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDateTools: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val haptic = LocalHapticFeedback.current

    var myPhoneExpanded by remember { mutableStateOf(true) }
    var samsungExpanded by remember { mutableStateOf(false) }
    var googleExpanded by remember { mutableStateOf(false) }

    val myPhoneAccounts = remember(accounts) {
        accounts.filter { it.groupType == AccountGroupType.MY_PHONE }
    }
    val samsungAccounts = remember(accounts) {
        accounts.filter { it.groupType == AccountGroupType.SAMSUNG }
    }
    val googleAccounts = remember(accounts) {
        accounts.filter { it.groupType == AccountGroupType.GOOGLE }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.84f)
            .widthIn(min = 290.dp, max = 330.dp)
            .fillMaxHeight(),
        shape = OneUiShapes.DrawerShape,
        color = colors.focusBlockBackground,
        shadowElevation = if (colors.isDark) 0.dp else 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // Top Settings Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSettings()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Calendar settings",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            OneUiDottedDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // 5 View Mode Items (Year, Month, Week, Day, Reminder) matching Screenshot 7
            val strings = LocalAppStrings.current
            CalendarViewMode.entries.forEach { mode ->
                val isSelected = mode == currentViewMode
                val localizedLabel = when (mode) {
                    CalendarViewMode.YEAR -> strings.viewYear
                    CalendarViewMode.MONTH -> strings.viewMonth
                    CalendarViewMode.WEEK -> strings.viewWeek
                    CalendarViewMode.DAY -> strings.viewDay
                    CalendarViewMode.REMINDER -> strings.viewReminder
                }
                DrawerViewModeRow(
                    label = localizedLabel,
                    icon = viewModeIcon(mode),
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectViewMode(mode)
                        onCloseDrawer()
                    }
                )
            }

            DrawerViewModeRow(
                label = when (strings.language) {
                    com.oneui.calendar.core.common.AppLanguage.ENGLISH -> "Jump & Date Calculator"
                    com.oneui.calendar.core.common.AppLanguage.VIETNAMESE -> "Nhảy tới ngày & Tính ngày"
                    com.oneui.calendar.core.common.AppLanguage.CHINESE -> "跳转与日期计算"
                },
                icon = Icons.Rounded.Calculate,
                selected = false,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCloseDrawer()
                    onOpenDateTools()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            OneUiDottedDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // Collapsible "My phone" Section
            val phoneChevronRotation by animateFloatAsState(
                targetValue = if (myPhoneExpanded) 0f else -90f,
                animationSpec = tween(200),
                label = "myPhoneChevron"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable(role = Role.Button) {
                        myPhoneExpanded = !myPhoneExpanded
                    }
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Smartphone,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.myPhone,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (myPhoneExpanded) "Collapse" else "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = phoneChevronRotation }
                )
            }

            AnimatedVisibility(
                visible = myPhoneExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    val itemsToRender = myPhoneAccounts.ifEmpty {
                        listOf(
                            CalendarAccount(
                                id = 1L,
                                accountName = "My phone",
                                displayName = "My calendars",
                                groupType = AccountGroupType.MY_PHONE,
                                colorHex = 0xFFB39DFF,
                                isVisible = true
                            ),
                            CalendarAccount(
                                id = 2L,
                                accountName = "abc123@gmail.com",
                                displayName = "abc123@gmail.com",
                                groupType = AccountGroupType.MY_PHONE,
                                colorHex = 0xFFB39DFF,
                                isVisible = false
                            )
                        )
                    }
                    itemsToRender.forEach { account ->
                        val checkColor = if (account.colorHex == 0L) {
                            Color(0xFFB39DFF)
                        } else {
                            Color(account.colorHex)
                        }
                        AccountCheckboxRow(
                            label = account.displayName,
                            checked = account.isVisible,
                            checkedFillColor = checkColor,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleAccountVisibility(account.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OneUiDottedDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // External Account Section: Google
            val googleAccount = googleAccounts.firstOrNull()
            val googleChevronRotation by animateFloatAsState(
                targetValue = if (googleExpanded) 180f else 0f,
                animationSpec = tween(200),
                label = "googleChevron"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(role = Role.Button) {
                        googleExpanded = !googleExpanded
                    }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GoogleLogoBadge()
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google",
                            fontSize = 17.sp,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = googleAccount?.accountName ?: "abc123@gmail.com",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Expand Google account",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = googleChevronRotation }
                )
            }

            AnimatedVisibility(
                visible = googleExpanded && googleAccounts.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    googleAccounts.forEach { account ->
                        AccountCheckboxRow(
                            label = account.displayName,
                            checked = account.isVisible,
                            checkedFillColor = Color(0xFF4285F4),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleAccountVisibility(account.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerViewModeRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    val rowShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(50.dp)
            .clip(rowShape)
            .background(if (selected) colors.controlSubtleBackground else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun AccountCheckboxRow(
    label: String,
    checked: Boolean,
    checkedFillColor: Color,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (checked) {
                        Modifier.background(checkedFillColor)
                    } else {
                        Modifier.border(1.5.dp, colors.textTertiary, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Visible",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 16.5.sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GoogleLogoBadge() {
    val colors = OneUiTheme.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(
                width = 0.8.dp,
                color = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val strokePx = 3.2.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Red top-left arc
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 200f,
                sweepAngle = 115f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            // Yellow left arc
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 135f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            // Green bottom arc
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 35f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            // Blue right arc + horizontal bar
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -5f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.52f, size.height * 0.5f),
                end = Offset(size.width - inset, size.height * 0.5f),
                strokeWidth = strokePx,
                cap = StrokeCap.Butt
            )
        }
    }
}

private fun viewModeIcon(mode: CalendarViewMode): ImageVector {
    return when (mode) {
        CalendarViewMode.YEAR -> Icons.Outlined.CalendarViewMonth
        CalendarViewMode.MONTH -> Icons.Outlined.CalendarMonth
        CalendarViewMode.WEEK -> Icons.Outlined.DateRange
        CalendarViewMode.DAY -> Icons.Outlined.Today
        CalendarViewMode.REMINDER -> Icons.Outlined.Notifications
    }
}
