package com.xd.birthdayreminder.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xd.birthdayreminder.data.Friend
import java.time.LocalDate

/**
 * 添加 / 编辑好友。
 * original 为空表示新增，不为空表示编辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFriendScreen(
    original: Friend?,
    onCancel: () -> Unit,
    onSave: (Friend) -> Unit,
) {
    val context = LocalContext.current

    // 表单的每一项都是一个状态，改动时界面自动重画
    var name by remember { mutableStateOf(original?.name ?: "") }
    var month by remember { mutableStateOf(original?.month ?: LocalDate.now().monthValue) }
    var day by remember { mutableStateOf(original?.day ?: LocalDate.now().dayOfMonth) }
    var year by remember { mutableStateOf(original?.birthYear ?: 2000) }
    var knowsYear by remember { mutableStateOf(original?.birthYear != null) }
    var hobbiesText by remember { mutableStateOf(original?.hobbies?.joinToString(" ") ?: "") }
    var note by remember { mutableStateOf(original?.note ?: "") }

    // 用系统自带的日期选择器，不自己画轮盘，省事又符合安卓习惯
    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, pickedYear, pickedMonth, pickedDay ->
                year = pickedYear
                month = pickedMonth + 1   // 系统给的是 0-11
                day = pickedDay
            },
            year,
            month - 1,
            day,
        ).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "添加好友" else "编辑好友", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名字") },
                placeholder = { Text("比如：王小明") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 生日选择
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = ::openDatePicker),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("生日", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${month}月${day}日",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 知不知道出生年份
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("记录出生年份", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "知道的话可以显示今年过几岁生日",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = knowsYear, onCheckedChange = { knowsYear = it })
            }

            OutlinedTextField(
                value = hobbiesText,
                onValueChange = { hobbiesText = it },
                label = { Text("爱好（选填）") },
                placeholder = { Text("篮球 摄影 咖啡，用空格或逗号分隔") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（选填）") },
                placeholder = { Text("比如：她喜欢猫，别送花") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val friend = Friend(
                        id = original?.id ?: System.currentTimeMillis(),
                        name = name.trim(),
                        month = month,
                        day = day,
                        birthYear = if (knowsYear) year else null,
                        hobbies = parseHobbies(hobbiesText),
                        note = note.trim(),
                        calendarType = "SOLAR",
                        calendarEventIds = original?.calendarEventIds ?: emptyList(),
                    )
                    onSave(friend)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("保存并写入日历", style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "保存后，手机日历里会出现 3 个日程：提前 7 天买礼物、提前 3 天准备礼物、生日当天送祝福。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** 把 "篮球 摄影，咖啡、读书" 拆成标签列表 */
private fun parseHobbies(raw: String): List<String> =
    raw.split(' ', ',', '，', '、', ';', '；')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
