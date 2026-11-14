package com.xd.birthdayreminder.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 一个好友的全部信息。
 *
 * 为什么生日只存「月」和「日」两��整数？
 * 因为生日每年都过一次，存成完整日期反而每次都要换算。
 * birthYear 是可选的——很多人只知道朋友几月几号生日，不知道哪年生。
 *
 * calendarType 现在恒为 "SOLAR"，是给以后支持农历预留的字段，改数据不用重建。
 * calendarEventIds 记录我们往系统日历里写过的三个日程 id，改动好友时要先把旧的删掉。
 */
data class Friend(
    val id: Long,
    val name: String,
    val month: Int,                      // 1 - 12
    val day: Int,                        // 1 - 31
    val birthYear: Int? = null,          // 为空表示只知道月日，不知道年份
    val hobbies: List<String> = emptyList(),
    val note: String = "",
    val calendarType: String = "SOLAR",  // SOLAR = 公历，LUNAR = 农历（预留）
    val calendarEventIds: List<Long> = emptyList(),
) {
    /** 转成 JSON 存到手机文件里 */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("month", month)
        put("day", day)
        birthYear?.let { put("birthYear", it) }
        put("hobbies", JSONArray(hobbies))
        put("note", note)
        put("calendarType", calendarType)
        put("eventIds", JSONArray(calendarEventIds))
    }

    companion object {
        /** 从文件里读回来 */
        fun fromJson(o: JSONObject): Friend = Friend(
            id = o.getLong("id"),
            name = o.getString("name"),
            month = o.getInt("month"),
            day = o.getInt("day"),
            birthYear = if (o.has("birthYear")) o.getInt("birthYear") else null,
            hobbies = o.optJSONArray("hobbies").toStringList(),
            note = o.optString("note", ""),
            calendarType = o.optString("calendarType", "SOLAR"),
            calendarEventIds = o.optJSONArray("eventIds").toLongList(),
        )

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return (0 until length()).map { getString(it) }
        }

        private fun JSONArray?.toLongList(): List<Long> {
            if (this == null) return emptyList()
            return (0 until length()).map { getLong(it) }
        }
    }
}
