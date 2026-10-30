package com.example.ui.viewer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.capture.ScreenCaptureManager
import com.example.model.VrDisplayMode
import com.example.model.VrStateHolder
import com.example.ui.components.VrDualEyeCanvas
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VrBackground
import kotlinx.coroutines.delay

class CardboardViewerActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null

    private var currentPitch by mutableFloatStateOf(0f)
    private var currentYaw by mutableFloatStateOf(0f)
    private var baseYaw = 0f
    private var basePitch = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen active for VR headset viewing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enter sticky immersive fullscreen
        hideSystemBars()

        // Setup gyro / rotation vector for VR head tracking
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            MyApplicationTheme {
                val frameBitmap by ScreenCaptureManager.latestFrame.collectAsStateWithLifecycle()
                val settings by VrStateHolder.settings.collectAsStateWithLifecycle()
                val detectionResult by ScreenCaptureManager.detectionResult.collectAsStateWithLifecycle()

                var showHud by remember { mutableStateOf(true) }

                // Auto-hide HUD after 4 seconds of inactivity
                LaunchedEffect(showHud) {
                    if (showHud) {
                        delay(4000)
                        showHud = false
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VrBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showHud = !showHud
                        }
                ) {
                    // Google Cardboard Dual-Eye SBS View
                    VrDualEyeCanvas(
                        frameBitmap = frameBitmap,
                        settings = settings,
                        detectionResult = detectionResult,
                        headPitch = currentPitch - basePitch,
                        headYaw = currentYaw - baseYaw,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Auto-hiding VR HUD Overlay
                    AnimatedVisibility(
                        visible = showHud,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        CardboardViewerHud(
                            settings = settings,
                            onModeSelected = { mode -> VrStateHolder.setDisplayMode(mode) },
                            onIpdChange = { delta ->
                                VrStateHolder.setIpdOffset((settings.ipdOffsetDp + delta).coerceIn(-30f, 30f))
                            },
                            onScaleChange = { delta ->
                                VrStateHolder.setScreenScale((settings.screenScale + delta).coerceIn(0.6f, 1.3f))
                            },
                            onRecenter = {
                                baseYaw = currentYaw
                                basePitch = currentPitch
                            },
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (VrStateHolder.settings.value.volumeKeyControls) {
            when (keyCode) {
                android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    triggerHapticClick()
                    baseYaw = currentYaw
                    basePitch = currentPitch
                    android.widget.Toast.makeText(this, "VR Command: Center Calibrated", android.widget.Toast.LENGTH_SHORT).show()
                    return true
                }
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                    triggerHapticClick()
                    val current = VrStateHolder.settings.value.displayMode
                    val next = when (current) {
                        VrDisplayMode.AUTO_DETECT -> VrDisplayMode.FORCE_2D_TO_SBS
                        VrDisplayMode.FORCE_2D_TO_SBS -> VrDisplayMode.FORCE_ALREADY_VR
                        VrDisplayMode.FORCE_ALREADY_VR -> VrDisplayMode.AUTO_DETECT
                    }
                    VrStateHolder.setDisplayMode(next)
                    android.widget.Toast.makeText(this, "Mode: ${next.title}", android.widget.Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun triggerHapticClick() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(35, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            // azimuth/yaw = orientation[0], pitch = orientation[1], roll = orientation[2]
            currentYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
            currentPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun CardboardViewerHud(
    settings: com.example.model.VrSettings,
    onModeSelected: (VrDisplayMode) -> Unit,
    onIpdChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onRecenter: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .testTag("cardboard_viewer_hud"),
        color = Color(0xCC0B0F19),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Close / Exit VR
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF5252))
                    .testTag("exit_vr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit VR",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Mode Selector Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VrDisplayMode.values().forEach { mode ->
                    val isSelected = settings.displayMode == mode
                    val icon = when (mode) {
                        VrDisplayMode.AUTO_DETECT -> Icons.Default.AutoAwesome
                        VrDisplayMode.FORCE_2D_TO_SBS -> Icons.Default.ContentCopy
                        VrDisplayMode.FORCE_ALREADY_VR -> Icons.Default.Visibility
                    }
                    val label = when (mode) {
                        VrDisplayMode.AUTO_DETECT -> "Auto"
                        VrDisplayMode.FORCE_2D_TO_SBS -> "2D Duplicate"
                        VrDisplayMode.FORCE_ALREADY_VR -> "SBS As-Is"
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyanNeon else Color.Transparent)
                            .clickable { onModeSelected(mode) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // IPD Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "IPD: ${settings.ipdOffsetDp.toInt()}",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                IconButton(
                    onClick = { onIpdChange(-2f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease IPD",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onIpdChange(2f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase IPD",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Zoom Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Zoom: ${(settings.screenScale * 100).toInt()}%",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                IconButton(
                    onClick = { onScaleChange(-0.05f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease Zoom",
                        tint = AmberNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onScaleChange(0.05f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase Zoom",
                        tint = AmberNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Recenter Gyro
            IconButton(
                onClick = onRecenter,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300E5FF))
                    .testTag("recenter_vr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Recenter View",
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
