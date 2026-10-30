package com.example.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.model.VrDetectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

object ScreenCaptureManager {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _detectionResult = MutableStateFlow(VrDetectionResult())
    val detectionResult: StateFlow<VrDetectionResult> = _detectionResult.asStateFlow()

    private var lastAnalysisTimestamp = 0L
    private val scope = CoroutineScope(Dispatchers.Default)

    var lastResultCode: Int? = null
    var lastResultData: Intent? = null

    fun initialize(context: Context) {
        if (mediaProjectionManager == null) {
            mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
    }

    fun startCapture(context: Context, resultCode: Int, resultData: Intent): Boolean {
        try {
            stopCapture()

            lastResultCode = resultCode
            lastResultData = resultData

            val thread = HandlerThread("VrScreenCaptureThread").apply { start() }
            backgroundThread = thread
            val handler = Handler(thread.looper)
            backgroundHandler = handler

            val mpManager = mediaProjectionManager ?: (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            mediaProjectionManager = mpManager

            val projection = mpManager.getMediaProjection(resultCode, resultData) ?: return false
            mediaProjection = projection

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            // Scaled capture buffer for high performance and low battery consumption
            val targetWidth = 1280
            val targetHeight = (targetWidth * (metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat())).toInt()

            val reader = ImageReader.newInstance(targetWidth, targetHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            val vDisplay = projection.createVirtualDisplay(
                "CardboardVR_ScreenCapture",
                targetWidth,
                targetHeight,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
            virtualDisplay = vDisplay

            reader.setOnImageAvailableListener({ ir ->
                var image: Image? = null
                try {
                    image = ir.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer: ByteBuffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * targetWidth

                        val bitmap = Bitmap.createBitmap(
                            targetWidth + rowPadding / pixelStride,
                            targetHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        // Trim padding if rowStride had extra pixels
                        val finalBitmap = if (rowPadding > 0) {
                            val trimmed = Bitmap.createBitmap(bitmap, 0, 0, targetWidth, targetHeight)
                            bitmap.recycle()
                            trimmed
                        } else {
                            bitmap
                        }

                        val oldFrame = _latestFrame.value
                        _latestFrame.value = finalBitmap
                        if (oldFrame != null && !oldFrame.isRecycled && oldFrame != finalBitmap) {
                            oldFrame.recycle()
                        }

                        // Run SBS VR detection periodically (every 400ms to conserve CPU)
                        val now = System.currentTimeMillis()
                        if (now - lastAnalysisTimestamp > 400L) {
                            lastAnalysisTimestamp = now
                            scope.launch {
                                val result = VrDetector.analyzeFrame(finalBitmap)
                                _detectionResult.value = result
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    image?.close()
                }
            }, handler)

            _isCapturing.value = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _isCapturing.value = false
            return false
        }
    }

    fun stopCapture() {
        try {
            _isCapturing.value = false
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            backgroundThread?.quitSafely()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates manual or simulated frame when real screen capture is not active.
     */
    fun updateSimulatedFrame(bitmap: Bitmap) {
        _latestFrame.value = bitmap
        scope.launch {
            _detectionResult.value = VrDetector.analyzeFrame(bitmap)
        }
    }
}
