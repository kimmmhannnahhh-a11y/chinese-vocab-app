package com.example.chineselock.feature.conversation

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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.ui.SpeakButton
import com.example.chineselock.ui.SpeakerTag
import com.example.chineselock.ui.theme.AppColors

@Composable
fun ConversationScreen(
    onCaptureTranslation: (Long) -> Unit = {},
    vm: ConversationViewModel = hiltViewModel(),
) {
    val units by vm.units.collectAsStateWithLifecycle()
    val selectedId by vm.selectedUnitId.collectAsStateWithLifecycle()
    val turns by vm.turns.collectAsStateWithLifecycle()
    val showTranslation by vm.showTranslation.collectAsStateWithLifecycle()
    val editMode by vm.editMode.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val title = units.firstOrNull { it.id == selectedId }?.title ?: "—"
    val sectionTitle = turns.firstOrNull()?.sectionTitle

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("회화 배우기", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                UnitDropdown(title, units.map { it.id to it.title }, vm::selectUnit)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = vm::toggleEdit) {
                    Icon(Icons.Filled.Edit, "수정", tint = if (editMode) AppColors.Purple else AppColors.Sub)
                }
            }
            if (!sectionTitle.isNullOrBlank()) {
                Text(
                    sectionTitle,
                    color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            if (!editMode) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LangToggle(showTranslation, vm::setTranslation)
                    Spacer(Modifier.weight(1f))
                    PlayAllButton(vm::playAll)
                }
                Spacer(Modifier.height(6.dp))
                if (showTranslation && turns.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, AppColors.Faint, RoundedCornerShape(12.dp))
                            .clickable { selectedId?.let(onCaptureTranslation) }
                            .padding(11.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = AppColors.Purple, modifier = Modifier.size(15.dp))
                        Text("  번역 페이지 촬영해서 채우기", color = AppColors.Purple, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                Text("문장을 삭제하거나 새 문장을 추가하세요", color = AppColors.Sub, fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 6.dp))
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(turns, key = { it.id }) { t ->
                    TurnRow(t, showTranslation, editMode, onSpeak = { vm.speak(t.chinese) }, onDelete = { vm.delete(t) })
                    Box(Modifier.fillMaxWidth().background(AppColors.Line).height(1.dp))
                }
                if (editMode) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AppColors.Faint, RoundedCornerShape(12.dp))
                                .clickable { showAdd = true }.padding(11.dp),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, null, tint = AppColors.Purple, modifier = Modifier.size(15.dp))
                            Text("  문장 추가", color = AppColors.Purple, fontSize = 13.sp)
                        }
                    }
                    if (selectedId != null) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, AppColors.Faint, RoundedCornerShape(12.dp))
                                    .clickable { showRename = true }.padding(11.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Edit, null, tint = AppColors.Purple, modifier = Modifier.size(15.dp))
                                Text("  제목(단원) 수정", color = AppColors.Purple, fontSize = 13.sp)
                            }
                        }
                    }
                    if (selectedId != null && turns.isNotEmpty()) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { showDeleteAll = true }.padding(11.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.DeleteOutline, null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Text("  $title 회화 전체 삭제", color = DangerRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddLineDialog(onDismiss = { showAdd = false }) { sp, zh, pin, ko ->
            vm.addLine(sp, zh, pin, ko); showAdd = false
        }
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("$title 회화 전체 삭제") },
            text = { Text("이 단원의 회화가 모두 삭제돼요. (단어는 유지됩니다) 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteAllDialogues(); showDeleteAll = false }) {
                    Text("회화 전체 삭제", color = DangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("취소") } },
        )
    }

    if (showRename) {
        var text by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("제목(단원) 수정") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    placeholder = { Text("예: 3-1", color = AppColors.Muted) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.renameCurrentUnit(text.trim()); showRename = false },
                    enabled = text.isNotBlank(),
                ) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("취소") } },
        )
    }
}

private val DangerRed = Color(0xFFD14D4D)

@Composable
private fun TurnRow(t: Dialogue, showTranslation: Boolean, editMode: Boolean, onSpeak: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
        if (t.speaker != null) {
            SpeakerTag(t.speaker)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            if (showTranslation) {
                Text(t.korean ?: "(해석 없음)", fontSize = 14.sp, color = AppColors.Ink)
            } else {
                Text(t.chinese, fontSize = 14.sp, color = AppColors.Ink)
                t.pinyin?.let { Text(it, color = AppColors.Sub, fontSize = 10.5.sp) }
            }
        }
        if (editMode) {
            IconButton(onClick = onDelete) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFBE0E6)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DeleteOutline, "삭제", tint = Color(0xFFC24062), modifier = Modifier.size(15.dp))
                }
            }
        } else {
            SpeakButton(onSpeak)
        }
    }
}

@Composable
private fun LangToggle(showTranslation: Boolean, onSet: (Boolean) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(9.dp)).background(AppColors.Line).padding(2.dp)) {
        listOf(false to "중국어", true to "번역").forEach { (v, label) ->
            val on = v == showTranslation
            Box(
                Modifier.clip(RoundedCornerShape(7.dp))
                    .background(if (on) AppColors.Purple else Color.Transparent)
                    .clickable { onSet(v) }.padding(horizontal = 9.dp, vertical = 5.dp),
            ) { Text(label, color = if (on) Color.White else AppColors.Sub, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun PlayAllButton(onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(9.dp)).background(AppColors.Purple)
            .clickable { onClick() }.padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp))
        Text(" 전체 듣기", color = Color.White, fontSize = 10.5.sp)
    }
}

@Composable
private fun UnitDropdown(title: String, options: List<Pair<Long, String>>, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(11.dp)).border(1.dp, AppColors.Muted, RoundedCornerShape(11.dp))
                .clickable { open = true }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = AppColors.Purple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.ExpandMore, null, tint = AppColors.Muted, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, t) -> DropdownMenuItem(text = { Text(t) }, onClick = { onSelect(id); open = false }) }
        }
    }
}

@Composable
private fun AddLineDialog(onDismiss: () -> Unit, onAdd: (String?, String, String?, String?) -> Unit) {
    var speaker by remember { mutableStateOf("") }
    var chinese by remember { mutableStateOf("") }
    var pinyin by remember { mutableStateOf("") }
    var korean by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("문장 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(speaker, { speaker = it }, label = { Text("화자 (A/B, 본문이면 비움)") }, singleLine = true)
                OutlinedTextField(chinese, { chinese = it }, label = { Text("중국어") }, singleLine = true)
                OutlinedTextField(pinyin, { pinyin = it }, label = { Text("병음") }, singleLine = true)
                OutlinedTextField(korean, { korean = it }, label = { Text("해석") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(speaker.trim().ifBlank { null }, chinese.trim(), pinyin.trim().ifBlank { null }, korean.trim().ifBlank { null }) },
                enabled = chinese.isNotBlank(),
            ) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
