package com.xd.birthdayreminder.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xd.birthdayreminder.calendar.CalendarSync
import com.xd.birthdayreminder.data.BirthdayMath
import com.xd.birthdayreminder.data.Friend
import com.xd.birthdayreminder.data.FriendRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 整个 App 的「大脑」。
 *
 * 界面只负责画，所有数据和业务逻辑都在这里：
 * 谁存在哪个文件里、排序怎么排、日历什么时候写。
 * 好处是：以后你想换界面（比如做成 iPhone 版），这套逻辑不用动。
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FriendRepository(app)

    /** 好友列表，按离生日的远近排好序 */
    var friends by mutableStateOf<List<Friend>>(emptyList())
        private set

    /** 数据读完没有，用来避免界面闪一下空白 */
    var ready by mutableStateOf(false)
        private set

    /** 一次性提示消息，界面弹完就清掉 */
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            friends = sortByUpcoming(repo.load())
            ready = true
        }
    }

    fun find(id: Long): Friend? = friends.firstOrNull { it.id == id }

    /**
     * 保存好友。
     * syncCalendar = true 时会把三个提醒日程写进系统日历；
     * 如果日历写失败（比如没权限、手机没有日历账户），好友信息照样存下来，只是提示你。
     */
    fun save(friend: Friend, syncCalendar: Boolean) {
        viewModelScope.launch {
            var saved = friend
            if (syncCalendar) {
                saved = runCatching { writeCalendar(friend) }.getOrElse { error ->
                    message = "日历写入失败：${error.message}"
                    friend
                }
            }
            friends = sortByUpcoming(friends.filterNot { it.id == saved.id } + saved)
            repo.save(friends)
            if (syncCalendar && saved.calendarEventIds.isNotEmpty()) {
                message = "已写入日历：${saved.calendarEventIds.size} 个提醒"
            }
        }
    }

    /** 只对这个好友重写日历（比如你换了手机、手动删了日程） */
    fun resync(friend: Friend) {
        viewModelScope.launch {
            val updated = runCatching { writeCalendar(friend) }.getOrElse { error ->
                message = "日历写入失败：${error.message}"
                return@launch
            }
            friends = sortByUpcoming(friends.map { if (it.id == updated.id) updated else it })
            repo.save(friends)
            message = "已重新写入 ${updated.calendarEventIds.size} 个日程"
        }
    }

    /** 删除好友，并把日历里对应的日程一起删干净 */
    fun delete(friend: Friend) {
        viewModelScope.launch {
            CalendarSync.deleteEvents(getApplication(), friend.calendarEventIds)
            friends = friends.filterNot { it.id == friend.id }
            repo.save(friends)
            message = "已删除 ${friend.name}"
        }
    }

    fun consumeMessage() {
        message = null
    }

    /** 真正调用系统日历的写入口，返回带上新日程 id 的好友 */
    private fun writeCalendar(friend: Friend): Friend {
        val ids = CalendarSync.sync(getApplication(), friend)
        return friend.copy(calendarEventIds = ids)
    }

    private fun sortByUpcoming(list: List<Friend>): List<Friend> {
        val today = LocalDate.now()
        return list.sortedBy { BirthdayMath.daysUntil(it.month, it.day, today) }
    }
}
