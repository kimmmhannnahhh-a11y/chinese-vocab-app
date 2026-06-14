package com.example.chineselock.feature.home

import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.ui.theme.AppColors
import java.util.Locale

@Composable
fun HomeScreen(
    onAddVocab: () -> Unit,
    onAddDialogue: () -> Unit,
    onSearchClick: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val today by vm.todayWord.collectAsStateWithLifecycle()
    val todayDialogue by vm.todayDialogue.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 중국어 발음 듣기용 TTS. 화면을 떠날 때 정리.
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    val speak: (String) -> Unit = speak@{ text ->
        val e = tts ?: return@speak
        val r = e.setLanguage(Locale.CHINESE)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "기기에 중국어 음성(TTS)이 없어요. 설정 > 언어 > 음성에서 중국어를 설치해주세요.", Toast.LENGTH_LONG).show()
            return@speak
        }
        e.setSpeechRate(0.9f)
        e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "today")
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "오늘의 학습",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            TodayCard(today, todayDialogue, onSpeak = speak)
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
private fun TodayCard(word: Vocab?, dialogue: Dialogue?, onSpeak: (String) -> Unit) {
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

            if (dialogue != null) {
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
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { onSpeak(dialogue.chinese) }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "발음 듣기",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(dialogue.chinese, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                dialogue.pinyin?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                dialogue.korean?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
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
