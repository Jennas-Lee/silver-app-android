package io.github.jennaslee.silverapp.kiosk

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jennaslee.silverapp.BuildConfig
import io.github.jennaslee.silverapp.data.remote.KioskTarget
import io.github.jennaslee.silverapp.ui.BigButton

/** 눌러야 할 곳을 표시하는 색. 사진 위에서 눈에 잘 띄도록 진한 빨강 */
private val TargetColor = Color(0xFFE5191A)

@Composable
fun KioskScreen(onClose: () -> Unit, vm: KioskViewModel = viewModel()) {
    val step by vm.step.collectAsStateWithLifecycle()
    val goal by vm.goal.collectAsStateWithLifecycle()

    val close = {
        vm.stopSpeaking()
        onClose()
    }
    BackHandler(onBack = close)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = close, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("뒤로", style = MaterialTheme.typography.labelLarge)
                }
                Text("키오스크 도우미", style = MaterialTheme.typography.headlineMedium)
            }
            if (goal.isNotEmpty() && step !is KioskStep.AskGoal) {
                Text("하고 싶은 것: $goal", style = MaterialTheme.typography.bodyLarge)
            }

            when (val s = step) {
                KioskStep.AskGoal -> AskGoalContent(onGoal = vm::setGoal)
                is KioskStep.Camera -> CameraContent(hint = s.hint, onPhoto = vm::onPhoto, modifier = Modifier.weight(1f))
                is KioskStep.Thinking -> ThinkingContent(s.photo)
                is KioskStep.Answer -> AnswerContent(s, vm)
                is KioskStep.Failed -> FailedContent(s, vm)
            }
        }
    }
}

// ① 하고 싶은 것 묻기

@Composable
private fun AskGoalContent(onGoal: (String) -> Unit) {
    var typing by remember { mutableStateOf(false) }
    val speak = rememberVoiceInput(prompt = "하고 싶은 것을 말씀해 주세요", onResult = onGoal, onUnavailable = { typing = true })

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("무엇을 하고 싶으세요?", style = MaterialTheme.typography.headlineLarge)
        Text("예: 불고기버거 먹고 싶어, 병원 접수하고 싶어", style = MaterialTheme.typography.bodyLarge)
        BigButton("말로 하기", speak)
        if (typing) {
            TextAnswer(label = "하고 싶은 것", onSubmit = onGoal)
        } else {
            OutlinedBigButton("글로 쓰기") { typing = true }
        }
    }
}

// ② 화면 찍기

@Composable
private fun CameraContent(hint: String?, onPhoto: (KioskPhoto) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var capturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }

    // 디버그 빌드 전용: 에뮬레이터에서는 실제 키오스크를 비출 수 없으므로 저장된 사진으로 시험
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { KioskImage.fromUri(context, it) }?.let(onPhoto)
    }

    // 미리보기가 남는 공간만 차지해서, 작은 휴대폰에서도 큰 "화면 찍기" 버튼이 항상 보이게 한다
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hint != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text("방금 안내: $hint", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            }
        }

        if (!granted) {
            Text("화면을 보려면 카메라를 써야 해요.", style = MaterialTheme.typography.bodyLarge)
            BigButton("카메라 허용하기") { askCamera.launch(Manifest.permission.CAMERA) }
        } else {
            Text("주문하는 화면 전체가 보이게 비춰 주세요.", style = MaterialTheme.typography.bodyLarge)
            CameraPreview(
                imageCapture = imageCapture,
                onError = { cameraError = true },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            if (cameraError) {
                Text("카메라를 켜지 못했어요.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    capturing = true
                    capture(context, imageCapture, onPhoto = { capturing = false; onPhoto(it) }, onError = { capturing = false; cameraError = true })
                },
                enabled = !capturing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp),
            ) {
                Text(if (capturing) "찍는 중…" else "화면 찍기", style = MaterialTheme.typography.headlineMedium)
            }
        }

        if (BuildConfig.DEBUG) {
            OutlinedBigButton("사진 고르기 (시험용)") {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, onError: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            runCatching {
                val p = future.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val selector = if (p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            }.onFailure {
                Log.w("KioskScreen", "카메라 연결 실패", it)
                onError()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun capture(context: Context, imageCapture: ImageCapture, onPhoto: (KioskPhoto) -> Unit, onError: () -> Unit) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val bitmap = image.toBitmap()
                image.close()
                onPhoto(KioskImage.prepare(bitmap, rotation))
            }

            override fun onError(exception: ImageCaptureException) {
                Log.w("KioskScreen", "사진 찍기 실패", exception)
                onError()
            }
        },
    )
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

// ③ 살펴보는 중

@Composable
private fun ThinkingContent(photo: KioskPhoto) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PhotoWithTarget(photo, target = null)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
            Text("화면을 살펴보고 있어요…", style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ④ 안내 결과

@Composable
private fun AnswerContent(answer: KioskStep.Answer, vm: KioskViewModel) {
    val result = answer.result
    var typing by remember(answer) { mutableStateOf(false) }
    val speakReply = rememberVoiceInput(prompt = "대답을 말씀해 주세요", onResult = vm::reply, onUnavailable = { typing = true })

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PhotoWithTarget(answer.photo, result.target, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(result.speech, style = MaterialTheme.typography.headlineMedium)

        when (result.status) {
            "done" -> {
                BigButton("처음으로", vm::restart)
            }
            "need_input" -> {
                BigButton("말로 대답하기", speakReply)
                if (typing) {
                    TextAnswer(label = "대답", onSubmit = vm::reply)
                } else {
                    OutlinedBigButton("글로 대답하기") { typing = true }
                }
            }
            "unclear" -> {
                BigButton("다시 찍기", vm::retake)
            }
            else -> {
                BigButton("다 눌렀어요", vm::next)
            }
        }
        OutlinedBigButton("다시 듣기", vm::replay)
    }
}

@Composable
private fun FailedContent(failed: KioskStep.Failed, vm: KioskViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(failed.message, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        failed.photo?.let { photo -> BigButton("다시 해 보기") { vm.onPhoto(photo) } }
        OutlinedBigButton("다시 찍기", vm::retake)
    }
}

/** 찍은 사진 위에 눌러야 할 곳을 빨간 상자로 표시한다 */
@Composable
private fun PhotoWithTarget(photo: KioskPhoto, target: KioskTarget?, modifier: Modifier = Modifier) {
    val ratio = photo.bitmap.width.toFloat() / photo.bitmap.height
    Box(
        modifier = modifier
            .heightIn(max = 380.dp)
            .aspectRatio(ratio, matchHeightConstraintsFirst = true),
    ) {
        Image(
            bitmap = photo.bitmap.asImageBitmap(),
            contentDescription = "찍은 화면",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        val box = target?.box
        if (box != null && box.size == 4) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                drawRect(
                    color = TargetColor,
                    topLeft = Offset(box[0] * size.width, box[1] * size.height),
                    size = Size((box[2] - box[0]) * size.width, (box[3] - box[1]) * size.height),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}

// 공통 조각

@Composable
private fun OutlinedBigButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TextAnswer(label: String, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    BigButton("확인") { onSubmit(text) }
}

/**
 * 휴대폰 기본 음성 입력 창을 띄워 말을 글로 받는다 (마이크 권한은 음성 입력 앱이 처리).
 * 음성 입력을 쓸 수 없는 휴대폰이면 onUnavailable을 부른다.
 */
@Composable
private fun rememberVoiceInput(prompt: String, onResult: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let(onResult)
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("KioskScreen", "음성 입력을 쓸 수 없음", e)
            onUnavailable()
        }
    }
}
