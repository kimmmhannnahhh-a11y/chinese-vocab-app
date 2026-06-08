package com.example.chineselock.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.example.chineselock.ui.VocabListRow
import com.example.chineselock.ui.theme.AppColors

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WordSearchScreen(onBack: () -> Unit, vm: WordSearchViewModel = hiltViewModel()) {
    val query by vm.query.collectAsStateWithLifecycle()
    val selectedPos by vm.selectedPos.collectAsStateWithLifecycle()
    val posCounts by vm.posCounts.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                Text("단어찾기", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            TextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("중국어·병음·한글로 검색", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = AppColors.Muted) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, AppColors.Line, RoundedCornerShape(13.dp)),
            )

            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                posCounts.forEach { pc ->
                    val on = pc.pos == selectedPos
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) AppColors.Purple else Color.White)
                            .border(1.dp, if (on) AppColors.Purple else AppColors.Line, RoundedCornerShape(9.dp))
                            .clickable { vm.selectPos(pc.pos) }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    ) {
                        Text("${pc.pos} ${pc.count}", color = if (on) Color.White else AppColors.Sub, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (query.isBlank() && selectedPos == null) {
                Text("검색어를 입력하거나 품사를 선택하세요", color = AppColors.Muted, fontSize = 13.sp)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { v ->
                    VocabListRow(
                        hanzi = v.hanzi, pinyin = v.pinyin, partOfSpeech = v.partOfSpeech, meaning = v.meaning,
                        isFavorite = v.isFavorite, onSpeak = { vm.speak(v.hanzi) }, onToggleFav = { vm.toggleFavorite(v) },
                    )
                    Box(Modifier.fillMaxWidth().background(AppColors.Line).height(1.dp))
                }
            }
        }
    }
}
