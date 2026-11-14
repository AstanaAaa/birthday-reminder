package com.xd.birthdayreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xd.birthdayreminder.ui.AppViewModel
import com.xd.birthdayreminder.ui.BirthdayTheme
import com.xd.birthdayreminder.ui.screens.AppRoot

/**
 * App 的唯一入口。整个界面用 Compose 写，所以这里只有几行。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BirthdayTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}
