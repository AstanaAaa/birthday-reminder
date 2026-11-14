package com.xd.birthdayreminder.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * 好友数据的存取。
 *
 * 为什么不用数据库（Room/SQLite）？
 * 因为好友数量最多几十上百条，一个 JSON 文件足够快，而且你能直接打开看内容。
 * 等以后要做搜索、排序、多设备同步再换数据库，那时只需要改这一个类。
 */
class FriendRepository(context: Context) {

    private val file = File(context.filesDir, "friends.json")

    /** 读取全部好友 */
    suspend fun load(): List<Friend> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { Friend.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /**
     * 保存全部好友。
     * 先写临时文件再覆盖，避免写到一半断电把数据写坏。
     */
    suspend fun save(friends: List<Friend>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        friends.forEach { arr.put(it.toJson()) }
        val tmp = File(file.parentFile, "friends.json.tmp")
        tmp.writeText(arr.toString())
        tmp.copyTo(file, overwrite = true)
        tmp.delete()
    }
}
