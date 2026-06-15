package com.example.chineselock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chineselock.ui.theme.AppColors

/** 교재식 품사 박스 (작은 색 박스 + 한 글자 약자). 다중 품사면 여러 개. */
@Composable
fun PosBoxes(partOfSpeech: String?) {
    if (partOfSpeech.isNullOrBlank()) return
    Row {
        partOfSpeech.split("·").filter { it.isNotBlank() }.forEach { pos ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.posBg(pos))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(AppColors.posAbbr(pos), color = AppColors.posFg(pos), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StarButton(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(30.dp)) {
        if (isFavorite) {
            Icon(Icons.Filled.Star, "즐겨찾기 해제", tint = AppColors.Gold, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Outlined.StarBorder, "즐겨찾기", tint = AppColors.Faint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SpeakButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            "발음 듣기",
            tint = AppColors.Purple,
            modifier = Modifier.size(16.dp),
        )
    }
}

enum class StudyMode { ALL, HANZI_ONLY, MEANING_ONLY }

/** 단어 한 줄: 한자 · 품사 · 뜻 · 병음 + 듣기 + 즐겨찾기. 가리기 모드 지원. */
@Composable
fun VocabListRow(
    hanzi: String,
    pinyin: String,
    partOfSpeech: String?,
    meaning: String,
    isFavorite: Boolean,
    mode: StudyMode = StudyMode.ALL,
    revealed: Boolean = false,
    onReveal: () -> Unit = {},
    onSpeak: () -> Unit,
    onToggleFav: () -> Unit,
) {
    val showHanzi = mode != StudyMode.MEANING_ONLY || revealed
    val showMeaning = mode != StudyMode.HANZI_ONLY || revealed
    val showPinyin = mode == StudyMode.ALL || revealed
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = mode != StudyMode.ALL && !revealed) { onReveal() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(52.dp)) {
            if (showHanzi) Text(hanzi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            else Text("탭", color = AppColors.Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            if (showMeaning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PosBoxes(partOfSpeech)
                    Text(meaning, fontSize = 13.sp)
                }
            } else {
                Text("탭하면 공개", color = AppColors.Muted, fontSize = 11.sp)
            }
            if (showPinyin) Text(pinyin, color = AppColors.Sub, fontSize = 11.sp)
        }
        SpeakButton(onSpeak)
        StarButton(isFavorite, onToggleFav)
    }
}

/** 회화 화자 A/B 태그. */
@Composable
fun SpeakerTag(speaker: String) {
    val (bg, fg) = if (speaker.equals("A", true)) AppColors.SpeakerASoft to AppColors.SpeakerA
    else AppColors.SpeakerBSoft to AppColors.SpeakerB
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(speaker, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
