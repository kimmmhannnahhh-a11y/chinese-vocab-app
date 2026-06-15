package com.example.chineselock.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.network.ExampleSentence
import com.example.chineselock.ui.theme.AppColors

@Composable
fun HomeScreen(
    onAddVocab: () -> Unit,
    onAddDialogue: () -> Unit,
    onSearchClick: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val today by vm.todayWord.collectAsStateWithLifecycle()
    val todayTalk by vm.todayTalk.collectAsStateWithLifecycle()
    val talkLoading by vm.talkLoading.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "오늘의 학습",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            TodayCard(today, todayTalk, talkLoading, onSpeak = vm::speak)
            Spacer(Modifier.height(16.dp))
            MenuRow(Icons.Filled.PhotoCamera, "단어 추가", onAddVocab)
            Spacer(Modifier.height(12.dp))
            MenuRow(Icons.Filled.PhotoCamera, "회화 추가", onAddDialogue)
            Spacer(Modifier.height(12.dp))
            MenuRow(Icons.Filled.Search, "단어찾기 · 품사별", onSearchClick)
        }
    }
}

@Composable
private fun TodayCard(word: Vocab?, talk: ExampleSentence?, talkLoading: Boolean, onSpeak: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4A3A6E), Color(0xFF2C2048)))),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("오늘의 단어", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            Text(
                word?.hanzi ?: "—",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Text(
                word?.let { "${it.pinyin} · ${it.meaning}" } ?: "단어를 추가하면 매일 보여드려요",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
            )

            if (word != null && (talk != null || talkLoading)) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.18f)))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "오늘의 회화",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (talk != null) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = { onSpeak(talk.chinese) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "발음 듣기",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (talk != null) {
                    Text(talk.chinese, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    talk.pinyin.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    talk.korean.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                } else {
                    Text("예문 만드는 중…", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.PurpleSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, label, tint = AppColors.Purple, modifier = Modifier.size(17.dp)) }
            Text(label, Modifier.padding(start = 11.dp).weight(1f), fontSize = 13.5.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Faint)
        }
    }
}
