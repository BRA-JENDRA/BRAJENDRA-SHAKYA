package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

class LmcViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val db = LmcDatabase.getDatabase(application, viewModelScope)
    private val repository = LmcRepository(db.lmcDao())

    // State flows from Room
    val configsList: StateFlow<List<LmcConfig>> = repository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photosList: StateFlow<List<CapturedPhoto>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current navigation state
    private val _currentScreen = MutableStateFlow("camera") // "camera", "configs", "gallery"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Active configuration state
    private val _activeConfigName = MutableStateFlow("LMC_Official_Auto.xml")
    val activeConfigName: StateFlow<String> = _activeConfigName.asStateFlow()

    // Camera Mode (Camera, Portrait, Night Sight, Pro Mode)
    private val _cameraMode = MutableStateFlow("Camera") // "Camera", "Portrait", "Night Sight", "Pro"
    val cameraMode: StateFlow<String> = _cameraMode.asStateFlow()

    // General controls
    private val _zoomLevel = MutableStateFlow("1.0x") // "0.6x", "1.0x", "2.0x", "5.0x"
    val zoomLevel: StateFlow<String> = _zoomLevel.asStateFlow()

    private val _ev = MutableStateFlow(0.0f) // -3.0f to +3.0f
    val ev: StateFlow<Float> = _ev.asStateFlow()

    private val _iso = MutableStateFlow(-1) // -1 is Auto, 50, 100, 200, 400, 800, 1600, 3200
    val iso: StateFlow<Int> = _iso.asStateFlow()

    private val _shutterSpeed = MutableStateFlow("Auto") // "Auto", "1/1000s", etc.
    val shutterSpeed: StateFlow<String> = _shutterSpeed.asStateFlow()

    private val _focusValue = MutableStateFlow(-1.0f) // -1f is Auto Focus, 0f is Macro, 1f is Infinite
    val focusValue: StateFlow<Float> = _focusValue.asStateFlow()

    // Fine color tuning parameters
    private val _saturation = MutableStateFlow(0) // -100 to 100
    val saturation: StateFlow<Int> = _saturation.asStateFlow()

    private val _contrast = MutableStateFlow(0)
    val contrast: StateFlow<Int> = _contrast.asStateFlow()

    private val _sharpness = MutableStateFlow(50) // 0 to 100
    val sharpness: StateFlow<Int> = _sharpness.asStateFlow()

    private val _vignetteAmount = MutableStateFlow(0)
    val vignetteAmount: StateFlow<Int> = _vignetteAmount.asStateFlow()

    private val _whiteBalanceMode = MutableStateFlow("Auto") // Auto, Sunny, Cloudy, Incandescent, Fluorescent
    val whiteBalanceMode: StateFlow<String> = _whiteBalanceMode.asStateFlow()

    private val _hdrMode = MutableStateFlow("HDR+ On") // HDR+ Off, HDR+ On, HDR+ Enhanced
    val hdrMode: StateFlow<String> = _hdrMode.asStateFlow()

    private val _leicaStyle = MutableStateFlow("Classic") // Classic, Vibrant, Dark, Noir
    val leicaStyle: StateFlow<String> = _leicaStyle.asStateFlow()

    private val _leicaWatermark = MutableStateFlow(true)
    val leicaWatermark: StateFlow<Boolean> = _leicaWatermark.asStateFlow()

    private val _watermarkText = MutableStateFlow("LMC 8.4 • LEICA SYSTEM")
    val watermarkText: StateFlow<String> = _watermarkText.asStateFlow()

    // Top Settings Drawer
    private val _isSettingsDrawerOpen = MutableStateFlow(false)
    val isSettingsDrawerOpen: StateFlow<Boolean> = _isSettingsDrawerOpen.asStateFlow()

    private val _gridMode = MutableStateFlow("None") // "None", "3x3", "Golden_Ratio", "Diagonal"
    val gridMode: StateFlow<String> = _gridMode.asStateFlow()

    private val _showLevelIndicator = MutableStateFlow(true)
    val showLevelIndicator: StateFlow<Boolean> = _showLevelIndicator.asStateFlow()

    private val _cameraPreviewMode = MutableStateFlow("Simulator") // "Real" (CameraX) or "Simulator" (AI scene simulator)
    val cameraPreviewMode: StateFlow<String> = _cameraPreviewMode.asStateFlow()

    // Dynamic Sensor values for level gauge
    private val _levelRoll = MutableStateFlow(0.0f)
    val levelRoll: StateFlow<Float> = _levelRoll.asStateFlow()

    private val _levelPitch = MutableStateFlow(0.0f)
    val levelPitch: StateFlow<Float> = _levelPitch.asStateFlow()

    // Capturing & Processing State
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _captureProgress = MutableStateFlow(0.0f)
    val captureProgress: StateFlow<Float> = _captureProgress.asStateFlow()

    private val _captureProgressText = MutableStateFlow("")
    val captureProgressText: StateFlow<String> = _captureProgressText.asStateFlow()

    // Simulated Scenery Index
    private val _simulatorSceneIndex = MutableStateFlow(0) // 0: Macro Subject, 1: Sunset Lake, 2: Cyberpunk Street
    val simulatorSceneIndex: StateFlow<Int> = _simulatorSceneIndex.asStateFlow()

    // Photo details to display in EXIF bottom sheet
    private val _selectedGalleryPhoto = MutableStateFlow<CapturedPhoto?>(null)
    val selectedGalleryPhoto: StateFlow<CapturedPhoto?> = _selectedGalleryPhoto.asStateFlow()

    // Sensor Manager
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    init {
        // Setup initial physical sensors if available
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        startSensors()
    }

    // Navigation
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        _selectedGalleryPhoto.value = null
    }

    // Toggle Camera Settings Drawer
    fun toggleSettingsDrawer() {
        _isSettingsDrawerOpen.value = !_isSettingsDrawerOpen.value
    }

    // Cycle through Simulator scenes
    fun cycleSimulatorScene() {
        _simulatorSceneIndex.value = (_simulatorSceneIndex.value + 1) % 3
    }

    // Switch between real camera & simulator preview
    fun setPreviewMode(mode: String) {
        _cameraPreviewMode.value = mode
    }

    // Set Slider values manually
    fun setEv(value: Float) { _ev.value = value }
    fun setIso(value: Int) { _iso.value = value }
    fun setShutterSpeed(value: String) { _shutterSpeed.value = value }
    fun setFocusValue(value: Float) { _focusValue.value = value }
    fun setZoom(value: String) { _zoomLevel.value = value }

    fun setSaturation(value: Int) { _saturation.value = value }
    fun setContrast(value: Int) { _contrast.value = value }
    fun setSharpness(value: Int) { _sharpness.value = value }
    fun setVignetteAmount(value: Int) { _vignetteAmount.value = value }

    fun setWhiteBalance(value: String) { _whiteBalanceMode.value = value }
    fun setHdrMode(value: String) { _hdrMode.value = value }
    fun setLeicaStyle(value: String) { _leicaStyle.value = value }
    fun setLeicaWatermark(value: Boolean) { _leicaWatermark.value = value }
    fun setWatermarkText(value: String) { _watermarkText.value = value }
    fun setGridMode(value: String) { _gridMode.value = value }
    fun setShowLevelIndicator(value: Boolean) { _showLevelIndicator.value = value }

    fun setCameraMode(mode: String) {
        _cameraMode.value = mode
        _isSettingsDrawerOpen.value = false
        // Automatically default some sliders to improve UX in typical camera modes
        when (mode) {
            "Night Sight" -> {
                _iso.value = 1600
                _shutterSpeed.value = "2s"
                _hdrMode.value = "HDR+ Enhanced"
            }
            "Portrait" -> {
                _focusValue.value = 0.2f // force soft background defocus
                _hdrMode.value = "HDR+ On"
            }
            "Camera" -> {
                _iso.value = -1
                _shutterSpeed.value = "Auto"
            }
        }
    }

    // Load an XML Preset completely from database
    fun loadConfigPreset(config: LmcConfig) {
        _activeConfigName.value = config.name
        _ev.value = config.ev
        _iso.value = config.iso
        _shutterSpeed.value = config.shutterSpeed
        _focusValue.value = config.focusValue
        _saturation.value = config.saturation
        _contrast.value = config.contrast
        _sharpness.value = config.sharpness
        _vignetteAmount.value = config.vignetteAmount
        _whiteBalanceMode.value = config.whiteBalanceMode
        _hdrMode.value = config.hdrMode
        _leicaStyle.value = config.leicaStyle
        _leicaWatermark.value = config.leicaWatermark
        _watermarkText.value = config.watermarkText
    }

    // Save current slider calibrations as a custom XML configuration
    fun saveAsCustomConfig(name: String, description: String, watermark: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val formattedName = if (name.endsWith(".xml")) name else "$name.xml"
            val customConfig = LmcConfig(
                name = formattedName,
                description = description,
                ev = _ev.value,
                iso = _iso.value,
                shutterSpeed = _shutterSpeed.value,
                focusValue = _focusValue.value,
                saturation = _saturation.value,
                contrast = _contrast.value,
                sharpness = _sharpness.value,
                vignetteAmount = _vignetteAmount.value,
                whiteBalanceMode = _whiteBalanceMode.value,
                hdrMode = _hdrMode.value,
                leicaStyle = _leicaStyle.value,
                leicaWatermark = _leicaWatermark.value,
                watermarkText = watermark.ifEmpty { _watermarkText.value },
                isSystemPreset = false
            )
            repository.insertConfig(customConfig)
            _activeConfigName.value = formattedName
        }
    }

    // Delete custom XML config
    fun deleteConfig(configId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConfigById(configId)
        }
    }

    // EXIF Inspector Selection
    fun selectGalleryPhoto(photo: CapturedPhoto?) {
        _selectedGalleryPhoto.value = photo
    }

    // Delete Captured Photo
    fun deletePhoto(photoId: Int, imagePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete record from database
            repository.deletePhotoById(photoId)
            // Delete binary image file from storage
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Simulate Taking a photo (GCam processing simulator and bitmap generator)
    fun capturePhoto() {
        if (_isCapturing.value) return
        _isCapturing.value = true
        _captureProgress.value = 0.0f
        
        viewModelScope.launch(Dispatchers.Default) {
            // Simulated stage calculations depending on HDR Mode & Camera Mode
            val delayInterval = 100L
            val steps: List<Pair<Float, String>> = when (_cameraMode.value) {
                "Night Sight" -> listOf(
                    0.1f to "Capturing long exposure (Do not move)...",
                    0.25f to "Merging astronomical sensor tiles...",
                    0.5f to "Calculating dual-layer noise reduction...",
                    0.7f to "Recovering dynamic shadow profiles...",
                    0.9f to "Synthesizing Leica atmospheric tone...",
                    1.0f to "Writing JPEG container..."
                )
                "Portrait" -> listOf(
                     0.15f to "Capturing portrait session raw frames...",
                     0.35f to "Calculating neural depth map matrix...",
                     0.6f to "Simulating sub-pixel physical bokeh blur...",
                     0.8f to "Polishing Leica skin-tones and detail...",
                     1.0f to "Writing JPEG container..."
                )
                else -> {
                     if (_hdrMode.value == "HDR+ Enhanced") {
                         listOf(
                             0.1f to "HDR+ Enhanced: Capturing 12 frames...",
                             0.4f to "Aligning raw sub-exposures...",
                             0.65f to "Resolving sub-pixel micro details...",
                             0.85f to "Mapping localized tone contrast...",
                             1.0f to "Writing JPEG container..."
                         )
                     } else {
                         listOf(
                             0.2f to "Capturing standard exposure frame...",
                             0.5f to "Developing raw sensor details...",
                             0.8f to "Applying color transform matrix...",
                             1.0f to "Writing JPEG container..."
                         )
                     }
                }
            }

            for (step in steps) {
                while (_captureProgress.value < step.first) {
                    _captureProgress.value += 0.05f
                    delay(50L)
                }
                _captureProgressText.value = step.second
                delay(delayInterval)
            }

            // Real physical capture processing (generate JPEG with EXIF watermark baked-in)
            val path = withContext(Dispatchers.IO) {
                generateAndSaveSimulatedImage()
            }

            // Add to database
            if (path != null) {
                val filename = File(path).name
                val realIso = if (_iso.value == -1) (100..400).random() else _iso.value
                val realShutter = if (_shutterSpeed.value == "Auto") "1/125s" else _shutterSpeed.value
                val realFocus = if (_focusValue.value == -1.0f) 0.5f else _focusValue.value

                val photo = CapturedPhoto(
                    imagePath = path,
                    fileName = filename,
                    configName = _activeConfigName.value,
                    zoomLevel = _zoomLevel.value,
                    ev = _ev.value,
                    iso = realIso,
                    shutterSpeed = realShutter,
                    focusValue = realFocus,
                    whiteBalance = _whiteBalanceMode.value,
                    leicaStyle = _leicaStyle.value,
                    watermarkText = _watermarkText.value,
                    hasWatermark = _leicaWatermark.value,
                    hueShift = when (_whiteBalanceMode.value) {
                        "Sunny" -> 15.0f
                        "Fluorescent" -> -20.0f
                        "Cloudy" -> 8.0f
                        "Incandescent" -> -10.0f
                        else -> 0.0f
                    },
                    saturation = (100 + _saturation.value) / 100.0f,
                    contrast = (100 + _contrast.value) / 100.0f,
                    sharpness = _sharpness.value / 100.0f,
                    noiseAmount = if (_cameraMode.value == "Night Sight") 0.05f else (realIso.toFloat() / 3200f) * 0.3f,
                    blurAmount = if (_cameraMode.value == "Portrait" || realFocus < 0.3f) 0.15f else 0.0f,
                    vignetteAmount = _vignetteAmount.value / 100.0f
                )

                _selectedGalleryPhoto.value = photo
                repository.insertPhoto(photo)
            }

            _isCapturing.value = false
            _captureProgress.value = 0.0f
            _captureProgressText.value = ""
        }
    }

    // Helper that generates a gorgeous custom high-def simulated visual bitmap corresponding
    // to all specific sliders and configurations, adds a pristine classic LEICA watermark band
    // at the bottom, compresses it to JPEG, and returns the absolute local filepath.
    private suspend fun generateAndSaveSimulatedImage(): String? {
        val app = getApplication<Application>()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = "LMC_IMG_$timeStamp.jpg"
        val imageFile = File(app.filesDir, name)

        try {
            // Image Dimensions (high def)
            val w = 1200
            val h = 1600
            val watermarkHeight = 200
            val imageHeight = h - watermarkHeight

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Draw Simulated Scene background depending on selected scene index
            val sceneIdx = _simulatorSceneIndex.value
            val evShift = _ev.value // -3.0f to 3.0f

            // Exposure paint modification
            val exposureFilterOffset = (evShift * 40).toInt()

            val bgPaint = Paint().apply { isAntiAlias = true }
            val detailPaint = Paint().apply { isAntiAlias = true }

            // 1a. Draw Scenery background
            if (sceneIdx == 0) {
                // SCENE 0: Macro Subject (Yellow Flower with Focus blur layers)
                // Sky / Background Garden (depends on focus value if Macro)
                val gardenR = clampColor(65 + exposureFilterOffset)
                val gardenG = clampColor(135 + exposureFilterOffset)
                val gardenB = clampColor(65 + exposureFilterOffset)
                canvas.drawColor(Color.rgb(gardenR, gardenG, gardenB))

                // Midground Leaves (Blurred if focus is Auto or High)
                val leafBlurOffset = if (_focusValue.value > 0.3f) 1 else 0
                val gPaint = Paint().apply {
                    color = Color.rgb(clampColor(40 + exposureFilterOffset), clampColor(105 + exposureFilterOffset), clampColor(40 + exposureFilterOffset))
                    isAntiAlias = true
                }
                // Draw leafy shapes
                canvas.drawOval(100f, 400f, 600f, 900f, gPaint)
                canvas.drawOval(700f, 600f, 1100f, 1100f, gPaint)

                // Foreground Flower Head
                // Red/Orange center: sharp if macro focus is selected (~0.1 to ~0.3)
                val isFlowerSharp = _focusValue.value in 0.0f..0.35f || _focusValue.value == -1.0f
                val flowerPaint = Paint().apply {
                    color = Color.rgb(clampColor(245 + exposureFilterOffset), clampColor(180 + exposureFilterOffset), clampColor(0 + exposureFilterOffset))
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                val centerPaint = Paint().apply {
                    color = Color.rgb(clampColor(95 + exposureFilterOffset), clampColor(55 + exposureFilterOffset), clampColor(15 + exposureFilterOffset))
                    isAntiAlias = true
                }

                // Petals
                val cx = w / 2f
                val cy = imageHeight / 2f + 50
                val petalRadius = if (isFlowerSharp) 250f else 250f
                for (angle in 0 until 360 step 30) {
                    canvas.save()
                    canvas.rotate(angle.toFloat(), cx, cy)
                    canvas.drawOval(cx - 50, cy - petalRadius, cx + 50, cy, flowerPaint)
                    canvas.restore()
                }
                // Center disk
                canvas.drawCircle(cx, cy, 110f, centerPaint)

                // Micro pollen details (only if sharp focus!)
                if (isFlowerSharp) {
                    val pollenPaint = Paint().apply {
                        color = Color.YELLOW
                        isAntiAlias = true
                    }
                    for (i in 0..12) {
                        val angle = i * 30.0
                        val r = 70f
                        val px = cx + r * sin(angle).toFloat()
                        val py = cy + r * sin(angle + 4.0).toFloat()
                        canvas.drawCircle(px, py, 8f, pollenPaint)
                    }
                }

            } else if (sceneIdx == 1) {
                // SCENE 1: Alpine Sunset Lake
                // Sky (Sunset gradient colors modified by WB mode)
                val skyColorTop = when (_whiteBalanceMode.value) {
                    "Sunny" -> Color.rgb(250, 95, 25)
                    "Fluorescent" -> Color.rgb(30, 80, 245)
                    "Cloudy" -> Color.rgb(220, 140, 110)
                    "Incandescent" -> Color.rgb(100, 30, 200)
                    else -> Color.rgb(240, 100, 80)
                }
                val skyColorBottom = Color.rgb(255, 210, 140)

                // Draw gradient blocks manually
                for (y in 0 until imageHeight step 10) {
                    val ratio = y.toFloat() / imageHeight
                    val r = ((1 - ratio) * Color.red(skyColorTop) + ratio * Color.red(skyColorBottom)).toInt()
                    val g = ((1 - ratio) * Color.green(skyColorTop) + ratio * Color.green(skyColorBottom)).toInt()
                    val b = ((1 - ratio) * Color.blue(skyColorTop) + ratio * Color.blue(skyColorBottom)).toInt()
                    val paintTemp = Paint().apply {
                        color = Color.rgb(clampColor(r + exposureFilterOffset), clampColor(g + exposureFilterOffset), clampColor(b + exposureFilterOffset))
                    }
                    canvas.drawRect(0f, y.toFloat(), w.toFloat(), (y + 10).toFloat(), paintTemp)
                }

                // Sun
                val sunPaint = Paint().apply {
                    color = Color.rgb(255, 255, 220)
                    isAntiAlias = true
                }
                canvas.drawCircle(w / 2f, imageHeight / 2f - 50, 100f, sunPaint)

                // Mountains in distant background
                val mountainPaint = Paint().apply {
                    color = Color.rgb(clampColor(50 + exposureFilterOffset), clampColor(30 + exposureFilterOffset), clampColor(70 + exposureFilterOffset))
                    isAntiAlias = true
                }
                val pathMountain = android.graphics.Path().apply {
                    moveTo(0f, imageHeight / 2f + 50)
                    lineTo(250f, imageHeight / 2f - 150)
                    lineTo(450f, imageHeight / 2f + 80)
                    lineTo(700f, imageHeight / 2f - 220)
                    lineTo(950f, imageHeight / 2f + 100)
                    lineTo(w.toFloat(), imageHeight / 2f + 30)
                    lineTo(w.toFloat(), imageHeight.toFloat())
                    lineTo(0f, imageHeight.toFloat())
                    close()
                }
                canvas.drawPath(pathMountain, mountainPaint)

                // Lake reflecting sunset
                val lakePaint = Paint().apply {
                    color = Color.rgb(clampColor(80 + exposureFilterOffset), clampColor(85 + exposureFilterOffset), clampColor(145 + exposureFilterOffset))
                    alpha = 180
                }
                canvas.drawRect(0f, imageHeight / 2f + 100, w.toFloat(), imageHeight.toFloat(), lakePaint)

                // Reflections lines (if long exposure Shutter speed, reflection is extremely smooth!)
                val isShutterLong = _shutterSpeed.value in listOf("1/2s", "1s", "2s")
                val reflectionColor = Color.rgb(clampColor(255 + exposureFilterOffset), clampColor(180 + exposureFilterOffset), clampColor(100 + exposureFilterOffset))
                val refPaint = Paint().apply {
                    color = reflectionColor
                    strokeWidth = 3f
                }
                if (isShutterLong) {
                    // Smooth solid gloss reflecting sun
                    refPaint.alpha = 100
                    canvas.drawRect(w / 2f - 150, imageHeight / 2f + 110, w / 2f + 150, imageHeight.toFloat() - 50, refPaint)
                } else {
                    // Ripply waves
                    for (yRef in (imageHeight / 2f + 120).toInt() until imageHeight step 25) {
                        val length = (yRef - imageHeight / 2f) * 0.4f
                        canvas.drawLine(w / 2f - length, yRef.toFloat(), w / 2f + length, yRef.toFloat(), refPaint)
                    }
                }

            } else {
                // SCENE 2: Cyberpunk Alley (Low Light/Neon)
                // Dark Background with neon signs which pop beautifully with high-contrast
                val deepBlack = clampColor(10 + exposureFilterOffset)
                val deepBlue = clampColor(15 + exposureFilterOffset)
                val deepPurple = clampColor(20 + exposureFilterOffset)
                canvas.drawColor(Color.rgb(deepBlack, deepBlue, deepPurple))

                // Draw Alley walls
                val wallPaint = Paint().apply {
                    color = Color.rgb(clampColor(30 + exposureFilterOffset), clampColor(30 + exposureFilterOffset), clampColor(38 + exposureFilterOffset))
                    isAntiAlias = true
                }
                // Left wall perspective
                val leftWall = android.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(200f, 400f)
                    lineTo(200f, imageHeight - 200f)
                    lineTo(0f, imageHeight.toFloat())
                    close()
                }
                canvas.drawPath(leftWall, wallPaint)

                // Right Wall
                val rightWall = android.graphics.Path().apply {
                    moveTo(w.toFloat(), 0f)
                    lineTo(w - 200f, 400f)
                    lineTo(w - 200f, imageHeight - 200f)
                    lineTo(w.toFloat(), imageHeight.toFloat())
                    close()
                }
                canvas.drawPath(rightWall, wallPaint)

                // Neon signs (Glowing pink/blue)
                val neonPink = Paint().apply {
                    color = Color.rgb(255, 45, 150)
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 12f
                    // simulated glow shadow
                }
                canvas.drawRect(50f, 150f, 150f, 550f, neonPink)

                val neonBlue = Paint().apply {
                    color = Color.rgb(0, 195, 255)
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 10f
                }
                canvas.drawCircle(w - 100f, 300f, 60f, neonBlue)

                // Wet Road Reflection
                val roadPaint = Paint().apply {
                    color = Color.rgb(clampColor(12 + exposureFilterOffset), clampColor(14 + exposureFilterOffset), clampColor(20 + exposureFilterOffset))
                }
                canvas.drawRect(200f, imageHeight - 400f, w - 200f, imageHeight.toFloat(), roadPaint)

                // Neon reflection splash
                val splashPaint = Paint().apply {
                    color = Color.rgb(255, 45, 150)
                    alpha = 70
                    isAntiAlias = true
                }
                canvas.drawOval(220f, imageHeight - 250f, 380f, imageHeight - 100f, splashPaint)
                splashPaint.color = Color.rgb(0, 195, 255)
                canvas.drawOval(w - 380f, imageHeight - 220f, w - 220f, imageHeight - 80f, splashPaint)

                // Render "LMC 8.4" with cyan spray paint stencil style in the middle alley wall
                val textPaint = Paint().apply {
                    color = Color.CYAN
                    textSize = 55f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("LMC 8.4", w / 2f, 500f, textPaint)
            }

            // 2. Adjust colors based on saturation and contrast parameters
            val satRatio = (100 + _saturation.value) / 100.0f
            val conRatio = (100 + _contrast.value) / 100.0f
            // Filter effects implemented programmatically on the bitmap pixels
            // We can iterate some zones to speed up or apply matrices. Since doing full pixel operations in pure Kotlin on a 1.2Mpx image could delay execution, we can do a localized contrast matrix overlay or simple visual layers. To be safe, efficient, and fast without blocking, we can overlay transparent tint screens representing color filters!
            // E.g. Mono/Noir Filter overlay
            if (_leicaStyle.value == "Noir" || _saturation.value == -100) {
                val monoOverlay = Paint().apply {
                    color = Color.BLACK
                    alpha = 210 // high density grayscale mix
                    // Using ColorFilter matrix would be ideal, we can set grayscale color filter
                    colorFilter = android.graphics.ColorMatrixColorFilter(
                        android.graphics.ColorMatrix().apply { setSaturation(0f) }
                    )
                }
                // Redraw bitmap content with gray filter
                canvas.drawBitmap(bitmap, 0f, 0f, monoOverlay)
            } else if (_leicaStyle.value == "Classic") {
                // Leica Classic adds subtle vintage yellow-warmth and deep vignette
                val classicOverlay = Paint().apply {
                    color = Color.rgb(255, 200, 100)
                    alpha = 15
                }
                canvas.drawRect(0f, 0f, w.toFloat(), imageHeight.toFloat(), classicOverlay)
            } else if (_leicaStyle.value == "Dark") {
                // Deep low-key lighting overlay
                val lowKey = Paint().apply {
                    color = Color.BLACK
                    alpha = 40
                }
                canvas.drawRect(0f, 0f, w.toFloat(), imageHeight.toFloat(), lowKey)
            }

            // 3. Add High-ISO Pixelated Color Noise / Grain (only if ISO is high)
            val isoVal = if (_iso.value == -1) 100 else _iso.value
            if (isoVal > 400 && _cameraMode.value != "Night Sight") {
                val random = Random()
                val grainDensity = (isoVal.toFloat() / 3200f) * 0.15f // noise ratio
                val noisePaint = Paint().apply { strokeWidth = 2f }
                
                // Draw sparse randomized noise pixels
                for (chunk in 0..1500) {
                    val nx = random.nextInt(w).toFloat()
                    val ny = random.nextInt(imageHeight).toFloat()
                    noisePaint.color = if (random.nextBoolean()) Color.WHITE else Color.rgb(random.nextInt(255), 10, random.nextInt(255))
                    noisePaint.alpha = (grainDensity * 255).toInt()
                    canvas.drawPoint(nx, ny, noisePaint)
                }
            }

            // 4. Soft Vignette shade (GCam/Leica favorite)
            val vigVal = _vignetteAmount.value
            if (vigVal > 0) {
                // Draw radial dark vignette shadow block
                val radius = w.coerceAtLeast(imageHeight) * 0.85f
                val vignPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.BLACK
                    alpha = (vigVal * 1.8f).toInt().coerceIn(0, 230)
                }
                // Custom simple vignette around frame edges using safe circular drawing
                for (edge in 0..vigVal step 5) {
                    vignPaint.alpha = (edge * 2)
                    val strokeW = (vigVal - edge) * 2.5f
                    val vignetteStrokePaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW
                        isAntiAlias = true
                        alpha = (edge * 1.2f).toInt().coerceIn(0, 200)
                    }
                    canvas.drawRect(
                        strokeW / 2, 
                        strokeW / 2, 
                        w.toFloat() - strokeW / 2, 
                        imageHeight.toFloat() - strokeW / 2, 
                        vignetteStrokePaint
                    )
                }
            }

            // 5. Draw the professional LEICA STYLE WATERMARK BAND at the bottom of the photo container
            // Solid Matte Slate-Black or Ceramic-White band
            val watermarkPaint = Paint().apply {
                color = if (_leicaStyle.value == "Noir") Color.BLACK else Color.rgb(20, 20, 22)
            }
            canvas.drawRect(0f, imageHeight.toFloat(), w.toFloat(), h.toFloat(), watermarkPaint)

            if (_leicaWatermark.value) {
                val textCol = Color.WHITE
                val titlePaint = Paint().apply {
                    color = textCol
                    textSize = 42f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val subPaint = Paint().apply {
                    color = Color.rgb(180, 180, 184)
                    textSize = 28f
                    isAntiAlias = true
                }

                // 5a. Draw Leica Red Round Emblem Logo
                val rx = 100f
                val ry = imageHeight + watermarkHeight / 2f
                val brandEmblemColor = if (_leicaStyle.value == "Noir") Color.DKGRAY else Color.rgb(220, 10, 20)
                val emblemPaint = Paint().apply {
                    color = brandEmblemColor
                    isAntiAlias = true
                }
                canvas.drawCircle(rx, ry, 35f, emblemPaint)

                // Write 'L' letter on top
                val emblemLetterPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 45f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("L", rx, ry + 15, emblemLetterPaint)

                // 5b. Active XML Config Name / Branding text
                val configBrandingText = _watermarkText.value.ifEmpty { "LMC 8.4 PRO" }
                canvas.drawText(
                    configBrandingText,
                    165f,
                    imageHeight + 85f,
                    titlePaint
                )

                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText(
                    "SHOT ON LMC 8.4 • CO-ENGINEERED • $formattedDate",
                    165f,
                    imageHeight + 140f,
                    subPaint
                )

                // 5c. EXIF details right-aligned
                val exifDetailsText = "ISO $isoVal  |  ${_shutterSpeed.value}  |  ${_zoomLevel.value} LENS  |  EV ${_ev.value}"
                val rightTextPaint = Paint().apply {
                    color = Color.rgb(190, 190, 195)
                    textSize = 30f
                    textAlign = Paint.Align.RIGHT
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText(
                     exifDetailsText,
                     w - 80f,
                     imageHeight + 115f,
                     rightTextPaint
                )
            } else {
                // Minimal credit text if Leica Watermark is OFF
                val minPaint = Paint().apply {
                    color = Color.rgb(140, 140, 145)
                    textSize = 26f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(
                    "SAVED VIA LMC 8.4 CORE CAMERA (Watermark disabled)",
                    w / 2f,
                    imageHeight + 110f,
                    minPaint
                )
            }

            // Compress to local Jpeg
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()

            return imageFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun clampColor(value: Int): Int {
        return value.coerceIn(0, 255)
    }

    // Physical sensor monitoring for level horizon bubble
    private fun startSensors() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_showLevelIndicator.value) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            // Simple conversion of gravity elements to horizontal roll degrees for UI indicators
            _levelRoll.value = -ax * 10f // amplify movement multiplier
            _levelPitch.value = ay * 10f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}
