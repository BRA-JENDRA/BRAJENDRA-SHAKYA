package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LmcConfig::class, CapturedPhoto::class], version = 1, exportSchema = false)
abstract class LmcDatabase : RoomDatabase() {

    abstract fun lmcDao(): LmcDao

    companion object {
        @Volatile
        private var INSTANCE: LmcDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): LmcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LmcDatabase::class.java,
                    "lmc_camera_database"
                )
                .addCallback(LmcDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LmcDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialConfigs(database.lmcDao())
                }
            }
        }

        private suspend fun populateInitialConfigs(dao: LmcDao) {
            val systemPresets = listOf(
                LmcConfig(
                    id = 1,
                    name = "LMC_Official_Auto.xml",
                    description = "Well-balanced general profile designed for natural daylight and quick capturing.",
                    ev = 0.0f,
                    iso = -1,
                    shutterSpeed = "Auto",
                    focusValue = -1.0f,
                    saturation = 0,
                    contrast = 0,
                    sharpness = 50,
                    vignetteAmount = 0,
                    whiteBalanceMode = "Auto",
                    hdrMode = "HDR+ On",
                    leicaStyle = "Classic",
                    leicaWatermark = true,
                    watermarkText = "LMC 8.4 • AUTO SENSOR",
                    isSystemPreset = true
                ),
                LmcConfig(
                    id = 2,
                    name = "Leica_M11_Classic.xml",
                    description = "Emulates high-contrast Leica German optics. Rich shadows, deep colors, and classic matte vignette.",
                    ev = -0.3f,
                    iso = 100,
                    shutterSpeed = "Auto",
                    focusValue = -1.0f,
                    saturation = -10,
                    contrast = 25,
                    sharpness = 65,
                    vignetteAmount = 30,
                    whiteBalanceMode = "Auto",
                    hdrMode = "HDR+ Enhanced",
                    leicaStyle = "Classic",
                    leicaWatermark = true,
                    watermarkText = "LEICA CAMERA • M11 SPECIAL",
                    isSystemPreset = true
                ),
                LmcConfig(
                    id = 3,
                    name = "HDR_Vibrant_Pro.xml",
                    description = "Ultra high dynamic range profile with stunning shadow recovery and punchy cinematic color channels.",
                    ev = 0.0f,
                    iso = -1,
                    shutterSpeed = "Auto",
                    focusValue = -1.0f,
                    saturation = 30,
                    contrast = 15,
                    sharpness = 60,
                    vignetteAmount = 10,
                    whiteBalanceMode = "Sunny",
                    hdrMode = "HDR+ Enhanced",
                    leicaStyle = "Vibrant",
                    leicaWatermark = true,
                    watermarkText = "LMC 8.4 • VIBRANT HDR+ PRO",
                    isSystemPreset = true
                ),
                LmcConfig(
                    id = 4,
                    name = "Astro_NightSight.xml",
                    description = "Engineered for astrophotography. Extreme exposure, noise-reduction smoothing, cosmic blue white balance.",
                    ev = 1.5f,
                    iso = 1600,
                    shutterSpeed = "2s",
                    focusValue = 1.0f, // Infinite focus plane
                    saturation = 15,
                    contrast = 5,
                    sharpness = 40,
                    vignetteAmount = 15,
                    whiteBalanceMode = "Fluorescent", // cool tone
                    hdrMode = "HDR+ Enhanced",
                    leicaStyle = "Vibrant",
                    leicaWatermark = true,
                    watermarkText = "LMC ASTRO • LONG EXPOSURE",
                    isSystemPreset = true
                ),
                LmcConfig(
                    id = 5,
                    name = "Pro_Macro_Bokeh.xml",
                    description = "Configured for macro details. Emulates shallow aperture depth of field for extreme bokeh highlights.",
                    ev = 0.0f,
                    iso = 100,
                    shutterSpeed = "Auto",
                    focusValue = 0.1f, // Close focus distance
                    saturation = 10,
                    contrast = -5,
                    sharpness = 75,
                    vignetteAmount = 20,
                    whiteBalanceMode = "Cloudy",
                    hdrMode = "HDR+ Off",
                    leicaStyle = "Classic",
                    leicaWatermark = false,
                    watermarkText = "LMC • MACRO COMPACT",
                    isSystemPreset = true
                ),
                LmcConfig(
                    id = 6,
                    name = "Noir_Monochrome.xml",
                    description = "Beautiful high-contrast black and white street photography style. Zero color noise.",
                    ev = -0.5f,
                    iso = 200,
                    shutterSpeed = "1/250s",
                    focusValue = -1.0f,
                    saturation = -100,
                    contrast = 35,
                    sharpness = 85,
                    vignetteAmount = 40,
                    whiteBalanceMode = "Auto",
                    hdrMode = "HDR+ On",
                    leicaStyle = "Noir",
                    leicaWatermark = true,
                    watermarkText = "LEICA NOIR • STREET EDITION",
                    isSystemPreset = true
                )
            )

            for (preset in systemPresets) {
                dao.insertConfig(preset)
            }
        }
    }
}
