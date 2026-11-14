package com.xd.birthdayreminder.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.xd.birthdayreminder.data.BirthdayMath
import com.xd.birthdayreminder.data.Friend
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 把好友生日写进手机系统日历。
 *
 * 设计要点：
 * 1. 每个好友写 3 个「全天」日程：提前 7 天挑礼物、提前 3 天准备礼物、当天祝福。
 * 2. 每个日程用 RRULE=FREQ=YEARLY 设成年年重复，这样不用每年重新写一遍，
 *    就算你一年不开这个 App，提醒也一直在。
 * 3. 全天事件的起点是「UTC 零点」，所以提醒的分钟数要用「本地 9 点 - 时区偏移」换算，
 *    否则会在半夜零点弹提醒。
 */
object CalendarSync {

    /** 提醒希望在本地时间几点响 */
    private const val REMINDER_HOUR = 9

    /** 三个提醒各自提前几天 */
    private const val DAY_GIFT_SHOPPING = 7L
    private const val DAY_PREPARE_GIFT = 3L
    private const val DAY_BIRTHDAY = 0L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 找一个可以写入的日历账户。
     * 优先选系统主日历，其次选权限最高的那个。
     */
    fun findWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        var bestId: Long? = null
        var bestScore = -1
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val access = c.getInt(1)
                val primary = if (c.isNull(2)) 0 else c.getInt(2)
                // 至少要有「可添加事件」的权限
                if (access < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                val score = access + primary * 1000
                if (score > bestScore) {
                    bestScore = score
                    bestId = id
                }
            }
        }
        return bestId
    }

    /**
     * 为一个好友写入三个提醒日程，返回新日程的 id 列表。
     * 调用前先确保已有日历权限，并且会先删掉这个好友以前写过的旧日程。
     */
    fun sync(context: Context, friend: Friend): List<Long> {
        val calendarId = findWritableCalendarId(context)
            ?: throw IllegalStateException("手机上找不到可写入的日历。请先打开系统「日历」App，确认里面有日历账户。")

        // 清掉上一次写的，避免同一个人出现两套提醒
        deleteEvents(context, friend.calendarEventIds)

        val today = LocalDate.now()
        val nextBirthday = BirthdayMath.nextOccurrence(friend.month, friend.day, today)

        return buildPlans(friend).mapNotNull { plan ->
            var date = nextBirthday.minusDays(plan.offsetDays)
            // 如果这个提醒的时间点已经过去了，就顺延到明年同一天，别塞一个过去的日程进去
            if (date.isBefore(today)) date = date.plusYears(1)
            insertAllDayEvent(context, calendarId, date, plan.title, plan.description)
        }
    }

    /** 删掉指定日程（连同它的提醒） */
    fun deleteEvents(context: Context, ids: List<Long>) {
        ids.forEach { id ->
            runCatching {
                context.contentResolver.delete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                    null,
                    null,
                )
            }
        }
    }

    private data class EventPlan(val offsetDays: Long, val title: String, val description: String)

    /** 三个日程的文案。文案写得好，你收到提醒时才知道该干什么 */
    private fun buildPlans(friend: Friend): List<EventPlan> {
        val hobbyText = if (friend.hobbies.isEmpty()) {
            ""
        } else {
            "\nTA 的爱好：${friend.hobbies.joinToString("、")}"
        }
        return listOf(
            EventPlan(
                DAY_GIFT_SHOPPING,
                "给${friend.name}挑生日礼物 🎁",
                "还有 7 天就是${friend.name}的生日（${friend.month}月${friend.day}日）。" +
                    "今天把礼物挑好、下单，物流才来得及。$hobbyText",
            ),
            EventPlan(
                DAY_PREPARE_GIFT,
                "准备${friend.name}的生日礼物 🎀",
                "还有 3 天！检查${friend.name}的礼物是否到货、要不要包装，想好当天怎么送。$hobbyText",
            ),
            EventPlan(
                DAY_BIRTHDAY,
                "${friend.name} 生日快乐 🎂",
                "今天是${friend.name}的生日，记得发祝福。$hobbyText",
            ),
        )
    }

    private fun insertAllDayEvent(
        context: Context,
        calendarId: Long,
        date: LocalDate,
        title: String,
        description: String,
    ): Long? {
        val startMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            // 全天事件的时间基准必须是 UTC
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            put(CalendarContract.Events.ALL_DAY, 1)
            // 每年重复
            put(CalendarContract.Events.RRULE, "FREQ=YEARLY")
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        val uri = runCatching {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        }.getOrNull() ?: return null

        val eventId = ContentUris.parseId(uri)
        insertReminder(context, eventId)
        return eventId
    }

    /**
     * 给日程加闹钟。
     *
     * 全天事件的 DTSTART 固定是「UTC 零点」，而提醒的 MINUTES 语义是「事件开始前多少分钟」。
     * 想让提醒落在本地 9:00，要先算本地 9:00 距离 UTC 零点多少分钟：
     *
     *   北京 UTC+8：本地 9:00 = UTC 01:00，比 UTC 零点「晚」60 分钟
     *   → MINUTES 必须写 -60（负数 = 事件开始之后），而不是 +60
     *
     * 写成 +60 的话，提醒会落在 UTC 前一天 23:00 = 北京前一天早上 7:00，整整差了一天。
     */
    private fun insertReminder(context: Context, eventId: Long) {
        val offsetMinutes = ZoneId.systemDefault().rules
            .getOffset(Instant.now()).totalSeconds / 60
        val minutesAfterUtcMidnight = REMINDER_HOUR * 60 - offsetMinutes
        val minutes = -minutesAfterUtcMidnight

        // 极少数机型的日历不接受负值（"开始之后"的提醒），退回到 UTC 零点（北京时间 8:00）
        if (!insertReminderRow(context, eventId, minutes)) {
            insertReminderRow(context, eventId, 0)
        }
    }

    private fun insertReminderRow(context: Context, eventId: Long, minutes: Int): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minutes)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        return runCatching {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values) != null
        }.getOrDefault(false)
    }
}
