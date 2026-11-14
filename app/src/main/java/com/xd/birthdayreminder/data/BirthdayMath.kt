package com.xd.birthdayreminder.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 生日的日期计算。所有「还有几天」「下次是哪天」都从这里出，界面不自己算。
 */
object BirthdayMath {

    /**
     * 算出「下一次」生日是哪天。
     * 如果今年的生日还没到或就是今天，返回今年；已经过了就返回明年。
     */
    fun nextOccurrence(month: Int, day: Int, today: LocalDate = LocalDate.now()): LocalDate {
        var d = safeDate(today.year, month, day)
        if (d.isBefore(today)) d = safeDate(today.year + 1, month, day)
        return d
    }

    /** 还有几天过生日。今天就是生日返回 0 */
    fun daysUntil(month: Int, day: Int, today: LocalDate = LocalDate.now()): Int =
        ChronoUnit.DAYS.between(today, nextOccurrence(month, day, today)).toInt()

    /** 今年是第几个生日。不知道出生年份就返回 null */
    fun turningAge(birthYear: Int?, month: Int, day: Int, today: LocalDate = LocalDate.now()): Int? {
        if (birthYear == null) return null
        return nextOccurrence(month, day, today).year - birthYear
    }

    /**
     * 2 月 29 日出生的人在平年没有生日，
     * YearMonth 会告诉我们这年 2 月只有 28 天，于是落到 2 月 28 日。
     */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
        val maxDay = YearMonth.of(year, month).lengthOfMonth()
        return LocalDate.of(year, month, minOf(day, maxDay))
    }

    /** "3月18日" */
    fun formatMonthDay(month: Int, day: Int): String = "${month}月${day}日"

    /** 界面上那句倒计时文案 */
    fun countdownText(days: Int): String = when (days) {
        0 -> "就是今天 🎉"
        1 -> "明天生日"
        2 -> "后天生日"
        else -> "还有 $days 天"
    }
}
