package com.example.chineselock.feature.capture

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.chineselock.ui.PosBoxes
import com.example.chineselock.ui.theme.AppColors
import com.google.mlkit.vision.common.InputImage

@Composable
fun CaptureScreen(onBack: () -> Unit, vm: CaptureViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                if (hasPermission) {
                    CameraView(onCaptured = vm::onImageCaptured, onBack = onBack)
                } else {
                    PermissionPrompt(
                        onRequest = { permLauncher.launch(Manifest.permission.CAMERA) },
                        onBack = onBack,
                    )
                }

            CaptureViewModel.Phase.PROCESSING -> ProcessingView()

            CaptureViewModel.Phase.REVIEW -> ReviewView(
                ui = ui,
                onTitle = vm::setTitle,
                onRemove = vm::removeItem,
                onRetake = vm::retake,
                onSave = {
                    vm.save(System.currentTimeMillis()) { n ->
                        Toast.makeText(context, "${n}개 단어를 등록했어요", Toast.LENGTH_SHORT).show()
                        if (n > 0) onBack()
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
}

@Composable
private fun CameraView(onCaptured: (InputImage) -> Unit, onBack: () -> Unit) {
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
                "단어 페이지를 화면에 꽉 차게 맞추고 촬영하세요",
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
                                    val bitmap = image.toBitmap()
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()
                                    onCaptured(InputImage.fromBitmap(bitmap, rotation))
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

@Composable
private fun ProcessingView() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = AppColors.Purple)
        Spacer(Modifier.height(20.dp))
        Text("글자를 읽고 단어를 정리하는 중…", color = AppColors.Sub, fontSize = 14.sp)
    }
}

@Composable
private fun ReviewView(
    ui: CaptureViewModel.UiState,
    onTitle: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetake) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "다시 찍기") }
            Text("인식 결과", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        Text("제목 (단원)", color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        OutlinedTextField(
            value = ui.title, onValueChange = onTitle, singleLine = true,
            placeholder = { Text("예: 3-1") }, modifier = Modifier.fillMaxWidth(),
        )

        Text(
            "인식된 단어 ${ui.items.size}개 — 틀린 항목은 X로 빼고 등록하세요",
            color = AppColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        Column(
            Modifier.fillMaxWidth().border(1.dp, AppColors.Line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        ) {
            ui.items.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
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
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onSave, enabled = ui.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("${ui.items.size}개 단어 등록")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text("다시 찍기") }
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
