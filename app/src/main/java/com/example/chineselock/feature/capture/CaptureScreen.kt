package com.example.chineselock.feature.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.chineselock.core.network.DialogueItem
import com.example.chineselock.core.network.VocabItem
import com.example.chineselock.ui.PosBoxes
import com.example.chineselock.ui.SpeakerTag
import com.example.chineselock.ui.theme.AppColors

@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    mode: CaptureMode = CaptureMode.VOCAB,
    unitId: Long? = null,
    vm: CaptureViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val gateUnlocked by vm.gateUnlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(mode, unitId) { vm.configure(mode, unitId) }

    val unitLabel = when (ui.mode) {
        CaptureMode.VOCAB -> "단어"
        CaptureMode.DIALOGUE -> "문장"
        CaptureMode.TRANSLATION -> "번역"
    }

    // null=닫힘, -1=새로 추가, 0이상=해당 index 수정
    var editIndex by remember { mutableStateOf<Int?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Surface(Modifier.fillMaxSize()) {
        when (ui.phase) {
            CaptureViewModel.Phase.CAMERA ->
                if (!gateUnlocked) {
                    AdGateView(
                        unitLabel = unitLabel,
                        onWatchAd = { context.findActivity()?.let { vm.watchAdToUnlock(it) } },
                        onBack = onBack,
                    )
                } else if (hasPermission) {
                    CameraView(
                        guide = when (ui.mode) {
                            CaptureMode.VOCAB -> "단어 페이지를 화면에 꽉 차게 맞추고 촬영하세요"
                            CaptureMode.DIALOGUE -> "회화 페이지를 화면에 꽉 차게 맞추고 촬영하세요"
                            CaptureMode.TRANSLATION -> "해석(번역) 페이지를 화면에 꽉 차게 맞추고 촬영하세요"
                        },
                        onCaptured = vm::onPhotoTaken,
                        onBack = onBack,
                    )
                } else {
                    PermissionPrompt(
                        onRequest = { permLauncher.launch(Manifest.permission.CAMERA) },
                        onBack = onBack,
                    )
                }

            CaptureViewModel.Phase.CROP -> {
                val bmp = ui.captured
                if (bmp == null) {
                    LaunchedEffect(Unit) { vm.cancelCrop() }
                } else {
                    CropView(
                        bitmap = bmp,
                        onRotate = vm::rotateCaptured,
                        onConfirm = vm::onImageCaptured,
                        onCancel = vm::cancelCrop,
                    )
                }
            }

            CaptureViewModel.Phase.PROCESSING -> ProcessingView()

            CaptureViewModel.Phase.REVIEW -> ReviewView(
                ui = ui,
                unitLabel = unitLabel,
                onTitle = vm::setTitle,
                onRemove = vm::removeItem,
                onRetake = vm::retake,
                onCaptureMore = vm::captureMore,
                onRowClick = { editIndex = it },
                onAddManual = { editIndex = -1 },
                onSave = {
                    vm.save(System.currentTimeMillis()) { n ->
                        if (n > 0) {
                            Toast.makeText(context, "${n}개 ${unitLabel}을 등록했어요", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                },
            )

            CaptureViewModel.Phase.ERROR -> ErrorView(
                message = ui.error ?: "오류가 발생했어요.",
                onRetake = vm::retake,
                onBack = onBack,
            )
        }
    }

    val idx = editIndex
    if (idx != null) {
        when (ui.mode) {
            CaptureMode.VOCAB -> VocabEditDialog(
                initial = if (idx >= 0) ui.items.getOrNull(idx) else null,
                onDismiss = { editIndex = null },
                onConfirm = { h, p, pos, m ->
                    if (idx >= 0) vm.updateVocabItem(idx, h, p, pos, m) else vm.addVocabItem(h, p, pos, m)
                    editIndex = null
                },
            )
            CaptureMode.DIALOGUE -> DialogueEditDialog(
                initial = if (idx >= 0) ui.lines.getOrNull(idx) else null,
                onDismiss = { editIndex = null },
                onConfirm = { sp, zh, pin ->
                    if (idx >= 0) vm.updateDialogueLine(idx, sp, zh, pin) else vm.addDialogueLine(sp, zh, pin)
                    editIndex = null
                },
            )
            CaptureMode.TRANSLATION -> TranslationEditDialog(
                initial = if (idx >= 0) ui.translations.getOrNull(idx) else null,
                onDismiss = { editIndex = null },
                onConfirm = { t ->
                    if (idx >= 0) vm.updateTranslationLine(idx, t) else vm.addTranslationLine(t)
                    editIndex = null
                },
            )
        }
    }
}

@Composable
private fun CameraView(guide: String, onCaptured: (Bitmap) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.White) }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                guide,
                color = Color.White, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            // 셔터 버튼
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
                    .border(4.dp, AppColors.Purple, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val raw = image.toBitmap()
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()
                                    val upright = if (rotation == 0) raw else {
                                        val m = Matrix().apply { postRotate(rotation.toFloat()) }
                                        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                                    }
                                    onCaptured(upright)
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Toast.makeText(
                                        context,
                                        "촬영 실패: ${exc.message}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    },
                    modifier = Modifier.size(60.dp),
                ) {
                    Box(Modifier.size(56.dp).background(AppColors.Purple, CircleShape))
                }
            }
        }
    }
}

/**
 * 촬영 후 크롭/회전 화면. 사용자가 번역할 영역만 사각형으로 맞추고(모서리 드래그)
 * 누운 사진은 회전한 뒤 그 부분만 Gemini로 보낸다 — 옆 섹션·여백 노이즈 제거로 인식률 향상.
 */
@Composable
private fun CropView(
    bitmap: Bitmap,
    onRotate: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val handlePx = with(density) { 13.dp.toPx() }
    val touchPx = with(density) { 28.dp.toPx() }
    val minPx = with(density) { 56.dp.toPx() }
    val borderPx = with(density) { 2.dp.toPx() }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    // 크롭 영역을 '정규화 좌표(0~1, 이미지 기준)'로 보관 → 하단 버튼은 화면 크기 몰라도 됨.
    // 회전하면 bitmap이 바뀌므로 자동으로 전체에 가깝게 리셋된다.
    var norm by remember(bitmap) { mutableStateOf(Rect(0.05f, 0.05f, 0.95f, 0.95f)) }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()
            val scale = minOf(cw / bw, ch / bh)
            val dispW = bw * scale
            val dispH = bh * scale
            val imgLeft = (cw - dispW) / 2f
            val imgTop = (ch - dispH) / 2f
            // 정규화→화면 px 변환
            fun toScreen(n: Rect) = Rect(
                imgLeft + n.left * dispW, imgTop + n.top * dispH,
                imgLeft + n.right * dispW, imgTop + n.bottom * dispH,
            )
            val minNx = (minPx / dispW).coerceIn(0f, 1f)
            val minNy = (minPx / dispH).coerceIn(0f, 1f)
            // 0=TL,1=TR,2=BL,3=BR,4=이동,-1=없음
            var activeHandle by remember(bitmap) { mutableStateOf(-1) }

            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )

            Canvas(
                Modifier.fillMaxSize().pointerInput(bitmap, dispW, dispH) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val r = toScreen(norm)
                            activeHandle = when {
                                near(pos, r.left, r.top, touchPx) -> 0
                                near(pos, r.right, r.top, touchPx) -> 1
                                near(pos, r.left, r.bottom, touchPx) -> 2
                                near(pos, r.right, r.bottom, touchPx) -> 3
                                r.contains(pos) -> 4
                                else -> -1
                            }
                        },
                        onDragEnd = { activeHandle = -1 },
                        onDragCancel = { activeHandle = -1 },
                        onDrag = { change, drag ->
                            change.consume()
                            val dx = drag.x / dispW   // 화면 px → 정규화 delta
                            val dy = drag.y / dispH
                            val n = norm
                            norm = when (activeHandle) {
                                0 -> Rect((n.left + dx).coerceIn(0f, n.right - minNx), (n.top + dy).coerceIn(0f, n.bottom - minNy), n.right, n.bottom)
                                1 -> Rect(n.left, (n.top + dy).coerceIn(0f, n.bottom - minNy), (n.right + dx).coerceIn(n.left + minNx, 1f), n.bottom)
                                2 -> Rect((n.left + dx).coerceIn(0f, n.right - minNx), n.top, n.right, (n.bottom + dy).coerceIn(n.top + minNy, 1f))
                                3 -> Rect(n.left, n.top, (n.right + dx).coerceIn(n.left + minNx, 1f), (n.bottom + dy).coerceIn(n.top + minNy, 1f))
                                4 -> {
                                    val nl = (n.left + dx).coerceIn(0f, 1f - n.width)
                                    val nt = (n.top + dy).coerceIn(0f, 1f - n.height)
                                    Rect(nl, nt, nl + n.width, nt + n.height)
                                }
                                else -> n
                            }
                        },
                    )
                }
            ) {
                val r = toScreen(norm)
                val dim = Color(0xAA000000)
                drawRect(dim, Offset(0f, 0f), Size(size.width, r.top))
                drawRect(dim, Offset(0f, r.bottom), Size(size.width, size.height - r.bottom))
                drawRect(dim, Offset(0f, r.top), Size(r.left, r.height))
                drawRect(dim, Offset(r.right, r.top), Size(size.width - r.right, r.height))
                drawRect(
                    color = Color.White,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    style = Stroke(width = borderPx),
                )
                listOf(
                    Offset(r.left, r.top), Offset(r.right, r.top),
                    Offset(r.left, r.bottom), Offset(r.right, r.bottom),
                ).forEach { drawCircle(AppColors.Purple, handlePx, it) }
            }

            IconButton(onClick = onCancel, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "취소", tint = Color.White)
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.Black).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "번역할 영역만 남기고 모서리를 끌어 맞추세요. 사진이 누웠으면 회전하세요.",
                color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRotate, modifier = Modifier.weight(1f)) { Text("↻ 회전") }
                Button(
                    onClick = {
                        val n = norm
                        val bx = (n.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
                        val by = (n.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
                        val bwid = (n.width * bitmap.width).roundToInt().coerceIn(1, bitmap.width - bx)
                        val bhei = (n.height * bitmap.height).roundToInt().coerceIn(1, bitmap.height - by)
                        onConfirm(Bitmap.createBitmap(bitmap, bx, by, bwid, bhei))
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("이 영역만 인식") }
            }
        }
    }
}

private fun near(p: Offset, x: Float, y: Float, r: Float): Boolean {
    val dx = p.x - x; val dy = p.y - y
    return dx * dx + dy * dy <= r * r
}

@Composable
private fun ProcessingView() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = AppColors.Purple)
        Spacer(Modifier.height(20.dp))
        Text("글자를 읽고 정리하는 중…", color = AppColors.Sub, fontSize = 14.sp)
    }
}

@Composable
private fun ReviewView(
    ui: CaptureViewModel.UiState,
    unitLabel: String,
    onTitle: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onRetake: () -> Unit,
    onCaptureMore: () -> Unit,
    onRowClick: (Int) -> Unit,
    onAddManual: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetake) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "처음부터") }
            Text("인식 결과", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        if (ui.mode == CaptureMode.TRANSLATION) {
            Text(
                "회화 문장 순서대로 위에서부터 번역이 채워져요.",
                color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Text("제목 (단원) · 필수", color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            OutlinedTextField(
                value = ui.title, onValueChange = onTitle, singleLine = true,
                placeholder = { Text("예: 3-1", color = AppColors.Muted) }, modifier = Modifier.fillMaxWidth(),
            )
        }

        if (ui.error != null) {
            Text("⚠ ${ui.error}", color = Color(0xFFC24062), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        }

        Text(
            "인식된 $unitLabel ${ui.count}개 — 항목을 누르면 수정, X로 빼기",
            color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        Column(
            Modifier.fillMaxWidth().border(1.dp, AppColors.Line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        ) {
            when (ui.mode) {
                CaptureMode.VOCAB -> ui.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().clickable { onRowClick(index) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.hanzi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PosBoxes(item.pos.joinToString("·").ifEmpty { null })
                                Text(item.meaning, fontSize = 13.sp)
                            }
                            Text(item.pinyin, color = AppColors.Sub, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Close, "제외", tint = AppColors.Muted)
                        }
                    }
                }
                CaptureMode.DIALOGUE -> ui.lines.forEachIndexed { index, line ->
                    val speaker = line.speaker
                    val pinyin = line.pinyin
                    Row(Modifier.fillMaxWidth().clickable { onRowClick(index) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!speaker.isNullOrBlank()) {
                            SpeakerTag(speaker)
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(line.chinese, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            if (!pinyin.isNullOrBlank()) {
                                Text(pinyin, color = AppColors.Sub, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Close, "제외", tint = AppColors.Muted)
                        }
                    }
                }
                CaptureMode.TRANSLATION -> ui.translations.forEachIndexed { index, ko ->
                    Row(Modifier.fillMaxWidth().clickable { onRowClick(index) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", color = AppColors.Muted, fontSize = 12.sp, modifier = Modifier.width(22.dp))
                        Text(ko, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Close, "제외", tint = AppColors.Muted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAddManual, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
            Text("직접 추가")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCaptureMore, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
            Text("사진 더 찍기 (이어서 추가)")
        }
        Spacer(Modifier.height(8.dp))
        val titleRequired = ui.mode != CaptureMode.TRANSLATION
        val canSave = ui.count > 0 && (!titleRequired || ui.title.isNotBlank())
        Button(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
            Text("${ui.count}개 $unitLabel 등록")
        }
        if (titleRequired && ui.title.isBlank() && ui.count > 0) {
            Text(
                "제목(단원)을 입력해야 저장할 수 있어요",
                color = Color(0xFFC24062), fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text("처음부터 다시") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ErrorView(message: String, onRetake: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = AppColors.Ink, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text("다시 찍기") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("돌아가기") }
    }
}

/** "광고 보고 오늘 무료로 인식하기" 게이트 화면(출시 시 활성화). */
@Composable
private fun AdGateView(unitLabel: String, onWatchAd: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("오늘 ${unitLabel} 인식 무료로 열기", color = AppColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "광고를 한 번 보면 오늘 하루 사진 인식을 무제한으로 쓸 수 있어요.",
            color = AppColors.Sub, fontSize = 13.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onWatchAd, modifier = Modifier.fillMaxWidth()) { Text("광고 보고 오늘 무료로 인식하기") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("돌아가기") }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("교재를 촬영하려면 카메라 권한이 필요해요.", color = AppColors.Ink, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("카메라 권한 허용") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("돌아가기") }
    }
}

@Composable
private fun VocabEditDialog(
    initial: VocabItem?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>, String) -> Unit,
) {
    var hanzi by remember { mutableStateOf(initial?.hanzi ?: "") }
    var pinyin by remember { mutableStateOf(initial?.pinyin ?: "") }
    var meaning by remember { mutableStateOf(initial?.meaning ?: "") }
    var pos by remember { mutableStateOf(initial?.pos?.joinToString("·") ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "단어 추가" else "단어 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(hanzi, { hanzi = it }, label = { Text("한자") }, singleLine = true)
                OutlinedTextField(pinyin, { pinyin = it }, label = { Text("병음") }, singleLine = true)
                OutlinedTextField(meaning, { meaning = it }, label = { Text("뜻") }, singleLine = true)
                OutlinedTextField(pos, { pos = it }, label = { Text("품사 (·로 구분, 선택)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val posList = pos.split('·', ',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
                    onConfirm(hanzi.trim(), pinyin.trim(), posList, meaning.trim())
                },
                enabled = hanzi.isNotBlank() && meaning.isNotBlank(),
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun DialogueEditDialog(
    initial: DialogueItem?,
    onDismiss: () -> Unit,
    onConfirm: (String?, String, String?) -> Unit,
) {
    var speaker by remember { mutableStateOf(initial?.speaker ?: "") }
    var chinese by remember { mutableStateOf(initial?.chinese ?: "") }
    var pinyin by remember { mutableStateOf(initial?.pinyin ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "문장 추가" else "문장 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(speaker, { speaker = it }, label = { Text("화자 (A/B, 본문이면 비움)") }, singleLine = true)
                OutlinedTextField(chinese, { chinese = it }, label = { Text("중국어") })
                OutlinedTextField(pinyin, { pinyin = it }, label = { Text("병음") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(speaker.trim().ifBlank { null }, chinese.trim(), pinyin.trim().ifBlank { null }) },
                enabled = chinese.isNotBlank(),
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun TranslationEditDialog(
    initial: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "번역 추가" else "번역 수정") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("한국어 해석") }) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
