package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.capture.ScreenCaptureManager
import com.example.model.VrStateHolder
import com.example.service.VrOverlayService
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewer.CardboardViewerActivity

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var hasOverlayPermission by mutableStateOf(false)

    // Screen Capture Intent launcher for MediaProjection
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            VrOverlayService.start(this, result.resultCode, result.data!!)
            Toast.makeText(this, "Cardboard VR Overlay Started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Android 13+ POST_NOTIFICATIONS
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications recommended for VR controls", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        ScreenCaptureManager.initialize(this)

        checkNotificationPermission()
        checkOverlayPermission()

        setContent {
            MyApplicationTheme {
                val settings by VrStateHolder.settings.collectAsStateWithLifecycle()
                val isOverlayActive by VrStateHolder.isOverlayActive.collectAsStateWithLifecycle()
                val isCapturing by ScreenCaptureManager.isCapturing.collectAsStateWithLifecycle()
                val latestFrame by ScreenCaptureManager.latestFrame.collectAsStateWithLifecycle()
                val detectionResult by ScreenCaptureManager.detectionResult.collectAsStateWithLifecycle()

                HomeScreen(
                    settings = settings,
                    detectionResult = detectionResult,
                    isOverlayActive = isOverlayActive,
                    isCapturing = isCapturing,
                    hasOverlayPermission = hasOverlayPermission,
                    latestFrame = latestFrame,
                    onSettingsChange = { transform -> VrStateHolder.updateSettings(transform) },
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onStartScreenCaptureAndOverlay = {
                        if (!hasOverlayPermission) {
                            requestOverlayPermission()
                        } else {
                            startScreenCapture()
                        }
                    },
                    onStopOverlay = {
                        VrOverlayService.stop(this)
                        Toast.makeText(this, "VR Overlay Stopped", Toast.LENGTH_SHORT).show()
                    },
                    onLaunchViewerActivity = {
                        val intent = Intent(this, CardboardViewerActivity::class.java)
                        startActivity(intent)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Please enable 'Display over other apps'", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }
}
