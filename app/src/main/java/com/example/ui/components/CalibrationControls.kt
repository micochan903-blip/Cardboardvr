package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VrDetectionResult
import com.example.model.VrDisplayMode
import com.example.model.VrSettings
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CardboardBrown
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VrBorder
import com.example.ui.theme.VrCardBg
import com.example.ui.theme.VrSurface

@Composable
fun CalibrationControls(
    settings: VrSettings,
    detectionResult: VrDetectionResult,
    onSettingsChange: ((VrSettings) -> VrSettings) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VR Mode Selector Card
        ModeSelectorCard(
            currentMode = settings.displayMode,
            detectionResult = detectionResult,
            onSelectMode = { mode ->
                onSettingsChange { it.copy(displayMode = mode) }
            }
        )

        // Optics Adjustment Card (IPD and Scale)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("optics_settings_card"),
            colors = CardDefaults.cardColors(containerColor = VrCardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VrBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Optics Calibration",
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Cardboard Lens Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            onSettingsChange { it.copy(ipdOffsetDp = 0f, screenScale = 0.95f) }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reset_optics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Optics",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // IPD (Eye Separation) Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Interpupillary Distance (IPD)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${if (settings.ipdOffsetDp > 0) "+" else ""}${settings.ipdOffsetDp.toInt()} dp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                    }
                    Slider(
                        value = settings.ipdOffsetDp,
                        onValueChange = { onSettingsChange { s -> s.copy(ipdOffsetDp = it) } },
                        valueRange = -30f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = VrSurface
                        ),
                        modifier = Modifier.testTag("ipd_slider")
                    )
                    Text(
                        text = "Adjust until both images align comfortably in your Cardboard lenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Edge-Safe Margin Slider (Guarantees screen sides never get cut off)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Edge-Safe Padding (Prevent Cut Sides)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${settings.edgeSafePaddingDp.toInt()} dp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                    }
                    Slider(
                        value = settings.edgeSafePaddingDp,
                        onValueChange = { onSettingsChange { s -> s.copy(edgeSafePaddingDp = it) } },
                        valueRange = 0f..28f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = VrSurface
                        ),
                        modifier = Modifier.testTag("safe_padding_slider")
                    )
                    Text(
                        text = "Guarantees full screen content fits inside Cardboard lenses without clipping sides",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Volume Key Commands Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hardware Key Control Commands",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Vol Down = Recenter/Click | Vol Up = Switch Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.volumeKeyControls,
                        onCheckedChange = { onSettingsChange { s -> s.copy(volumeKeyControls = it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = Color(0x5500E5FF)
                        ),
                        modifier = Modifier.testTag("volume_keys_switch")
                    )
                }

                // Visual Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Center Alignment Divider",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cardboard viewer nose divider notch",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.showCenterDivider,
                        onCheckedChange = { onSettingsChange { s -> s.copy(showCenterDivider = it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CardboardBrown,
                            checkedTrackColor = Color(0x55C79255)
                        ),
                        modifier = Modifier.testTag("center_divider_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lens Barrel Vignette",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Simulates realistic optical lens curvature",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.barrelDistortion,
                        onCheckedChange = { onSettingsChange { s -> s.copy(barrelDistortion = it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = Color(0x5500E5FF)
                        ),
                        modifier = Modifier.testTag("vignette_switch")
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelectorCard(
    currentMode: VrDisplayMode,
    detectionResult: VrDetectionResult,
    onSelectMode: (VrDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vr_mode_selector_card"),
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
            Text(
                text = "Screen Mode & VR Detection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // 3 Mode Buttons
            VrDisplayMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                val borderColor = if (isSelected) {
                    when (mode) {
                        VrDisplayMode.AUTO_DETECT -> CyanNeon
                        VrDisplayMode.FORCE_2D_TO_SBS -> CardboardBrown
                        VrDisplayMode.FORCE_ALREADY_VR -> AmberNeon
                    }
                } else VrBorder

                val bgColor = if (isSelected) {
                    when (mode) {
                        VrDisplayMode.AUTO_DETECT -> Color(0x1800E5FF)
                        VrDisplayMode.FORCE_2D_TO_SBS -> Color(0x18C79255)
                        VrDisplayMode.FORCE_ALREADY_VR -> Color(0x18FFB300)
                    }
                } else VrSurface

                val icon = when (mode) {
                    VrDisplayMode.AUTO_DETECT -> Icons.Default.AutoAwesome
                    VrDisplayMode.FORCE_2D_TO_SBS -> Icons.Default.ContentCopy
                    VrDisplayMode.FORCE_ALREADY_VR -> Icons.Default.Visibility
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onSelectMode(mode) }
                        .padding(12.dp)
                        .testTag("mode_${mode.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.title,
                        tint = if (isSelected) CyanNeon else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = TextPrimary
                            )
                            if (mode == VrDisplayMode.AUTO_DETECT) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x3300E676))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "SMART",
                                        color = GreenSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Real-time Detection Status Badge
            AnimatedVisibility(visible = currentMode == VrDisplayMode.AUTO_DETECT) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (detectionResult.isAlreadyVr) AmberNeon else GreenSuccess)
                        )
                        Column {
                            Text(
                                text = if (detectionResult.isAlreadyVr) "Detected: Already VR Content (SBS)" else "Detected: Regular 2D Screen",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (detectionResult.isAlreadyVr) AmberGlow else CyanNeon
                            )
                            Text(
                                text = if (detectionResult.isAlreadyVr)
                                    "Passing left & right halves directly to lenses as-is"
                                else
                                    "Cloning phone screen to both Cardboard eye lenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
