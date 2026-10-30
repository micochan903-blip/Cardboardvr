package com.example.model

enum class VrDisplayMode(val title: String, val description: String) {
    AUTO_DETECT(
        title = "Auto-Detect VR",
        description = "Intelligently detects if screen is already SBS VR or standard 2D"
    ),
    FORCE_2D_TO_SBS(
        title = "2D to VR (Clone)",
        description = "Duplicates regular full phone screen to Left and Right eye lenses"
    ),
    FORCE_ALREADY_VR(
        title = "VR Pass-Through",
        description = "Screen is already SBS VR: passes left half to left eye, right half to right eye as-is"
    )
}

enum class AspectRatioMode(val label: String) {
    UNCUT_FIT("Uncut Fit (No Sides Cut)"),
    FIT("Fit (Keep Ratio)"),
    FILL("Fill Viewport"),
    EXPAND("Wide Stretch")
}

data class VrSettings(
    val displayMode: VrDisplayMode = VrDisplayMode.AUTO_DETECT,
    val ipdOffsetDp: Float = 0f,
    val screenScale: Float = 0.90f, // Comfortable default preventing edge cutoff
    val edgeSafePaddingDp: Float = 10f, // Edge-safe margin so sides never get cut
    val barrelDistortion: Boolean = true,
    val showCenterDivider: Boolean = true,
    val showAlignmentCrosshair: Boolean = false,
    val touchPassThroughEnabled: Boolean = true, // Pass all touch commands to phone screen
    val volumeKeyControls: Boolean = true, // Volume keys send control commands in VR
    val overlayOpacity: Float = 1.0f,
    val enableHeadTracking: Boolean = false,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.UNCUT_FIT
)

data class VrDetectionResult(
    val isAlreadyVr: Boolean = false,
    val confidence: Float = 0f,
    val statusMessage: String = "Monitoring screen...",
    val timestamp: Long = System.currentTimeMillis()
)
