package com.example.chineselock.feature.add

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chineselock.ui.PosBoxes
import com.example.chineselock.ui.theme.AppColors

@Composable
fun AddScreen(onBack: () -> Unit, vm: AddViewModel = hiltViewModel()) {
    val title by vm.title.collectAsStateWithLifecycle()
    val paste by vm.pasteText.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                Text("추가하기", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Text("제목 (단원)", color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            OutlinedTextField(
                value = title, onValueChange = vm::setTitle, singleLine = true,
                placeholder = { Text("예: 3-1") }, modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "직접 입력 — 뜻 / 병음 / 한자 (탭 또는 콤마 구분, 줄바꿈으로 여러 개)",
                color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            )
            OutlinedTextField(
                value = paste, onValueChange = vm::setPaste,
                placeholder = { Text("다음에\txiàcì\t下次") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )

            if (preview.isNotEmpty()) {
                Text("미리보기 ${preview.size}개", color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                Column(
                    Modifier.fillMaxWidth().border(1.dp, AppColors.Line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
                ) {
                    preview.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.hanzi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PosBoxes(item.pos.joinToString("·").ifEmpty { null })
                                    Text(item.meaning, fontSize = 13.sp)
                                }
                                Text(item.pinyin, color = AppColors.Sub, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    vm.register(System.currentTimeMillis()) { n ->
                        Toast.makeText(context, "${n}개 단어를 등록했어요", Toast.LENGTH_SHORT).show()
                        if (n > 0) onBack()
                    }
                },
                enabled = preview.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("${preview.size}개 단어 등록") }

            Spacer(Modifier.height(8.dp))
            Text(
                "교재 촬영(OCR→자동 구조화)은 다음 단계에서 연결됩니다.",
                color = AppColors.Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}
