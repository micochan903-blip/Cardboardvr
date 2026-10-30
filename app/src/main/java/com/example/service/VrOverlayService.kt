package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.capture.ScreenCaptureManager
import com.example.model.VrDetectionResult
import com.example.model.VrDisplayMode
import com.example.model.VrSettings
import com.example.model.VrStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VrOverlayService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var windowManager: WindowManager? = null
    private var overlayView: VrSurfaceOverlayView? = null
    private var floatingBubbleView: View? = null

    companion object {
        const val CHANNEL_ID = "cardboard_vr_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.vr.ACTION_START"
        const val ACTION_STOP = "com.example.vr.ACTION_STOP"
        const val ACTION_TOGGLE_TOUCH = "com.example.vr.ACTION_TOGGLE_TOUCH"
        const val ACTION_SWITCH_MODE = "com.example.vr.ACTION_SWITCH_MODE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, VrOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VrOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_TOUCH -> {
                VrStateHolder.toggleTouchPassThrough()
                updateNotification()
                updateOverlayTouchFlags()
                return START_STICKY
            }
            ACTION_SWITCH_MODE -> {
                val current = VrStateHolder.settings.value.displayMode
                val next = when (current) {
                    VrDisplayMode.AUTO_DETECT -> VrDisplayMode.FORCE_2D_TO_SBS
                    VrDisplayMode.FORCE_2D_TO_SBS -> VrDisplayMode.FORCE_ALREADY_VR
                    VrDisplayMode.FORCE_ALREADY_VR -> VrDisplayMode.AUTO_DETECT
                }
                VrStateHolder.setDisplayMode(next)
                updateNotification()
                return START_STICKY
            }
            ACTION_START -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val resultData = if (intent != null) {
                    androidx.core.content.IntentCompat.getParcelableExtra(intent, EXTRA_RESULT_DATA, Intent::class.java)
                } else null

                if (resultCode != 0 && resultData != null) {
                    ScreenCaptureManager.startCapture(this, resultCode, resultData)
                }

                VrStateHolder.setOverlayActive(true)
                initOverlayWindows()
                observeState()
            }
        }

        return START_STICKY
    }

    private fun observeState() {
        // Collect latest screen frames
        scope.launch {
            ScreenCaptureManager.latestFrame.collectLatest { bitmap ->
                overlayView?.updateFrame(bitmap)
            }
        }

        // Collect settings changes
        scope.launch {
            VrStateHolder.settings.collectLatest { settings ->
                overlayView?.updateSettings(settings)
                updateOverlayTouchFlags()
            }
        }

        // Collect detection results
        scope.launch {
            ScreenCaptureManager.detectionResult.collectLatest { detection ->
                overlayView?.updateDetection(detection)
            }
        }
    }

    private fun initOverlayWindows() {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        try {
            if (overlayView == null) {
                val view = VrSurfaceOverlayView(this)
                overlayView = view

                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        (if (VrStateHolder.settings.value.touchPassThroughEnabled)
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    flags,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                windowManager?.addView(view, params)
            }

            // Also add a draggable floating control bubble so user can toggle touch pass-through & mode
            initFloatingControlBubble()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initFloatingControlBubble() {
        if (floatingBubbleView != null || !android.provider.Settings.canDrawOverlays(this)) return

        val bubble = FloatingBubbleView(this) { action ->
            when (action) {
                "toggle_touch" -> VrStateHolder.toggleTouchPassThrough()
                "toggle_mode" -> {
                    val current = VrStateHolder.settings.value.displayMode
                    val next = when (current) {
                        VrDisplayMode.AUTO_DETECT -> VrDisplayMode.FORCE_2D_TO_SBS
                        VrDisplayMode.FORCE_2D_TO_SBS -> VrDisplayMode.FORCE_ALREADY_VR
                        VrDisplayMode.FORCE_ALREADY_VR -> VrDisplayMode.AUTO_DETECT
                    }
                    VrStateHolder.setDisplayMode(next)
                }
                "open_app" -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
                "stop" -> stopForegroundService()
            }
        }
        floatingBubbleView = bubble

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 100
        }

        bubble.attachLayoutParams(bubbleParams, windowManager)
        windowManager?.addView(bubble, bubbleParams)
    }

    private fun updateOverlayTouchFlags() {
        val view = overlayView ?: return
        val wm = windowManager ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return

        val isTouchPassThrough = VrStateHolder.settings.value.touchPassThroughEnabled

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (isTouchPassThrough) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        params.flags = flags
        try {
            wm.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun buildNotification(): Notification {
        val settings = VrStateHolder.settings.value
        val modeStr = settings.displayMode.title
        val touchStr = if (settings.touchPassThroughEnabled) "Touch: Pass-Through ON" else "Touch: Controls"

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, VrOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleTouchIntent = PendingIntent.getService(
            this, 2,
            Intent(this, VrOverlayService::class.java).apply { action = ACTION_TOGGLE_TOUCH },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val switchModeIntent = PendingIntent.getService(
            this, 3,
            Intent(this, VrOverlayService::class.java).apply { action = ACTION_SWITCH_MODE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Google Cardboard VR Screen Cast")
            .setContentText("$modeStr | $touchStr")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(R.mipmap.ic_launcher, "Stop", stopIntent)
            .addAction(R.mipmap.ic_launcher, "Toggle Touch", toggleTouchIntent)
            .addAction(R.mipmap.ic_launcher, "Mode", switchModeIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cardboard VR Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of Google Cardboard VR screen mirroring and overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        VrStateHolder.setOverlayActive(false)
        ScreenCaptureManager.stopCapture()

        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
        floatingBubbleView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            floatingBubbleView = null
        }

        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()
    }
}

/**
 * Lightweight, zero-overhead custom overlay view rendering Cardboard dual-eye SBS.
 */
class VrSurfaceOverlayView(context: Context) : View(context) {

    private var frameBitmap: Bitmap? = null
    private var settings: VrSettings = VrSettings()
    private var detectionResult: VrDetectionResult = VrDetectionResult()

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val dividerPaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 4f
    }
    private val textPaint = Paint().apply {
        color = Color.CYAN
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val subPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    fun updateFrame(bitmap: Bitmap?) {
        frameBitmap = bitmap
        postInvalidate()
    }

    fun updateSettings(newSettings: VrSettings) {
        settings = newSettings
        postInvalidate()
    }

    fun updateDetection(detection: VrDetectionResult) {
        detectionResult = detection
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalW = width.toFloat()
        val totalH = height.toFloat()
        if (totalW <= 0 || totalH <= 0) return

        canvas.drawRect(0f, 0f, totalW, totalH, bgPaint)

        val halfW = totalW / 2f
        val ipdPx = settings.ipdOffsetDp * resources.displayMetrics.density
        val safeMarginPx = settings.edgeSafePaddingDp * resources.displayMetrics.density

        val minLeftX = safeMarginPx
        val maxLeftX = halfW - safeMarginPx
        val leftCenterX = (halfW / 2f - ipdPx).coerceIn(minLeftX + 20f, maxLeftX - 20f)
        val maxLeftAllowedW = 2f * minOf(leftCenterX - minLeftX, maxLeftX - leftCenterX)

        val minRightX = halfW + safeMarginPx
        val maxRightX = totalW - safeMarginPx
        val rightCenterX = (halfW + halfW / 2f + ipdPx).coerceIn(minRightX + 20f, maxRightX - 20f)
        val maxRightAllowedW = 2f * minOf(rightCenterX - minRightX, maxRightX - rightCenterX)

        val safeFitW = minOf(maxLeftAllowedW, maxRightAllowedW) * settings.screenScale
        val safeFitH = (totalH - 2 * safeMarginPx) * settings.screenScale
        val centerY = totalH / 2f

        val isEffectivelyVr = when (settings.displayMode) {
            VrDisplayMode.AUTO_DETECT -> detectionResult.isAlreadyVr
            VrDisplayMode.FORCE_ALREADY_VR -> true
            VrDisplayMode.FORCE_2D_TO_SBS -> false
        }

        val bmp = frameBitmap
        if (bmp != null && !bmp.isRecycled) {
            val srcW = bmp.width
            val srcH = bmp.height

            if (isEffectivelyVr) {
                // Pass through: Left half to left eye, right half to right eye
                val halfSrcW = srcW / 2
                val srcRectLeft = Rect(0, 0, halfSrcW, srcH)
                val srcRectRight = Rect(halfSrcW, 0, srcW, srcH)

                val aspect = halfSrcW.toFloat() / srcH.toFloat()
                var fitW = safeFitW
                var fitH = fitW / aspect
                if (fitH > safeFitH) {
                    fitH = safeFitH
                    fitW = fitH * aspect
                }

                val dstLeft = RectF(
                    leftCenterX - fitW / 2f,
                    centerY - fitH / 2f,
                    leftCenterX + fitW / 2f,
                    centerY + fitH / 2f
                )
                val dstRight = RectF(
                    rightCenterX - fitW / 2f,
                    centerY - fitH / 2f,
                    rightCenterX + fitW / 2f,
                    centerY + fitH / 2f
                )

                canvas.drawBitmap(bmp, srcRectLeft, dstLeft, null)
                canvas.drawBitmap(bmp, srcRectRight, dstRight, null)
            } else {
                // 2D Clone: Duplicate entire screen to both eyes without cutting sides
                val srcRect = Rect(0, 0, srcW, srcH)
                val aspect = srcW.toFloat() / srcH.toFloat()
                var fitW = safeFitW
                var fitH = fitW / aspect
                if (fitH > safeFitH) {
                    fitH = safeFitH
                    fitW = fitH * aspect
                }

                val dstLeft = RectF(
                    leftCenterX - fitW / 2f,
                    centerY - fitH / 2f,
                    leftCenterX + fitW / 2f,
                    centerY + fitH / 2f
                )
                val dstRight = RectF(
                    rightCenterX - fitW / 2f,
                    centerY - fitH / 2f,
                    rightCenterX + fitW / 2f,
                    centerY + fitH / 2f
                )

                canvas.drawBitmap(bmp, srcRect, dstLeft, null)
                canvas.drawBitmap(bmp, srcRect, dstRight, null)
            }
        }

        // Center divider line
        if (settings.showCenterDivider) {
            canvas.drawLine(halfW, 0f, halfW, totalH, dividerPaint)
        }
    }
}

/**
 * Floating draggable control pill with quick actions (Toggle Touch, Mode, Open App, Close)
 */
class FloatingBubbleView(
    context: Context,
    private val onAction: (String) -> Unit
) : View(context) {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var layoutParamsRef: WindowManager.LayoutParams? = null
    private var wmRef: WindowManager? = null

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#E6111827")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val subTextPaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 20f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    init {
        setPadding(24, 16, 24, 16)
    }

    fun attachLayoutParams(params: WindowManager.LayoutParams, wm: WindowManager?) {
        layoutParamsRef = params
        wmRef = wm
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        setMeasuredDimension((200 * density).toInt(), (64 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val corner = 32f

        canvas.drawRoundRect(0f, 0f, w, h, corner, corner, bgPaint)
        canvas.drawRoundRect(0f, 0f, w, h, corner, corner, borderPaint)

        val isTouchPass = VrStateHolder.settings.value.touchPassThroughEnabled
        val mode = VrStateHolder.settings.value.displayMode

        canvas.drawText(
            if (isTouchPass) "VR: PASS-THRU ON" else "VR: CONTROLS",
            w / 2f,
            h * 0.44f,
            textPaint
        )
        canvas.drawText(
            "Tap: Toggle | Double-tap: Mode",
            w / 2f,
            h * 0.78f,
            subTextPaint
        )
    }

    private var lastTapTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParamsRef ?: return super.onTouchEvent(event)
        val wm = wmRef ?: return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    wm.updateViewLayout(this, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 350L) {
                        onAction("toggle_mode")
                    } else {
                        onAction("toggle_touch")
                    }
                    lastTapTime = now
                    postInvalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
