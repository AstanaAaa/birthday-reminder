package com.xd.birthdayreminder.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.xd.birthdayreminder.calendar.CalendarSync
import com.xd.birthdayreminder.data.Friend
import com.xd.birthdayreminder.ui.AppViewModel

/**
 * 页面跳转全靠这个。没有引入第三方导航库，因为就三个页面，
 * 一个状态变量就够了，你也能一眼看懂整个流程。
 */
sealed class Screen {
    data object Home : Screen()
    data class Edit(val friend: Friend?) : Screen()
    data class Detail(val friendId: Long) : Screen()
}

@Composable
fun AppRoot(vm: AppViewModel) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // 系统返回键：回到首页，而不是直接退出 App
    BackHandler(enabled = screen !is Screen.Home) { screen = Screen.Home }

    // 权限弹窗需要一个地方暂存「正在等权限的那个好友」
    var pendingFriend by remember { mutableStateOf<Friend?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.WRITE_CALENDAR] == true
        pendingFriend?.let { f ->
            // 没给权限也照样存好友，只是不写日历
            vm.save(f, granted)
            if (!granted) vm.consumeMessage()
        }
        pendingFriend = null
        screen = Screen.Home
    }

    fun saveWithCalendar(friend: Friend) {
        pendingFriend = friend
        permissionLauncher.launch(
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
        )
    }

    val context = LocalContext.current

    when (val s = screen) {
        Screen.Home -> FriendListScreen(
            vm = vm,
            onAdd = { screen = Screen.Edit(null) },
            onOpen = { id -> screen = Screen.Detail(id) },
        )

        is Screen.Edit -> EditFriendScreen(
            original = s.friend,
            onCancel = { screen = Screen.Home },
            onSave = { friend ->
                if (CalendarSync.hasPermission(context)) {
                    vm.save(friend, true)
                    screen = Screen.Home
                } else {
                    saveWithCalendar(friend)
                }
            },
        )

        is Screen.Detail -> {
            val friend = vm.find(s.friendId)
            if (friend == null) {
                // 好友被删掉了，退回首页
                LaunchedEffect(s.friendId) { screen = Screen.Home }
            } else {
                FriendDetailScreen(
                    friend = friend,
                    onBack = { screen = Screen.Home },
                    onEdit = { screen = Screen.Edit(friend) },
                    onResync = { vm.resync(friend) },
                    onDelete = {
                        vm.delete(friend)
                        screen = Screen.Home
                    },
                )
            }
        }
    }
}
