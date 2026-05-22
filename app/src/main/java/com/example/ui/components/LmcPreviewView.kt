package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.LmcViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LmcPreviewView(
    viewModel: LmcViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isInitialized by remember { mutableStateOf(false) }

    // Use Accompanist permissions to request Camera authorization
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val previewMode by viewModel.cameraPreviewMode.collectAsState()
    val isCameraGranted = cameraPermissionState.status.isGranted

    Box(modifier = modifier.fillMaxSize()) {
        if (previewMode == "Real" && isCameraGranted) {
            // Render physical CameraX view
            CameraXPreview(
                context = context,
                modifier = Modifier.fillMaxSize()
            )
            // Floating feedback of active filter/tuning profile overlay on the physical sensor
            FilterOverlay(viewModel = viewModel)
        } else {
            // Render dynamic Leica Atmospheric Scene Simulator
            InteractiveSimulatorScene(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay layout lines and Grid Guides
        val gridMode by viewModel.gridMode.collectAsState()
        LmcGridOverlay(gridMode = gridMode)

        // Show horizontal crosshair gauge alignment helper (horizon level)
        val showLevel by viewModel.showLevelIndicator.collectAsState()
        val roll by viewModel.levelRoll.collectAsState()
        val pitch by viewModel.levelPitch.collectAsState()

        if (showLevel) {
            LmcLevelGauge(
                roll = roll,
                pitch = pitch,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-50).dp)
            )
        }

        // Fallback banner overlay if Camera permission isn't requested/granted
        if (previewMode == "Real" && !isCameraGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NoFlash,
                        contentDescription = "Permission Blocked",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To access physical camera optic feeds, please grant permissions.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("request_camera_permission_btn")
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { viewModel.setPreviewMode("Simulator") }
                    ) {
                        Text("Continue in Simulator Mode", color = Color(0xFFFDD835))
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Real CameraX preview implementation container using Android Lifecycle
// --------------------------------------------------------------------------
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraXPreview(
    context: Context,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(key1 = cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

// --------------------------------------------------------------------------
// Digital Filter overlay to shift Leica hues / BW tone on Real preview
// --------------------------------------------------------------------------
@Composable
fun FilterOverlay(viewModel: LmcViewModel) {
    val style by viewModel.leicaStyle.collectAsState()
    val ev by viewModel.ev.collectAsState()

    val overlayAlpha = (ev * 0.15f).coerceIn(-0.4f, 0.4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                // Color overlay for exposure and leica mode
                if (style == "Noir") {
                    // Gray overlay simulation (purely client filter styling over live view)
                    drawRect(
                        color = Color.Black,
                        alpha = 0.05f
                    )
                } else if (style == "Classic") {
                    drawRect(
                        color = Color(0xFFBF8000), // warm tint screen
                        alpha = 0.08f
                    )
                }

                // Brightness adjustment draw (positive / negative EV)
                if (overlayAlpha > 0f) {
                    drawRect(color = Color.White, alpha = overlayAlpha)
                } else if (overlayAlpha < 0f) {
                    drawRect(color = Color.Black, alpha = -overlayAlpha)
                }
            }
    )
}

// --------------------------------------------------------------------------
// Custom Interactive High-Fidelity Scenic Simulator Composable
// --------------------------------------------------------------------------
@Composable
fun InteractiveSimulatorScene(
    viewModel: LmcViewModel,
    modifier: Modifier = Modifier
) {
    val sceneIdx by viewModel.simulatorSceneIndex.collectAsState()
    val ev by viewModel.ev.collectAsState()
    val iso by viewModel.iso.collectAsState()
    val shutter by viewModel.shutterSpeed.collectAsState()
    val focus by viewModel.focusValue.collectAsState()
    val whiteBalance by viewModel.whiteBalanceMode.collectAsState()
    val leicaStyle by viewModel.leicaStyle.collectAsState()
    val activeConfig by viewModel.activeConfigName.collectAsState()
    val cameraMode by viewModel.cameraMode.collectAsState()

    // Breathing stars/clouds animation time flow
    val timeTransition = rememberInfiniteTransition(label = "scenic_movement")
    val timeSpeed by timeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radial_radians"
    )

    // Blur multiplier depending on Focus Slider value
    val focusDiffFactor: Float = remember(focus, sceneIdx) {
        // Compute blur maps based on focus
        // Scene 0: focus at 0.2f is hyper sharp flower, focus at 1.0f is sharp back
        // Scene 1: focus at 1.0f is sharp mountains, 0.1f is soft
        when (sceneIdx) {
            0 -> {
                val bestFocus = 0.22f
                if (focus == -1.0f) 0f else kotlin.math.abs(focus - bestFocus) * 15f
            }
            1 -> {
                val bestFocus = 1.0f
                if (focus == -1.0f) 0f else kotlin.math.abs(focus - bestFocus) * 12f
            }
            else -> {
                // Cyberpunk
                val bestFocus = 0.5f
                if (focus == -1.0f) 0f else kotlin.math.abs(focus - bestFocus) * 10f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101012))
            .blur(focusDiffFactor.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Calculate exposure shift multiplier (represented as an offset representing lighting)
            // positive value is brighter, negative value is darker
            val evLightingShift = (ev * 35).toInt()

            val pBlackBackground = Paint().apply { isAntiAlias = true }

            if (sceneIdx == 0) {
                // ----------------------------------------------------
                // Scene 0: Flower Macro Close-up
                // ----------------------------------------------------
                // Soft gradient green garden circle backgrounds
                drawCircle(
                    color = Color(0xFF2E5A1C).copy(alpha = 0.9f),
                    center = Offset(w * 0.2f, h * 0.3f),
                    radius = 200f
                )
                drawCircle(
                    color = Color(0xFF1B4D0F),
                    center = Offset(w * 0.8f, h * 0.7f),
                    radius = 350f
                )

                // Flower Center
                val fx = w / 2f
                val fy = h / 2f
                val petalPaintColor = Color(0xFFFFB300)

                // Render background defocused petals if focus is away
                val scaleFactor = if (focusDiffFactor > 4f) 0.9f else 1.0f
                
                // Draw 8 big yellow orange flower petals radiating outward
                for (a in 0 until 360 step 45) {
                    val angleRad = Math.toRadians(a.toDouble())
                    val dX = (fx + 180 * scaleFactor * cos(angleRad)).toFloat()
                    val dY = (fy + 180 * scaleFactor * sin(angleRad)).toFloat()
                    drawCircle(
                        color = petalPaintColor,
                        center = Offset(dX, dY),
                        radius = 110f
                    )
                }

                // Dark brown middle flower disk
                drawCircle(
                    color = Color(0xFF421E07),
                    center = Offset(fx, fy),
                    radius = 115f
                )

                // Draw tiny yellow pollen seeds in concentric circles
                for (rot in 0 until 360 step 30) {
                    val angleRad = Math.toRadians(rot.toDouble())
                    val seedX = (fx + 75 * cos(angleRad)).toFloat()
                    val seedY = (fy + 75 * sin(angleRad)).toFloat()
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        center = Offset(seedX, seedY),
                        radius = 8f
                    )
                }

            } else if (sceneIdx == 1) {
                // ----------------------------------------------------
                // Scene 1: Alpine Sunset Lake
                // ----------------------------------------------------
                // Sky base colors modified by WB mode
                val topColor = when (whiteBalance) {
                    "Sunny" -> Color(0xFFFF5722)
                    "Fluorescent" -> Color(0xFF2196F3)
                    "Cloudy" -> Color(0xFFFF9800)
                    "Incandescent" -> Color(0xFF9C27B0)
                    else -> Color(0xFFFF4500)
                }

                val brush = Brush.verticalGradient(
                    colors = listOf(topColor, Color(0xFFFFCC80), Color(0xFF81D4FA)),
                    startY = 0f,
                    endY = h * 0.7f
                )
                drawRect(brush = brush)

                // Beautiful large setting sun in the back
                drawCircle(
                    color = Color(0xFFFFF9C4),
                    center = Offset(w / 2f, h * 0.42f),
                    radius = 90f
                )

                // Purple mountain skyline peak paths
                val p1 = Offset(0f, h * 0.48f)
                val p2 = Offset(w * 0.3f, h * 0.35f)
                val p3 = Offset(w * 0.55f, h * 0.45f)
                val p4 = Offset(w * 0.78f, h * 0.28f)
                val p5 = Offset(w, h * 0.5f)

                val mountainPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                    lineTo(p5.x, p5.y)
                    lineTo(w, h * 0.55f)
                    lineTo(0f, h * 0.55f)
                    close()
                }
                drawPath(
                    path = mountainPath,
                    color = Color(0xFF3F1D56)
                )

                // Dark water sheet
                drawRect(
                    color = Color(0xFF204070).copy(alpha = 0.85f),
                    topLeft = Offset(0f, h * 0.55f),
                    size = androidx.compose.ui.geometry.Size(w, h * 0.45f)
                )

                // Live animated sunset water ripples!
                val rippleWave = sin(timeSpeed) * 35f
                drawLine(
                    color = Color(0xFFFFF59D).copy(alpha = 0.5f),
                    start = Offset(w / 2f - 80f + rippleWave, h * 0.60f),
                    end = Offset(w / 2f + 80f + rippleWave, h * 0.60f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFFFFF59D).copy(alpha = 0.35f),
                    start = Offset(w / 2f - 140f - rippleWave, h * 0.68f),
                    end = Offset(w / 2f + 140f - rippleWave, h * 0.68f),
                    strokeWidth = 3.5f
                )
                drawLine(
                    color = Color(0xFFFFF59D).copy(alpha = 0.25f),
                    start = Offset(w / 2f - 220f + rippleWave, h * 0.78f),
                    end = Offset(w / 2f + 220f + rippleWave, h * 0.78f),
                    strokeWidth = 4f
                )

                // Shooting stars (only if Shutter duration is long e.g. Night mode/Pro astro selection)
                val activeShutterLong = shutter in listOf("1s", "2s") || cameraMode == "Night Sight"
                if (activeShutterLong) {
                    val starAnimOffset = timeSpeed * 40f
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(100f + starAnimOffset, 120f + starAnimOffset),
                        end = Offset(180f + starAnimOffset, 200f + starAnimOffset),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(w - 300f + starAnimOffset, 80f + starAnimOffset),
                        end = Offset(w - 220f + starAnimOffset, 160f + starAnimOffset),
                        strokeWidth = 2.5f
                    )
                }

            } else {
                // ----------------------------------------------------
                // Scene 2: Cyberpunk Street (Lowlight Neon tracker)
                // ----------------------------------------------------
                // Dark asphalt gradient background
                val noirBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF07000B), Color(0xFF0F0A1F), Color(0xFF040209)),
                    startY = 0f,
                    endY = h
                )
                drawRect(brush = noirBrush)

                // Draw neon buildings side structures
                drawRect(
                    color = Color(0xFF140F24),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(120.dp.toPx(), h)
                )
                drawRect(
                    color = Color(0xFF140F24),
                    topLeft = Offset(w - 120.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(120.dp.toPx(), h)
                )

                // Glowing Neon signs drawing
                drawCircle(
                    color = Color(0xFFFF2D87),
                    center = Offset(70.dp.toPx(), 200.dp.toPx()),
                    radius = 35.dp.toPx(),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(w - 85.dp.toPx(), 120.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(50.dp.toPx(), 180.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Defocused light blobs in the background center of the futuristic street
                val cycleBob = sin(timeSpeed) * 15f
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = 0.35f),
                    center = Offset(w / 2f - 40f, h / 2f - 80f + cycleBob),
                    radius = 75f
                )
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = 0.3f),
                    center = Offset(w / 2f + 50f, h / 2f - 40f - cycleBob),
                    radius = 90f
                )

                // Street reflective puddles (shimmering)
                drawLine(
                    color = Color(0xFFFF2D87).copy(alpha = 0.45f),
                    start = Offset(110.dp.toPx(), h - 250.dp.toPx()),
                    end = Offset(240.dp.toPx(), h - 250.dp.toPx()),
                    strokeWidth = 14f
                )
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                    start = Offset(w - 240.dp.toPx(), h - 180.dp.toPx()),
                    end = Offset(w - 110.dp.toPx(), h - 180.dp.toPx()),
                    strokeWidth = 16f
                )
            }

            // Draw Exposure Correction value filter
            val finalFilterAlpha = (evLightingShift.toFloat() / 255f).coerceIn(-0.6f, 0.6f)
            if (finalFilterAlpha > 0f) {
                drawRect(color = Color.White, alpha = finalFilterAlpha)
            } else if (finalFilterAlpha < 0f) {
                drawRect(color = Color.Black, alpha = -finalFilterAlpha)
            }

            // Draw Vignette parameters
            val vigPercent = (100 - (100 - viewModel.vignetteAmount.value)).toFloat() / 100f
            if (vigPercent > 0.1f) {
                // Gradient vignette simulation
                val vigAlpha = (vigPercent * 0.7f).coerceIn(0f, 0.95f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = vigAlpha)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.75f
                    )
                )
            }
        }

        // Apply Grain Noise on simulator if high ISO (synthetic noise overlay)
        val targetNoise = if (cameraMode == "Night Sight") 0.04f else (if (iso == -1) 0.02f else (iso.toFloat() / 3200f) * 0.28f)
        if (targetNoise > 0.05f) {
            GrainNoiseOverlay(intensity = targetNoise)
        }

        // Telemetry readout block floating on top
        TelemetryDataOverlay(
            viewModel = viewModel,
            activeConfig = activeConfig,
            sceneIdx = sceneIdx,
            shutter = shutter,
            iso = iso,
            focus = focus,
            cameraMode = cameraMode,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )

        // Scene changer toggle button
        IconButton(
            onClick = { viewModel.cycleSimulatorScene() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .testTag("cycle_simulator_scene_btn")
                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SwitchVideo,
                contentDescription = "Switch Scene Context",
                tint = Color(0xFFFDD835)
            )
        }
    }
}

// Draw dynamic raw salt and pepper style noise
@Composable
fun GrainNoiseOverlay(intensity: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val w = size.width
        val h = size.height
        val rand = Random()
        val count = (intensity * 6000).toInt().coerceIn(100, 15000)

        for (i in 0 until count) {
            val rx = rand.nextFloat() * w
            val ry = rand.nextFloat() * h
            val c = if (rand.nextBoolean()) Color.White.copy(alpha = 0.18f) else Color.Cyan.copy(alpha = 0.1f)
            drawCircle(
                color = c,
                radius = 1.3f,
                center = Offset(rx, ry)
            )
        }
    }
}

// Beautiful technical overlay of active photography specs
@Composable
fun TelemetryDataOverlay(
    viewModel: LmcViewModel,
    activeConfig: String,
    sceneIdx: Int,
    shutter: String,
    iso: Int,
    focus: Float,
    cameraMode: String,
    modifier: Modifier = Modifier
) {
    val style by viewModel.leicaStyle.collectAsState()
    val rawBadgeText = if (style == "Noir") "LEICA B&W" else "RAW 14B"
    val zoomLevel by viewModel.zoomLevel.collectAsState()

    val sceneLabel = when (sceneIdx) {
        0 -> "Flower Macro"
        1 -> "Sunset Alps"
        else -> "Cyberpunk Neon"
    }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = rawBadgeText,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "SCENE: $sceneLabel • ${zoomLevel} LENS",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "XML: $activeConfig",
            color = Color(0xFFFDD835),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(2.dp))

        val isoLabel = if (iso == -1) "Auto ISO" else "ISO ${iso}"
        val focusLabel = if (focus == -1.0f) "Auto Focus" else "MF: ${(focus * 100).toInt()}%"
        Text(
            text = "$cameraMode  |  $shutter  |  $isoLabel  |  $focusLabel",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
