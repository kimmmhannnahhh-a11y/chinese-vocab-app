package com.example.chineselock.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chineselock.ui.StudyMode
import com.example.chineselock.ui.VocabListRow
import com.example.chineselock.ui.theme.AppColors

@Composable
fun FavoritesScreen(vm: FavoritesViewModel = hiltViewModel()) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val revealed by vm.revealed.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("즐겨찾기", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))

            ModeSegment(mode, vm::setMode)
            Spacer(Modifier.height(12.dp))

            if (favorites.isEmpty()) {
                Text("★ 표시한 단어가 여기 모입니다", color = AppColors.Muted, fontSize = 13.sp)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(favorites, key = { it.id }) { v ->
                    VocabListRow(
                        hanzi = v.hanzi, pinyin = v.pinyin, partOfSpeech = v.partOfSpeech, meaning = v.meaning,
                        isFavorite = v.isFavorite, mode = mode, revealed = v.id in revealed,
                        onReveal = { vm.reveal(v.id) }, onSpeak = { vm.speak(v.hanzi) },
                        onToggleFav = { vm.toggleFavorite(v) },
                    )
                    Box(Modifier.fillMaxWidth().background(AppColors.Line).height(1.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeSegment(mode: StudyMode, onSelect: (StudyMode) -> Unit) {
    val items = listOf(StudyMode.ALL to "전체", StudyMode.HANZI_ONLY to "한자만", StudyMode.MEANING_ONLY to "뜻만")
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppColors.Line).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (m, label) ->
            val on = m == mode
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onSelect(m) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) AppColors.Purple else AppColors.Sub, fontSize = 12.sp) }
        }
    }
}
