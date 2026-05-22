package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_photos")
data class CapturedPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,              // File URI or path in application storage
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val configName: String = "Auto XML",
    val zoomLevel: String = "1.0x",
    val ev: Float = 0.0f,
    val iso: Int = 100,
    val shutterSpeed: String = "Auto",
    val focusValue: Float = -1.0f,
    val whiteBalance: String = "Auto",
    val leicaStyle: String = "Classic",
    val watermarkText: String = "",
    val hasWatermark: Boolean = true,
    // Simulation rendering values to accurately load thumbnail details
    val hueShift: Float = 0.0f,
    val saturation: Float = 1.0f,
    val contrast: Float = 1.0f,
    val sharpness: Float = 1.0f,
    val noiseAmount: Float = 0.0f,
    val blurAmount: Float = 0.0f,
    val vignetteAmount: Float = 0.0f
)
