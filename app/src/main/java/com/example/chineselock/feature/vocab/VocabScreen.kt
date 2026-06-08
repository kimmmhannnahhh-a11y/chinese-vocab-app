package com.example.chineselock.feature.vocab

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.ui.PosBoxes
import com.example.chineselock.ui.StudyMode
import com.example.chineselock.ui.VocabListRow
import com.example.chineselock.ui.theme.AppColors

@Composable
fun VocabScreen(vm: VocabViewModel = hiltViewModel()) {
    val units by vm.units.collectAsStateWithLifecycle()
    val selectedId by vm.selectedUnitId.collectAsStateWithLifecycle()
    val vocab by vm.vocab.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val editMode by vm.editMode.collectAsStateWithLifecycle()
    val revealed by vm.revealed.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    val title = units.firstOrNull { it.id == selectedId }?.title ?: "—"

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            // 헤더: 제목 + 단원 드롭다운 + 편집
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("단어장", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                UnitDropdown(title, units.map { it.id to it.title }, onSelect = vm::selectUnit)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = vm::toggleEdit) {
                    Icon(Icons.Filled.Edit, "수정", tint = if (editMode) AppColors.Purple else AppColors.Sub)
                }
            }

            if (!editMode) {
                ModeSegment(mode, vm::setMode)
                Spacer(Modifier.height(12.dp))
            }

            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(vocab, key = { it.id }) { v ->
                    if (editMode) {
                        VocabEditRow(v, onDelete = { vm.delete(v) }, onSpeak = { vm.speak(v.hanzi) })
                    } else {
                        VocabListRow(
                            hanzi = v.hanzi, pinyin = v.pinyin, partOfSpeech = v.partOfSpeech, meaning = v.meaning,
                            isFavorite = v.isFavorite, mode = mode, revealed = v.id in revealed,
                            onReveal = { vm.reveal(v.id) }, onSpeak = { vm.speak(v.hanzi) },
                            onToggleFav = { vm.toggleFavorite(v) },
                        )
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 4.dp).background(AppColors.Line).height(1.dp))
                }
                if (editMode) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AppColors.Faint, RoundedCornerShape(12.dp))
                                .clickable { showAdd = true }
                                .padding(11.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, null, tint = AppColors.Purple, modifier = Modifier.size(15.dp))
                            Text("  단어 추가", color = AppColors.Purple, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddVocabDialog(
            onDismiss = { showAdd = false },
            onAdd = { h, p, pos, m -> vm.addVocab(h, p, pos, m); showAdd = false },
        )
    }
}

@Composable
private fun UnitDropdown(title: String, options: List<Pair<Long, String>>, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(11.dp))
                .border(1.dp, AppColors.Muted, RoundedCornerShape(11.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = AppColors.Purple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Muted, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, t) ->
                DropdownMenuItem(text = { Text(t) }, onClick = { onSelect(id); open = false })
            }
        }
    }
}

@Composable
private fun ModeSegment(mode: StudyMode, onSelect: (StudyMode) -> Unit) {
    val items = listOf(StudyMode.ALL to "전체", StudyMode.HANZI_ONLY to "한자만", StudyMode.MEANING_ONLY to "뜻만")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppColors.Line).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (m, label) ->
            val on = m == mode
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (on) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) AppColors.Purple else AppColors.Sub, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VocabEditRow(v: Vocab, onDelete: () -> Unit, onSpeak: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(v.hanzi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(52.dp))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PosBoxes(v.partOfSpeech)
                Text(v.meaning, fontSize = 13.sp)
            }
            Text(v.pinyin, color = AppColors.Sub, fontSize = 11.sp)
        }
        IconButton(onClick = onDelete) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(androidx.compose.ui.graphics.Color(0xFFFBE0E6)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.DeleteOutline, "삭제", tint = androidx.compose.ui.graphics.Color(0xFFC24062), modifier = Modifier.size(15.dp)) }
        }
    }
}

@Composable
private fun AddVocabDialog(onDismiss: () -> Unit, onAdd: (String, String, List<String>, String) -> Unit) {
    var hanzi by remember { mutableStateOf("") }
    var pinyin by remember { mutableStateOf("") }
    var pos by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("단어 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(hanzi, { hanzi = it }, label = { Text("한자") }, singleLine = true)
                OutlinedTextField(pinyin, { pinyin = it }, label = { Text("병음") }, singleLine = true)
                OutlinedTextField(pos, { pos = it }, label = { Text("품사 (예: 명사·양사)") }, singleLine = true)
                OutlinedTextField(meaning, { meaning = it }, label = { Text("뜻") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(hanzi.trim(), pinyin.trim(), pos.split("·").map { it.trim() }.filter { it.isNotEmpty() }, meaning.trim()) },
                enabled = hanzi.isNotBlank() && meaning.isNotBlank(),
            ) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
