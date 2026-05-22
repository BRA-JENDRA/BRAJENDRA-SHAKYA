package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LmcViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: LmcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Force a sleek dark mode color scheme for a classic Leica feel
            val darkSlateColorScheme = darkColorScheme(
                primary = Color(0xFFFDD835),     // Leica warm yellow accent
                secondary = Color(0xFFE53935),   // Leica classic red accent
                background = Color(0xFF0C0C0E),
                surface = Color(0xFF141418),
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(
                colorScheme = darkSlateColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsState()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                    fadeOut(animationSpec = tween(220))
                        },
                        label = "screen_routing"
                    ) { screen ->
                        when (screen) {
                            "camera" -> MainCameraDashboard(viewModel)
                            "configs" -> LmcXmlManager(viewModel)
                            "gallery" -> LmcGalleryView(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainCameraDashboard(viewModel: LmcViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Observe ViewModel flows
    val activeConfigName by viewModel.activeConfigName.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val ev by viewModel.ev.collectAsState()
    val iso by viewModel.iso.collectAsState()
    val shutter by viewModel.shutterSpeed.collectAsState()
    val focus by viewModel.focusValue.collectAsState()
    val cameraMode by viewModel.cameraMode.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val captureProgress by viewModel.captureProgress.collectAsState()
    val captureProgressText by viewModel.captureProgressText.collectAsState()
    val isSettingsOpen by viewModel.isSettingsDrawerOpen.collectAsState()
    val photos by viewModel.photosList.collectAsState()

    // Determine latest photo taken for gallery thumbnail preview
    val latestPhoto = remember(photos) {
        if (photos.isNotEmpty()) photos.first() else null
    }

    val latestBitmap = remember(latestPhoto) {
        try {
            if (latestPhoto != null) {
                val f = File(latestPhoto.imagePath)
                if (f.exists()) {
                    android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Capture system safe areas automatically
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("camera_screen_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            // ----------------------------------------------------
            // 1. FULL SCREEN CAMERA PREVIEW CANVAS
            // ----------------------------------------------------
            LmcPreviewView(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("active_camera_sensor")
            )

            // ----------------------------------------------------
            // 2. TOP ACTION SETTINGS OVERLAY
            // ----------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                TopHUDHeader(
                    viewModel = viewModel,
                    activeConfig = activeConfigName,
                    isSettingsOpen = isSettingsOpen
                )

                // Slide-down Tactical settings tray drawer
                AnimatedVisibility(
                    visible = isSettingsOpen,
                    enter = slideInVertically(animationSpec = tween(300)) { -it } + fadeIn(),
                    exit = slideOutVertically(animationSpec = tween(250)) { -it } + fadeOut()
                ) {
                    TacticalSettingsTray(viewModel = viewModel)
                }
            }

            // ----------------------------------------------------
            // 3. FLOATING SIDE PRO SLIDERS (EV / FOCUS)
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus Slider node
                LmcVerticalSlider(
                    label = "FOCUS",
                    value = focus,
                    onValueChange = { viewModel.setFocusValue(it) },
                    valueRange = -1.0f..1.0f,
                    displayValue = if (focus == -1.0f) "AUTO" else "${(focus * 100).toInt()}%"
                )

                // Exposure (EV) Slider node
                LmcVerticalSlider(
                    label = "EV",
                    value = ev,
                    onValueChange = { viewModel.setEv(it) },
                    valueRange = -3.0f..3.0f,
                    displayValue = if (ev > 0f) "+${String.format("%.1f", ev)}" else String.format("%.1f", ev)
                )
            }

            // ----------------------------------------------------
            // 4. FLOATING ADJACENT EXTRA SIDE SLIDERS FOR "PRO MODE"
            // ----------------------------------------------------
            if (cameraMode == "Pro") {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "PRO MANUAL",
                        color = Color(0xFFFDD835),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(90.dp)
                    )

                    // Compact Manual ISO toggler
                    ProInlineSegmentSelector(
                        title = "ISO SENSOR",
                        selected = if (iso == -1) "AUTO" else iso.toString(),
                        options = listOf("AUTO", "100", "400", "1600", "3200"),
                        onSelect = {
                            viewModel.setIso(if (it == "AUTO") -1 else it.toInt())
                        }
                    )

                    // Compact Manual Shutter speed toggler
                    ProInlineSegmentSelector(
                        title = "SHUTTER TIME",
                        selected = shutter,
                        options = listOf("Auto", "1/500s", "1/60s", "1/2s", "2s"),
                        onSelect = { viewModel.setShutterSpeed(it) }
                    )
                }
            }

            // ----------------------------------------------------
            // 5. BOTTOM COMMAND DECK PANELS (LENS, SHUTTER, MODES)
            // ----------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 0f,
                            endY = 150f
                        )
                    )
                    .padding(bottom = 12.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars), // Prevent systemic bottom-nav overlap!
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auxiliary zoom lenses selector bar
                LmcAuxLensSelector(
                    selectedLens = zoomLevel,
                    onLensSelected = { viewModel.setZoom(it) }
                )

                // Lens Mode Selector (Camera, Portrait, Night Sight, Pro)
                LmcModeCarousel(
                    currentMode = cameraMode,
                    onModeSelected = { viewModel.setCameraMode(it) }
                )

                // Shutter trigger deck rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left element: Gallery Thumbnail preview
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                            .border(2.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                            .clickable { viewModel.navigateTo("gallery") }
                            .testTag("gallery_shortcut_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (latestBitmap != null) {
                            Image(
                                bitmap = latestBitmap,
                                contentDescription = "Last captured thumbnail preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Empty Gallery roll",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Shutter trigger
                    LmcShutterButton(
                        onClick = { viewModel.capturePhoto() },
                        isCapturing = isCapturing,
                        progress = captureProgress
                    )

                    // Right element: XML database folder loader "SET"
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { viewModel.navigateTo("configs") }
                            .testTag("configs_shortcut_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "XML Presets Manager",
                                tint = Color(0xFFFDD835),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "SET",
                                color = Color(0xFFFDD835),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 6. FLOATING LIVE CAPTURING STAGE DIALOGS
            // ----------------------------------------------------
            AnimatedVisibility(
                visible = isCapturing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFDD835).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFDD835),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "PROCESSING ALGORITHMS...",
                        color = Color(0xFFFDD835),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = captureProgressText.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Compact Segment Selector for Pro Modes
// ----------------------------------------------------
@Composable
fun ProInlineSegmentSelector(
    title: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .width(90.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selected.lowercase() == opt.lowercase()
                val bg = if (isSelected) Color(0xFFFDD835) else Color.Transparent
                val tc = if (isSelected) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bg)
                        .clickable { onSelect(opt) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (opt == "AUTO") "A" else opt.substringBefore("s"),
                        color = tc,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Collapsed HUD Bar on top of the viewfinder screen
// ----------------------------------------------------
@Composable
fun TopHUDHeader(
    viewModel: LmcViewModel,
    activeConfig: String,
    isSettingsOpen: Boolean
) {
    val style by viewModel.leicaStyle.collectAsState()
    val rawBadgeText = if (style == "Noir") "B&W NOIR" else "RAW 14"
    val sensorMode by viewModel.cameraPreviewMode.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Branding label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "L",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
            Text(
                text = "LMC 8.4",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Center Dial: Clicking this drops-down the extensive config dashboard
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.60f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .clickable { viewModel.toggleSettingsDrawer() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("hud_dropdown_capsule"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = activeConfig.substringBefore(".xml").uppercase(),
                    color = Color(0xFFFDD835),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = if (isSettingsOpen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Expand controls deck",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Right side: Technical Badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (sensorMode == "Real") "PHYSICAL" else "SIMULATOR",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------
// Extensive Pro settings drawer matrix dropdown block
// ----------------------------------------------------
@Composable
fun TacticalSettingsTray(viewModel: LmcViewModel) {
    val style by viewModel.leicaStyle.collectAsState()
    val hdr by viewModel.hdrMode.collectAsState()
    val grid by viewModel.gridMode.collectAsState()
    val showLevel by viewModel.showLevelIndicator.collectAsState()
    val previewSensor by viewModel.cameraPreviewMode.collectAsState()
    val watermarkFlag by viewModel.leicaWatermark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141418))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "LMC MASTER PRO OPTION DECK",
            color = Color(0xFFFDD835),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )

        // 1. Capture Algorithm (HDR Select)
        SegmentedHUDSelector(
            title = "HDR+ PIPELINE",
            icon = Icons.Outlined.HdrOn,
            selected = hdr,
            options = listOf("HDR+ Off", "HDR+ On", "HDR+ Enhanced"),
            onSelected = { viewModel.setHdrMode(it) }
        )

        // 2. Camera Color presets type
        SegmentedHUDSelector(
            title = "LEICA COLOR TRANSFORM MATRIX",
            icon = Icons.Outlined.Palette,
            selected = style,
            options = listOf("Classic", "Vibrant", "Dark", "Noir"),
            onSelected = { viewModel.setLeicaStyle(it) }
        )

        // 3. Grid Lines Guide Overlays
        SegmentedHUDSelector(
            title = "VIEWFINDER GRID LINES",
            icon = Icons.Outlined.GridOn,
            selected = grid,
            options = listOf("None", "3x3", "Golden_Ratio", "Diagonal"),
            onSelected = { viewModel.setGridMode(it) }
        )

        // 4. Double column secondary settings switches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Level Horizon bubble switch
            Button(
                onClick = { viewModel.setShowLevelIndicator(!showLevel) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showLevel) Color(0xFFFDD835).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                    contentColor = if (showLevel) Color(0xFFFDD835) else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        if (showLevel) Color(0xFFFDD835).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = if (showLevel) Icons.Default.FilterCenterFocus else Icons.Default.FilterCenterFocus,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showLevel) " horizonte ON " else " horizonte OFF ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Watermark Toggle switch
            Button(
                onClick = { viewModel.setLeicaWatermark(!watermarkFlag) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (watermarkFlag) Color(0xFFFDD835).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                    contentColor = if (watermarkFlag) Color(0xFFFDD835) else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        if (watermarkFlag) Color(0xFFFDD835).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Loyalty,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (watermarkFlag) "WATERMARK ON" else "WATERMARK OFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Hardware Sensor Select: Physical vs Simulator mode
        SegmentedHUDSelector(
            title = "VIEWFINDER TARGET SENSOR",
            icon = Icons.Outlined.PhotoCamera,
            selected = if (previewSensor == "Real") "PHYSICAL" else "SIMULATOR",
            options = listOf("SIMULATOR", "PHYSICAL"),
            onSelected = {
                viewModel.setPreviewMode(if (it == "PHYSICAL") "Real" else "Simulator")
            }
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ----------------------------------------------------
// Custom Segmented Option bar wrapper
// ----------------------------------------------------
@Composable
fun SegmentedHUDSelector(
    title: String,
    icon: ImageVector,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(13.dp))
            Text(text = title, color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { opt ->
                val isSelected = selected.lowercase().trim() == opt.lowercase().trim()
                val bg = if (isSelected) Color(0xFFFDD835) else Color.Transparent
                val borderCol = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent
                val tc = if (isSelected) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                        .clickable { onSelected(opt) }
                        .padding(vertical = 8.dp)
                        .testTag("segment_${title.replace(" ", "_")}_$opt"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt.uppercase(),
                        color = tc,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
