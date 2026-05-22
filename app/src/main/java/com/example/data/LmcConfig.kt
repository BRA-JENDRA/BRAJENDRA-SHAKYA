package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lmc_configs")
data class LmcConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val ev: Float = 0.0f, // Exposure adjustments -3.0 to +3.0
    val iso: Int = -1,    // -1 for Auto, or manual 50, 100, 200, 400, 800, 1600, 3200
    val shutterSpeed: String = "Auto", // Auto or specific fraction e.g. "1/125s"
    val focusValue: Float = -1.0f,     // -1.0 for Auto Focus, or 0.0 to 1.0 for Manual Focus
    val saturation: Int = 0,           // -100 to 100 adjustment
    val contrast: Int = 0,             // -100 to 100 adjustment
    val sharpness: Int = 50,           // 0 to 100
    val vignetteAmount: Int = 0,       // 0 to 100
    val whiteBalanceMode: String = "Auto", // Auto, Sunny, Cloudy, Incandescent, Fluorescent
    val hdrMode: String = "HDR+ Enhanced", // HDR+ Off, HDR+ On, HDR+ Enhanced
    val leicaStyle: String = "Classic",    // Classic, Vibrant, Dark, Noir
    val leicaWatermark: Boolean = true,
    val watermarkText: String = "LMC 8.4 KEYSTONE EDITION",
    val isSystemPreset: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
