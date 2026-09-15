package io.github.meko123456.kharji.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.meko123456.kharji.domain.receipt.ReceiptParser
import io.github.meko123456.kharji.domain.receipt.ScannedReceipt

/**
 * Point-and-shoot receipt capture: CameraX preview → still capture → on-device ML Kit
 * text recognition → the pure [ReceiptParser]. The result is handed back as a guess the
 * user confirms, so a bad scan can never silently become an expense.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onScanned: (ScannedReceipt) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var status by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    // One recognizer for the screen rather than one per shot. The flow here is capture, read
    // "try again, straighter", capture again, and building a client each time reloads the model
    // on every attempt. It holds native resources, so it is closed when the screen goes away.
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }

    // Binding the camera is only half of it; something has to let go of it again.
    //
    // This screen is shown by a plain `if` in the home screen, not a navigation destination, so
    // closing it removes the composable while the activity stays RESUMED — and CameraX keeps a
    // binding for exactly as long as the lifecycle it was given stays started. Nothing here ever
    // unbound, so the camera stayed open after the scanner was closed: the green camera indicator
    // stuck on, no other app able to take the camera, and the sensor drawing power for as long as
    // Kharji was in the foreground.
    //
    // The provider arrives asynchronously, which adds the other half of the problem: closing the
    // scanner before it arrives used to bind a camera that nothing was left to unbind at all.
    // [disposed] is read and written only on the main executor, which is also where onDispose runs.
    if (hasPermission) {
        DisposableEffect(lifecycleOwner, previewView) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            var provider: ProcessCameraProvider? = null
            var disposed = false

            providerFuture.addListener({
                if (disposed) return@addListener
                val resolved = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
                provider = resolved
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                runCatching {
                    resolved.unbindAll()
                    resolved.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                disposed = true
                provider?.unbindAll()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan receipt") },
                navigationIcon = {
                    // material-icons-core has no Close glyph; a text ✕ matches the rest of the app.
                    IconButton(onClick = onBack) {
                        Text(
                            "✕",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "Close scanner" },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (hasPermission) {
                    // Binding lives in the DisposableEffect above, so it has somewhere to be undone.
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
                } else {
                    Text(
                        "Camera permission is needed to scan receipts.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Button(
                    enabled = hasPermission,
                    onClick = {
                        status = "Reading receipt…"
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    recognize(recognizer, image) { lines ->
                                        val receipt = ReceiptParser.parse(lines)
                                        if (receipt == null) {
                                            status = "Couldn't read a total — try again, straighter."
                                        } else {
                                            onScanned(receipt)
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    status = "Capture failed: ${exception.message}"
                                }
                            },
                        )
                    },
                ) { Text("Capture") }
            }
        }
    }
}

/**
 * Runs on-device text recognition and hands back the recognised lines.
 *
 * [ImageProxy.getImage] is opt-in: ML Kit needs the underlying `android.media.Image`, and
 * we honour the contract by keeping the proxy open until recognition completes.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun recognize(recognizer: TextRecognizer, image: ImageProxy, onLines: (List<String>) -> Unit) {
    val media = image.image
    if (media == null) {
        image.close()
        onLines(emptyList())
        return
    }
    val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
    recognizer.process(input)
        .addOnSuccessListener { text ->
            onLines(text.textBlocks.flatMap { block -> block.lines.map { it.text } })
        }
        .addOnFailureListener { onLines(emptyList()) }
        .addOnCompleteListener { image.close() }
}
