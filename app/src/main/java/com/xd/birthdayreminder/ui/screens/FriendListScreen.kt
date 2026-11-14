package com.xd.birthdayreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.birthdayreminder.data.BirthdayMath
import com.xd.birthdayreminder.data.Friend
import com.xd.birthdayreminder.ui.AppViewModel
import java.time.LocalDate

/**
 * 首页：好友列表，离生日最近的排在最上面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(
    vm: AppViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 有提示消息就弹一条，弹完通知 ViewModel 清掉
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val friends = vm.friends
    val today = remember(friends) { LocalDate.now() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("生日提醒", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("添加好友") },
            )
        },
    ) { innerPadding ->

        if (friends.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onAdd = onAdd,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    // 底部多留 100dp，避免最后一条被悬浮按钮挡住
                    bottom = innerPadding.calculateBottomPadding() + 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(friends, key = { it.id }) { friend ->
                    FriendCard(
                        friend = friend,
                        days = BirthdayMath.daysUntil(friend.month, friend.day, today),
                        onClick = { onOpen(friend.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Friend, days: Int, onClick: () -> Unit) {
    val soon = days <= 7
    val container = if (soon) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (soon) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像：用名字第一个字 + 固定配色，不联网也能看
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor(friend.name)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.name.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle(friend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))
            CountdownBadge(days)
        }
    }
}

/** 副标题：3月18日 · 24 岁 · 篮球 摄影 */
private fun subtitle(friend: Friend): String = buildString {
    append(BirthdayMath.formatMonthDay(friend.month, friend.day))
    BirthdayMath.turningAge(friend.birthYear, friend.month, friend.day)?.let { append(" · $it 岁") }
    if (friend.hobbies.isNotEmpty()) append(" · " + friend.hobbies.joinToString(" "))
}

@Composable
private fun CountdownBadge(days: Int) {
    val background: Color
    val foreground: Color
    when {
        days == 0 -> {
            background = MaterialTheme.colorScheme.primary
            foreground = MaterialTheme.colorScheme.onPrimary
        }
        days <= 7 -> {
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            foreground = MaterialTheme.colorScheme.primary
        }
        else -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            foreground = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(
            text = BirthdayMath.countdownText(days),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎂", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "还没有好友",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "添加第一个好友，\nApp 会自动把生日写进你的系统日历，\n提前一周提醒你买礼物。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("添加好友") },
        )
    }
}

/** 根据名字固定挑一个颜色，同一个人永远是同一个色 */
private fun avatarColor(name: String): Color {
    val palette = listOf(
        Color(0xFFE8623C),
        Color(0xFF7B8CDE),
        Color(0xFF56A08A),
        Color(0xFFD98E3A),
        Color(0xFF9C6ADE),
        Color(0xFF4A9FBF),
    )
    val index = (name.sumOf { it.code } % palette.size)
    return palette[index]
}
