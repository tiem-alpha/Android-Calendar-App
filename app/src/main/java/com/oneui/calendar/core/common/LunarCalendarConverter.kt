package com.oneui.calendar.core.common

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

data class LunarDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeapMonth: Boolean = false
)

/**
 * Offline Astronomical Lunar Calendar Converter using the Ho Ngoc Duc / Jean Meeus algorithm.
 */
object LunarCalendarConverter {

    private const val SYNODIC_MONTH = 29.530588853
    private const val EPOCH_JD = 2415021.076998695

    private val MONTH_SHORT_NAMES = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun solarToLunar(date: LocalDate, timeZoneOffsetHours: Double = 7.0): LunarDate {
        val dayNumber = jdFromDate(date.dayOfMonth, date.monthValue, date.year)
        val k = floor((dayNumber - EPOCH_JD) / SYNODIC_MONTH).toInt()
        var monthStart = getNewMoonDay(k + 1, timeZoneOffsetHours)
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZoneOffsetHours)
        }
        var a11 = getLunarMonth11(date.year, timeZoneOffsetHours)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = date.year
            a11 = getLunarMonth11(date.year - 1, timeZoneOffsetHours)
        } else {
            lunarYear = date.year + 1
            b11 = getLunarMonth11(date.year + 1, timeZoneOffsetHours)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var lunarLeap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, timeZoneOffsetHours)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    lunarLeap = true
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth -= 12
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }
        return LunarDate(
            day = lunarDay,
            month = lunarMonth,
            year = lunarYear,
            isLeapMonth = lunarLeap
        )
    }

    fun lunarToSolar(
        lunarDay: Int,
        lunarMonth: Int,
        lunarYear: Int,
        isLeapMonth: Boolean = false,
        timeZoneOffsetHours: Double = 7.0
    ): LocalDate {
        val a11: Int
        val b11: Int
        if (lunarMonth < 11) {
            a11 = getLunarMonth11(lunarYear - 1, timeZoneOffsetHours)
            b11 = getLunarMonth11(lunarYear, timeZoneOffsetHours)
        } else {
            a11 = getLunarMonth11(lunarYear, timeZoneOffsetHours)
            b11 = getLunarMonth11(lunarYear + 1, timeZoneOffsetHours)
        }
        val k = floor(0.5 + (a11 - EPOCH_JD) / SYNODIC_MONTH).toInt()
        var off = lunarMonth - 11
        if (off < 0) {
            off += 12
        }
        if (b11 - a11 > 365) {
            val leapOff = getLeapMonthOffset(a11, timeZoneOffsetHours)
            var leapMonth = leapOff - 2
            if (leapMonth < 0) {
                leapMonth += 12
            }
            if (isLeapMonth && lunarMonth == leapMonth) {
                off += 1
            } else if (!isLeapMonth && off >= leapOff) {
                off += 1
            }
        }
        val monthStart = getNewMoonDay(k + off, timeZoneOffsetHours)
        val nextMonthStart = getNewMoonDay(k + off + 1, timeZoneOffsetHours)
        val maxDayInMonth = (nextMonthStart - monthStart).coerceAtLeast(29)
        val clampedDay = lunarDay.coerceIn(1, maxDayInMonth)
        return jdToDate(monthStart + clampedDay - 1)
    }

    fun formatShortLunarSubDate(solarDate: LocalDate): String {
        val lunar = solarToLunar(solarDate)
        val monthIndex = (lunar.month - 1).coerceIn(0, 11)
        return "${lunar.day} ${MONTH_SHORT_NAMES[monthIndex]}"
    }

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        }
        return jd
    }

    private fun jdToDate(jd: Int): LocalDate {
        val a: Int
        val b: Int
        val c: Int
        if (jd > 2299160) {
            a = jd + 32044
            b = (4 * a + 3) / 146097
            c = a - (146097 * b) / 4
        } else {
            b = 0
            c = jd + 32082
        }
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = b * 100 + d - 4800 + m / 10
        return LocalDate.of(year, month, day)
    }

    private fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180.0
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 -= 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 += 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 -= 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 -= 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 += 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val deltat = if (t < -11.0) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltat
    }

    private fun getNewMoonDay(k: Int, timeZone: Double): Int {
        return floor(newMoon(k) + 0.5 + timeZone / 24.0).toInt()
    }

    private fun sunLongitude(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525.0
        val t2 = t * t
        val dr = PI / 180.0
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        val dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m) +
            (0.019993 - 0.000101 * t) * sin(dr * 2 * m) +
            0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2.0 * floor(l / (PI * 2.0))
        return l
    }

    private fun getSunLongitude(dayNumber: Int, timeZone: Double): Int {
        return floor(sunLongitude(dayNumber - 0.5 - timeZone / 24.0) / PI * 6.0).toInt()
    }

    private fun getLunarMonth11(yy: Int, timeZone: Double): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / SYNODIC_MONTH).toInt()
        var nm = getNewMoonDay(k, timeZone)
        val sunLong = getSunLongitude(nm, timeZone)
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone)
        }
        return nm
    }

    private fun getLeapMonthOffset(a11: Int, timeZone: Double): Int {
        val k = floor((a11 - EPOCH_JD) / SYNODIC_MONTH + 0.5).toInt()
        var last: Int
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        } while (arc != last && i < 14)
        return i - 1
    }
}
