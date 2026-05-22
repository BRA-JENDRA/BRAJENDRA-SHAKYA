package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LmcViewModel
import kotlin.math.abs

// ----------------------------------------------------
// Auxiliary Lens Toggle (0.6x, 1.0x, 2.0x, 5.0x)
// ----------------------------------------------------
@Composable
fun LmcAuxLensSelector(
    selectedLens: String,
    onLensSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lenses = listOf("0.6x", "1.0x", "2.0x", "5.0x")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lenses.forEach { lens ->
            val isSelected = selectedLens == lens
            val labelColor = if (isSelected) Color.Black else Color.White
            val bgColor = if (isSelected) Color(0xFFFDD835) else Color.Transparent // Leica yellow highlight

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onLensSelected(lens) }
                    .testTag("lens_button_$lens"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lens.substringBefore("x"),
                    color = labelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------
// Digital Accel Level Meter Overlay
// ----------------------------------------------------
@Composable
fun LmcLevelGauge(
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    // Determine level alignment tolerance
    val isLeveled = abs(roll) < 1.8f && abs(pitch) < 1.8f
    val lineColor = if (isLeveled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
    val indicatorOffsetMax = 35f

    Box(
        modifier = modifier
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Center target circle
            drawCircle(
                color = lineColor,
                radius = 12f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            // Inner level bubble point
            val bx = (cx + (roll.coerceIn(-20f, 20f) / 20f) * indicatorOffsetMax)
            val by = (cy + (pitch.coerceIn(-20f, 20f) / 20f) * indicatorOffsetMax)
            drawCircle(
                color = if (isLeveled) Color(0xFF4CAF50) else Color(0xFFFDD835),
                radius = 6f
            )

            // Horizontal Horizon Axis indicator line (tilts based on roll value using math trig coordinate rotation)
            val rollRad = Math.toRadians(roll.toDouble())
            val cosR = kotlin.math.cos(rollRad).toFloat()
            val sinR = kotlin.math.sin(rollRad).toFloat()

            // Left pointer line
            drawLine(
                color = lineColor,
                start = Offset(cx - 45f * cosR, cy - 45f * sinR),
                end = Offset(cx - 15f * cosR, cy - 15f * sinR),
                strokeWidth = 3f
            )
            // Right pointer line
            drawLine(
                color = lineColor,
                start = Offset(cx + 15f * cosR, cy + 15f * sinR),
                end = Offset(cx + 45f * cosR, cy + 45f * sinR),
                strokeWidth = 3f
            )
        }
    }
}

// ----------------------------------------------------
// Grid Overlays
// ----------------------------------------------------
@Composable
fun LmcGridOverlay(gridMode: String) {
    if (gridMode == "None") return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val linePaintColor = Color.White.copy(alpha = 0.35f)
        val strokeWidthVal = 1.5.dp.toPx()

        when (gridMode) {
            "3x3" -> {
                // Horizontal lines
                drawLine(linePaintColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeWidthVal)
                drawLine(linePaintColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = strokeWidthVal)
                // Vertical lines
                drawLine(linePaintColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeWidthVal)
                drawLine(linePaintColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = strokeWidthVal)
            }
            "Golden_Ratio" -> {
                val ratio = 0.382f
                // Horizontal
                drawLine(linePaintColor, Offset(0f, h * ratio), Offset(w, h * ratio), strokeWidth = strokeWidthVal)
                drawLine(linePaintColor, Offset(0f, h * (1f - ratio)), Offset(w, h * (1f - ratio)), strokeWidth = strokeWidthVal)
                // Vertical
                drawLine(linePaintColor, Offset(w * ratio, 0f), Offset(w * ratio, h), strokeWidth = strokeWidthVal)
                drawLine(linePaintColor, Offset(w * (1f - ratio), 0f), Offset(w * (1f - ratio), h), strokeWidth = strokeWidthVal)
            }
            "Diagonal" -> {
                drawLine(linePaintColor, Offset(0f, 0f), Offset(w, h), strokeWidth = strokeWidthVal)
                drawLine(linePaintColor, Offset(w, 0f), Offset(0f, h), strokeWidth = strokeWidthVal)
            }
        }
    }
}

// ----------------------------------------------------
// Circular Dial Slider / Vertical Pro slider
// ----------------------------------------------------
@Composable
fun LmcVerticalSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    displayValue: String = String.format("%.1f", value)
) {
    Column(
        modifier = modifier
            .width(55.dp)
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFFFDD835),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFFFDD835),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    thumbColor = Color.White
                ),
                modifier = Modifier
                    .rotate(-90f)
                    .height(30.dp)
                    .width(180.dp)
                    .testTag("pro_slider_$label")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = displayValue,
            color = Color.White,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ----------------------------------------------------
// Tactical Dial / Shutter Wheel selection
// ----------------------------------------------------
@Composable
fun LmcShutterValueSelector(
    selectedVal: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(options) { opt ->
            val isSelected = selectedVal == opt
            val itemBg = if (isSelected) Color(0xFFE53935).copy(alpha = 0.2f) else Color.Transparent
            val borderCol = if (isSelected) Color(0xFFE53935) else Color.White.copy(alpha = 0.15f)
            val txtColor = if (isSelected) Color(0xFFE53935) else Color.White

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(itemBg)
                    .border(1.dp, borderCol, RoundedCornerShape(4.dp))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("shutter_wheel_opt_$opt"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    color = txtColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------
// Professional Camera Mode Carousel
// ----------------------------------------------------
@Composable
fun LmcModeCarousel(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("Camera", "Portrait", "Night Sight", "Pro")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw a neat under-glow horizontal bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { mode ->
                val isSelected = currentMode == mode
                val textColor = if (isSelected) Color(0xFFFDD835) else Color.White.copy(alpha = 0.5f)
                val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                val indicatorAlpha = if (isSelected) 1f else 0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onModeSelected(mode) }
                        .testTag("mode_tab_$mode")
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.uppercase(),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = textWeight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 3.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFFDD835).copy(alpha = indicatorAlpha))
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Beautiful Camera Shutter Press Trigger Button
// ----------------------------------------------------
@Composable
fun LmcShutterButton(
    onClick: () -> Unit,
    isCapturing: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shutter")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Box(
        modifier = modifier
            .size(86.dp)
            .testTag("shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        // Progress boundary ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = size.width / 2f - 4f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // Active processing progress drawing
            if (isCapturing && progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFFFDD835), Color(0xFFE53935), Color(0xFFFDD835))),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }

        // Inner Shutter Dot
        val scale = if (isCapturing) breathingScale else 1.0f
        val colorInterior = if (isCapturing) Color(0xFFE53935) else Color.White

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colorInterior)
                    .clickable(enabled = !isCapturing) { onClick() }
            )
        }
    }
}
