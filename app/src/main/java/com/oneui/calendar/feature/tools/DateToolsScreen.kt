package com.oneui.calendar.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.calendar.core.common.AppLanguage
import com.oneui.calendar.core.common.LocalAppStrings
import com.oneui.calendar.core.designsystem.components.OneUiDottedDivider
import com.oneui.calendar.core.designsystem.components.OneUiFocusBlock
import com.oneui.calendar.core.designsystem.theme.OneUiTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateToolsScreen(
    initialDate: LocalDate,
    onJumpToDate: (LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OneUiTheme.colors
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current

    var jumpTargetDate by remember { mutableStateOf(initialDate) }
    var calcStartDate by remember { mutableStateOf(initialDate) }
    var calcEndDate by remember { mutableStateOf(initialDate.plusDays(30)) }

    var activePickerTarget by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember(strings.language) {
        when (strings.language) {
            AppLanguage.ENGLISH -> DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
            AppLanguage.VIETNAMESE -> DateTimeFormatter.ofPattern("EEE, d 'Thg' M, yyyy", Locale("vi", "VN"))
            AppLanguage.CHINESE -> DateTimeFormatter.ofPattern("yyyy年M月d日 EEE", Locale.SIMPLIFIED_CHINESE)
        }
    }

    val diffDays = ChronoUnit.DAYS.between(calcStartDate, calcEndDate)
    val absDays = abs(diffDays)
    val weeks = absDays / 7
    val remDays = absDays % 7
    val monthsApprox = absDays / 30

    val ambientBrush = if (colors.isDark) {
        Brush.verticalGradient(listOf(Color(0xFF141E2B).copy(alpha = 0.4f), colors.canvasBackground))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEAF3FF), colors.canvasBackground))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ambientBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = when (strings.language) {
                    AppLanguage.ENGLISH -> "Jump to Date & Calculator"
                    AppLanguage.VIETNAMESE -> "Nhảy tới ngày & Tính ngày"
                    AppLanguage.CHINESE -> "跳转日期与日期计算"
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Jump to Date
            Text(
                text = when (strings.language) {
                    AppLanguage.ENGLISH -> "Jump to Date"
                    AppLanguage.VIETNAMESE -> "Nhảy nhanh tới ngày"
                    AppLanguage.CHINESE -> "快速跳转日期"
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            OneUiFocusBlock(contentPadding = PaddingValues(20.dp)) {
                DatePickerRowItem(
                    label = when (strings.language) {
                        AppLanguage.ENGLISH -> "Target date"
                        AppLanguage.VIETNAMESE -> "Ngày muốn đến"
                        AppLanguage.CHINESE -> "目标日期"
                    },
                    value = jumpTargetDate.format(dateFormatter),
                    onClick = { activePickerTarget = "JUMP" }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.accentBlue)
                        .clickable(role = Role.Button) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onJumpToDate(jumpTargetDate)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (strings.language) {
                                AppLanguage.ENGLISH -> "Jump to this date"
                                AppLanguage.VIETNAMESE -> "Chuyển đến ngày này"
                                AppLanguage.CHINESE -> "跳转到此日期"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Section 2: Date Duration Calculator
            Text(
                text = when (strings.language) {
                    AppLanguage.ENGLISH -> "Date Calculator"
                    AppLanguage.VIETNAMESE -> "Tính số ngày giữa 2 mốc"
                    AppLanguage.CHINESE -> "日期间隔计算器"
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            OneUiFocusBlock(contentPadding = PaddingValues(20.dp)) {
                DatePickerRowItem(
                    label = when (strings.language) {
                        AppLanguage.ENGLISH -> "Start date"
                        AppLanguage.VIETNAMESE -> "Ngày bắt đầu"
                        AppLanguage.CHINESE -> "开始日期"
                    },
                    value = calcStartDate.format(dateFormatter),
                    onClick = { activePickerTarget = "START" }
                )

                OneUiDottedDivider(modifier = Modifier.padding(vertical = 12.dp))

                DatePickerRowItem(
                    label = when (strings.language) {
                        AppLanguage.ENGLISH -> "End date"
                        AppLanguage.VIETNAMESE -> "Ngày kết thúc"
                        AppLanguage.CHINESE -> "结束日期"
                    },
                    value = calcEndDate.format(dateFormatter),
                    onClick = { activePickerTarget = "END" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pastel Result Card
                val resultBg = if (colors.isDark) Color(0xFF141E2B) else Color(0xFFEAF3FF)
                val resultInk = if (colors.isDark) Color(0xFF79B6FF) else Color(0xFF226BD6)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(resultBg)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = null,
                            tint = resultInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (strings.language) {
                                AppLanguage.ENGLISH -> "Total Duration"
                                AppLanguage.VIETNAMESE -> "Tổng số ngày"
                                AppLanguage.CHINESE -> "相差总天数"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = resultInk
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (strings.language) {
                            AppLanguage.ENGLISH -> "$absDays days"
                            AppLanguage.VIETNAMESE -> "$absDays ngày"
                            AppLanguage.CHINESE -> "$absDays 天"
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = resultInk
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (strings.language) {
                            AppLanguage.ENGLISH -> "$weeks weeks, $remDays days  •  ~$monthsApprox months"
                            AppLanguage.VIETNAMESE -> "$weeks tuần, $remDays ngày  •  ~$monthsApprox tháng"
                            AppLanguage.CHINESE -> "$weeks 周 $remDays 天  •  约 $monthsApprox 个月"
                        },
                        fontSize = 13.5.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(colors.controlSubtleBackground)
                        .clickable(role = Role.Button) {
                            val tmp = calcStartDate
                            calcStartDate = calcEndDate
                            calcEndDate = tmp
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (strings.language) {
                            AppLanguage.ENGLISH -> "Swap dates"
                            AppLanguage.VIETNAMESE -> "Đổi ngày bắt đầu / kết thúc"
                            AppLanguage.CHINESE -> "互换起止日期"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (activePickerTarget != null) {
        val currentTargetDate = when (activePickerTarget) {
            "JUMP" -> jumpTargetDate
            "START" -> calcStartDate
            else -> calcEndDate
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentTargetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { activePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            when (activePickerTarget) {
                                "JUMP" -> jumpTargetDate = picked
                                "START" -> calcStartDate = picked
                                "END" -> calcEndDate = picked
                            }
                        }
                        activePickerTarget = null
                    }
                ) {
                    Text(strings.save, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activePickerTarget = null }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DatePickerRowItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = OneUiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colors.accentBlue,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 15.5.sp,
                color = colors.textSecondary
            )
        }
        Text(
            text = value,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}
