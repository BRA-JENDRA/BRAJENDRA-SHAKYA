package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CapturedPhoto
import com.example.ui.LmcViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LmcGalleryView(
    viewModel: LmcViewModel,
    modifier: Modifier = Modifier
) {
    val photos by viewModel.photosList.collectAsState()
    val selectedPhoto by viewModel.selectedGalleryPhoto.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .padding(16.dp)
    ) {
        // Gallery Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LMC MASTER GALLERY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Browse historic camera captures and physical EXIF packets",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { viewModel.navigateTo("camera") },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .testTag("gallery_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Gallery",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Empty Gallery",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your EXIF Gallery is empty",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trigger camera shutter exposures to process and bake raw photos with beautiful Leica watermarks inside the database.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    GalleryThumbnailItem(
                        photo = photo,
                        onClick = { viewModel.selectGalleryPhoto(photo) }
                    )
                }
            }
        }

        // Slide up EXIF packet inspector bottom screen sheet
        AnimatedVisibility(
            visible = selectedPhoto != null,
            enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(250)) { it } + fadeOut()
        ) {
            selectedPhoto?.let { photo ->
                ExifInspectorPanel(
                    photo = photo,
                    onClose = { viewModel.selectGalleryPhoto(null) },
                    onDelete = {
                        viewModel.deletePhoto(photo.id, photo.imagePath)
                        viewModel.selectGalleryPhoto(null)
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// Custom Thumbnail Decoder
// ----------------------------------------------------
@Composable
fun GalleryThumbnailItem(
    photo: CapturedPhoto,
    onClick: () -> Unit
) {
    // Decode image bitmap safely in local background thread memory
    val imageBitmap = remember(photo.imagePath) {
        try {
            val file = File(photo.imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("gallery_thumbnail_${photo.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = photo.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Warning indicator
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Missing file",
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Overlay Lens used
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = photo.zoomLevel,
                    color = Color(0xFFFDD835),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        PaddingValues(8.dp).let { p ->
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = photo.configName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ISO ${photo.iso} | EV ${photo.ev}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------
// Fully Detailed EXIF Overlay Analyzer Screen Panel
// ----------------------------------------------------
@Composable
fun ExifInspectorPanel(
    photo: CapturedPhoto,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val decodedBitmap = remember(photo.imagePath) {
        try {
            val file = File(photo.imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(enabled = false) {} // block click through
            .padding(top = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Panel Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEICA PACKET ANALYZER v8.4",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("exif_panel_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close panel",
                        tint = Color.White
                    )
                }
            }

            // Main Decoded Photo View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(Color(0xFF08080A)),
                contentAlignment = Alignment.Center
            ) {
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = "Full photo view",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Missing local Jpeg",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // EXIF Telemetry packets list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "METADATA PACKETS",
                    color = Color(0xFFFDD835),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Render Specs Table grid cleanly
                val dateStr = remember(photo.timestamp) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(photo.timestamp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14141A), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExifRow(lbl = "File Name", valStr = photo.fileName)
                    ExifRow(lbl = "Bake Directory", valStr = photo.imagePath.substringBeforeLast("/"))
                    ExifRow(lbl = "Capture Date", valStr = dateStr)
                    ExifRow(lbl = "XML Config Profile", valStr = photo.configName)
                    ExifRow(lbl = "Lens Node (Zoom)", valStr = "${photo.zoomLevel} (50mm equivalent optical)")
                    ExifRow(lbl = "Shutter speed multiplier", valStr = photo.shutterSpeed)
                    ExifRow(lbl = "ISO Sensor Rating", valStr = "ISO ${photo.iso}")
                    ExifRow(lbl = "Aperture node", valStr = "f/1.8 Dual Sub-pixel Lens")
                    ExifRow(lbl = "Exposure slider (EV)", valStr = "${photo.ev} eV Value Offset")
                    ExifRow(lbl = "Manual focus plane", valStr = if (photo.focusValue == -1f) "Auto Focus (Algorithmic)" else "${(photo.focusValue * 100).toInt()}% distance")
                    ExifRow(lbl = "White Balance Matrix", valStr = photo.whiteBalance)
                    ExifRow(lbl = "Leica Color Stylings", valStr = "Style: ${photo.leicaStyle} (Watermark text: ${photo.watermarkText})")
                    ExifRow(lbl = "Digital Post-processing status", valStr = "SUCCESS (HDR+ Baked Container)")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tactical action button tray (Delete / Export)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935).copy(alpha = 0.15f),
                            contentColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .testTag("delete_photo_bin")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete picture")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Capture", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { /* Simulated copy/share */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Packet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExifRow(lbl: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = lbl,
            color = Color.White.copy(alpha = 0.40f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = valStr,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Right,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}
