package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capture.ScreenCaptureManager
import com.example.model.VrDetectionResult
import com.example.model.VrDisplayMode
import com.example.model.VrSettings
import com.example.ui.components.CalibrationControls
import com.example.ui.components.VrDualEyeCanvas
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CardboardBrown
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VrBackground
import com.example.ui.theme.VrBorder
import com.example.ui.theme.VrCardBg
import com.example.ui.theme.VrSurface

@Composable
fun HomeScreen(
    settings: VrSettings,
    detectionResult: VrDetectionResult,
    isOverlayActive: Boolean,
    isCapturing: Boolean,
    hasOverlayPermission: Boolean,
    latestFrame: Bitmap?,
    onSettingsChange: ((VrSettings) -> VrSettings) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onStartScreenCaptureAndOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onLaunchViewerActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sampleModeIndex by remember { mutableStateOf(0) } // 0: Live / Interactive, 1: Simulated 2D Screen, 2: Simulated SBS VR 3D
    val scrollState = rememberScrollState()

    // Generate simulated test bitmaps for instant live proof
    val sampleFrame = remember(sampleModeIndex) {
        when (sampleModeIndex) {
            1 -> createSimulated2dFrame()
            2 -> createSimulatedSbsFrame()
            else -> null
        }
    }

    val activeFrame = sampleFrame ?: latestFrame

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VrBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardboardBrown),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Cardboard VR Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Cardboard VR",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Phone Screen to Google Cardboard SBS",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isOverlayActive) Color(0x3300E676) else Color(0x2294A3B8))
                        .border(1.dp, if (isOverlayActive) GreenSuccess else VrBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOverlayActive) GreenSuccess else TextMuted)
                        )
                        Text(
                            text = if (isOverlayActive) "VR CAST ACTIVE" else "STANDBY",
                            color = if (isOverlayActive) GreenSuccess else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Live Cardboard SBS Optical Preview Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vr_preview_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VrCardBg),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Optical Viewport",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Live Cardboard Stereo Optics",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        // Current active status indicator
                        Text(
                            text = when (settings.displayMode) {
                                VrDisplayMode.AUTO_DETECT -> if (detectionResult.isAlreadyVr || sampleModeIndex == 2) "Pass-Through (As-is)" else "Duplicating 2D"
                                VrDisplayMode.FORCE_2D_TO_SBS -> "Forced 2D Clone"
                                VrDisplayMode.FORCE_ALREADY_VR -> "Forced SBS As-is"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (detectionResult.isAlreadyVr || sampleModeIndex == 2) AmberNeon else CyanNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Stereo 16:9 Landscape Cardboard Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, VrBorder, RoundedCornerShape(12.dp))
                    ) {
                        VrDualEyeCanvas(
                            frameBitmap = activeFrame,
                            settings = settings,
                            detectionResult = detectionResult,
                            isSampleSbsContent = (sampleModeIndex == 2),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Test Selector Pills: Live vs Sample 2D vs Sample SBS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "Live Capture",
                            1 to "Test 2D Screen",
                            2 to "Test SBS 3D VR"
                        ).forEach { (idx, label) ->
                            val isSelected = sampleModeIndex == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x3300E5FF) else VrSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyanNeon else VrBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        sampleModeIndex = idx
                                        if (idx == 1) {
                                            ScreenCaptureManager.updateSimulatedFrame(createSimulated2dFrame())
                                        } else if (idx == 2) {
                                            ScreenCaptureManager.updateSimulatedFrame(createSimulatedSbsFrame())
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyanNeon else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Primary Actions: Start VR Overlay vs Headset Mode
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isOverlayActive) {
                    Button(
                        onClick = onStartScreenCaptureAndOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_vr_overlay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = Color(0xFF00363D)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Start VR",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Start System Floating VR Overlay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onStopOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("stop_vr_overlay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedWarning,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop VR",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Stop VR Overlay Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fullscreen Headset Mode Button
                OutlinedButton(
                    onClick = onLaunchViewerActivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_headset_viewer_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardboardBrown),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CardboardBrown)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Cardboard Headset Mode",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Fullscreen Headset Cinema (Landscape)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Privacy & Security Guarantee Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_security_card"),
                colors = CardDefaults.cardColors(containerColor = VrCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Vault",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Zero-Leak Privacy & Security",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "100% Air-Gapped • Local Memory Only",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanNeon
                            )
                        }
                    }

                    listOf(
                        "Air-Gapped (No Internet)" to "Zero network permissions requested. The app physically cannot send screen data anywhere.",
                        "Volatile RAM Pipeline" to "Captured frames are held in temporary memory for optical splitting and immediately recycled. Never written to disk or storage.",
                        "Zero Telemetry or Logging" to "No analytics, trackers, ads, or background profiling. Your screen contents remain completely confidential.",
                        "Anti-Leak Sandbox" to "System backup disabled (allowBackup=false) to prevent ADB or cloud backup leakage of app memory."
                    ).forEach { (title, detail) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Secure",
                                tint = GreenSuccess,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Key Feature Card: "The Rest of the Phone Keeps Working Normally"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("touch_passthrough_card"),
                colors = CardDefaults.cardColors(containerColor = VrCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VrBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2200E676)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = "Touch Pass-Through",
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Touch Pass-Through",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Use all other apps normally while in VR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = settings.touchPassThroughEnabled,
                            onCheckedChange = {
                                onSettingsChange { s -> s.copy(touchPassThroughEnabled = it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GreenSuccess,
                                checkedTrackColor = Color(0x4400E676)
                            ),
                            modifier = Modifier.testTag("touch_passthrough_switch")
                        )
                    }

                    Text(
                        text = "When enabled, user taps and gestures pass directly through the Cardboard VR overlay to whichever app is active below (browsers, videos, games, home screen). A floating bubble remains on screen to pause or toggle settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    // Draw Over Other Apps Permission Check
                    if (!hasOverlayPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFB300))
                                .border(1.dp, AmberNeon, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Permission Needed",
                                        tint = AmberNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "'Display over other apps' permission required",
                                        color = AmberGlow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "Grant this permission so the Cardboard VR display can float above all other phone apps.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Button(
                                    onClick = onRequestOverlayPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberNeon, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 4.dp).testTag("grant_overlay_perm_button")
                                ) {
                                    Text("Grant Overlay Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Overlay Granted",
                                tint = GreenSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Overlay permission granted",
                                color = GreenSuccess,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Calibration Controls (Mode Selector, IPD, Zoom, Center Divider)
            CalibrationControls(
                settings = settings,
                detectionResult = detectionResult,
                onSettingsChange = onSettingsChange
            )

            // Google Cardboard Quick-Start Guide Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cardboard_guide_card"),
                colors = CardDefaults.cardColors(containerColor = VrCardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VrBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Cardboard Guide",
                            tint = CardboardBrown,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "How to Use with Google Cardboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    listOf(
                        "1. Start Overlay or Headset Mode" to "Tap 'Start VR Overlay' or 'Open Fullscreen Headset Cinema'.",
                        "2. Smart SBS Detection" to "Standard 2D screens are automatically duplicated. If you play a 3D VR SBS video, it passes through as-is!",
                        "3. Insert Phone into Cardboard" to "Align the center divider notch with the viewer's nose bridge.",
                        "4. Continue Using Your Phone" to "With Touch Pass-Through enabled, tap and browse through the overlay normally!"
                    ).forEach { (step, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "•",
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = step,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = desc,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a synthetic 2D screen bitmap (like a web browser or feed) to test standard duplication.
 */
private fun createSimulated2dFrame(): Bitmap {
    val w = 640
    val h = 360
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    canvas.drawColor(AndroidColor.parseColor("#0F172A"))

    val paint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#38BDF8")
        textSize = 24f
        isAntiAlias = true
    }
    canvas.drawText("Regular 2D App Screen", 40f, 60f, paint)

    val subPaint = AndroidPaint().apply {
        color = AndroidColor.LTGRAY
        textSize = 16f
        isAntiAlias = true
    }
    canvas.drawText("Full phone screen content without split", 40f, 100f, subPaint)

    // Draw some simulated app cards
    val cardPaint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#1E293B")
        style = AndroidPaint.Style.FILL
    }
    canvas.drawRect(40f, 130f, 600f, 210f, cardPaint)
    canvas.drawRect(40f, 230f, 600f, 310f, cardPaint)

    return bmp
}

/**
 * Creates a synthetic SBS 3D VR bitmap with distinct Left and Right eye stereo half-images
 * to immediately trigger and test the "Already in VR Mode - Pass-Through As-Is" feature!
 */
private fun createSimulatedSbsFrame(): Bitmap {
    val w = 640
    val h = 360
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    canvas.drawColor(AndroidColor.BLACK)

    val halfW = w / 2f
    val bgPaint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#18181B")
    }
    canvas.drawRect(0f, 0f, halfW - 2f, h.toFloat(), bgPaint)
    canvas.drawRect(halfW + 2f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    // Draw Left Eye
    val leftPaint = AndroidPaint().apply {
        color = AndroidColor.parseColor("#FFB300")
        textSize = 20f
        isAntiAlias = true
        textAlign = AndroidPaint.Align.CENTER
    }
    canvas.drawText("SBS 3D (LEFT EYE)", halfW / 2f, 70f, leftPaint)

    // Draw Right Eye (matching stereo content)
    canvas.drawText("SBS 3D (RIGHT EYE)", halfW + halfW / 2f, 70f, leftPaint)

    // Stereo sphere with parallax
    val circlePaint = AndroidPaint().apply {
        color = AndroidColor.CYAN
        isAntiAlias = true
    }
    // Left eye circle shifted slightly left
    canvas.drawCircle(halfW / 2f - 10f, h / 2f, 35f, circlePaint)
    // Right eye circle shifted slightly right
    canvas.drawCircle(halfW + halfW / 2f + 10f, h / 2f, 35f, circlePaint)

    // Center divider
    val divPaint = AndroidPaint().apply {
        color = AndroidColor.DKGRAY
        strokeWidth = 3f
    }
    canvas.drawLine(halfW, 0f, halfW, h.toFloat(), divPaint)

    return bmp
}
