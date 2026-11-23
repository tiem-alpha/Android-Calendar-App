package com.oneui.calendar.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.domain.model.EventColorTag

@Immutable
data class OneUiColorScheme(
    val isDark: Boolean,
    val canvasBackground: Color,
    val calendarGridBackground: Color,
    val focusBlockBackground: Color,
    val elevatedCardBackground: Color,
    val controlSubtleBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val sundayRed: Color,
    val accentBlue: Color,
    val todayBadgeBg: Color,
    val todayBadgeText: Color,
    val dividerHairline: Color,
    val selectedCellBorder: Color
)

val LightOneUiColorScheme = OneUiColorScheme(
    isDark = false,
    canvasBackground = Color(0xFFF6F6F8),
    calendarGridBackground = Color(0xFFFFFFFF),
    focusBlockBackground = Color(0xFFFFFFFF),
    elevatedCardBackground = Color(0xFFFFFFFF),
    controlSubtleBackground = Color(0xFFF2F3F5),
    textPrimary = Color(0xFF101114),
    textSecondary = Color(0xFF636366),
    textTertiary = Color(0xFF8E8E93),
    sundayRed = Color(0xFFD93025),
    accentBlue = Color(0xFF0381FE),
    todayBadgeBg = Color(0xFF1F1F22),
    todayBadgeText = Color(0xFFFFFFFF),
    dividerHairline = Color(0x14000000),
    selectedCellBorder = Color(0xFFB8B8BC)
)

val DarkOneUiColorScheme = OneUiColorScheme(
    isDark = true,
    canvasBackground = Color(0xFF000000),
    calendarGridBackground = Color(0xFF000000),
    focusBlockBackground = Color(0xFF171719),
    elevatedCardBackground = Color(0xFF252528),
    controlSubtleBackground = Color(0xFF2C2C2F),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA3A3A8),
    textTertiary = Color(0xFF727278),
    sundayRed = Color(0xFFFF5C5C),
    accentBlue = Color(0xFF3E9EFF),
    todayBadgeBg = Color(0xFFFAFAFA),
    todayBadgeText = Color(0xFF101114),
    dividerHairline = Color(0x1CFFFFFF),
    selectedCellBorder = Color(0xFF525258)
)

val LocalOneUiColors = staticCompositionLocalOf { LightOneUiColorScheme }

object OneUiShapes {
    val FocusBlockShape = RoundedCornerShape(26.dp)
    val DrawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp)
    val PopupCardShape = RoundedCornerShape(26.dp)
    val FloatingConflictCardShape = RoundedCornerShape(22.dp)
    val DayCellSelectionShape = RoundedCornerShape(8.dp)
    val TodaySquircleShape = RoundedCornerShape(8.dp)
    val EventPillShape = RoundedCornerShape(5.dp)
    val FullPillShape = CircleShape
}

object OneUiTheme {
    val colors: OneUiColorScheme
        @Composable
        get() = LocalOneUiColors.current
}

private val OneUiTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun OneUiCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val oneUiColors = if (darkTheme) DarkOneUiColorScheme else LightOneUiColorScheme
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = oneUiColors.accentBlue,
            onPrimary = Color(0xFF001D36),
            background = oneUiColors.canvasBackground,
            onBackground = oneUiColors.textPrimary,
            surface = oneUiColors.focusBlockBackground,
            onSurface = oneUiColors.textPrimary,
            surfaceVariant = oneUiColors.controlSubtleBackground,
            onSurfaceVariant = oneUiColors.textSecondary,
            outline = oneUiColors.dividerHairline,
            error = oneUiColors.sundayRed
        )
    } else {
        lightColorScheme(
            primary = oneUiColors.accentBlue,
            onPrimary = Color.White,
            background = oneUiColors.canvasBackground,
            onBackground = oneUiColors.textPrimary,
            surface = oneUiColors.focusBlockBackground,
            onSurface = oneUiColors.textPrimary,
            surfaceVariant = oneUiColors.controlSubtleBackground,
            onSurfaceVariant = oneUiColors.textSecondary,
            outline = oneUiColors.dividerHairline,
            error = oneUiColors.sundayRed
        )
    }

    CompositionLocalProvider(LocalOneUiColors provides oneUiColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = OneUiTypography,
            content = content
        )
    }
}

@Composable
fun EventColorTag.containerColor(): Color {
    return Color(if (OneUiTheme.colors.isDark) darkBgHex else lightBgHex)
}

@Composable
fun EventColorTag.accentBarColor(): Color {
    return Color(if (OneUiTheme.colors.isDark) darkBarHex else lightBarHex)
}

@Composable
fun EventColorTag.textColor(): Color {
    return OneUiTheme.colors.textPrimary
}
