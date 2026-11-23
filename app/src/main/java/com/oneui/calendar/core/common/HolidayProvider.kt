package com.oneui.calendar.core.common

import java.time.LocalDate

data class PublicHoliday(
    val date: LocalDate,
    val name: String
)

object HolidayProvider {

    fun getHolidayForDate(date: LocalDate, language: AppLanguage): PublicHoliday? {
        val lunar = LunarCalendarConverter.solarToLunar(date)
        return when (language) {
            AppLanguage.VIETNAMESE -> getVietnamHoliday(date, lunar)
            AppLanguage.CHINESE -> getChinaHoliday(date, lunar)
            AppLanguage.ENGLISH -> getUsUkHoliday(date)
        }
    }

    private fun getVietnamHoliday(date: LocalDate, lunar: LunarDate): PublicHoliday? {
        // Fixed Solar Holidays in Vietnam
        when (date.monthValue to date.dayOfMonth) {
            1 to 1 -> return PublicHoliday(date, "Tết Dương lịch")
            4 to 30 -> return PublicHoliday(date, "Giải phóng miền Nam")
            5 to 1 -> return PublicHoliday(date, "Quốc tế Lao động")
            9 to 2 -> return PublicHoliday(date, "Quốc khánh 2/9")
        }
        // Lunar Holidays in Vietnam
        if (!lunar.isLeapMonth) {
            when (lunar.month to lunar.day) {
                1 to 1 -> return PublicHoliday(date, "Mùng 1 Tết")
                1 to 2 -> return PublicHoliday(date, "Mùng 2 Tết")
                1 to 3 -> return PublicHoliday(date, "Mùng 3 Tết")
                3 to 10 -> return PublicHoliday(date, "Giỗ Tổ Hùng Vương")
                8 to 15 -> return PublicHoliday(date, "Tết Trung Thu")
            }
        }
        return null
    }

    private fun getChinaHoliday(date: LocalDate, lunar: LunarDate): PublicHoliday? {
        // Fixed Solar Holidays in China
        when (date.monthValue to date.dayOfMonth) {
            1 to 1 -> return PublicHoliday(date, "元旦")
            4 to 5 -> return PublicHoliday(date, "清明节")
            5 to 1 -> return PublicHoliday(date, "劳动节")
            10 to 1 -> return PublicHoliday(date, "国庆节")
            10 to 2 -> return PublicHoliday(date, "国庆节")
            10 to 3 -> return PublicHoliday(date, "国庆节")
        }
        // Lunar Holidays in China
        if (!lunar.isLeapMonth) {
            when (lunar.month to lunar.day) {
                1 to 1 -> return PublicHoliday(date, "春节")
                1 to 2 -> return PublicHoliday(date, "春节")
                1 to 3 -> return PublicHoliday(date, "春节")
                1 to 15 -> return PublicHoliday(date, "元宵节")
                5 to 5 -> return PublicHoliday(date, "端午节")
                8 to 15 -> return PublicHoliday(date, "中秋节")
            }
        }
        return null
    }

    private fun getUsUkHoliday(date: LocalDate): PublicHoliday? {
        return when (date.monthValue to date.dayOfMonth) {
            1 to 1 -> PublicHoliday(date, "New Year's Day")
            2 to 14 -> PublicHoliday(date, "Valentine's Day")
            7 to 4 -> PublicHoliday(date, "Independence Day")
            10 to 31 -> PublicHoliday(date, "Halloween")
            11 to 11 -> PublicHoliday(date, "Veterans Day")
            12 to 25 -> PublicHoliday(date, "Christmas Day")
            12 to 26 -> PublicHoliday(date, "Boxing Day")
            else -> null
        }
    }
}
