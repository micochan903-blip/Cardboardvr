package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioMode
import com.example.model.VrDetectionResult
import com.example.model.VrDisplayMode
import com.example.model.VrSettings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CardboardBrown
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.VrBackground
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance dual-eye Side-by-Side (SBS) canvas for Google Cardboard VR rendering.
 * Strictly guarantees that screen edges and sides are never cut off or clipped.
 */
@Composable
fun VrDualEyeCanvas(
    frameBitmap: Bitmap?,
    settings: VrSettings,
    detectionResult: VrDetectionResult,
    modifier: Modifier = Modifier,
    useSimulatedIfNull: Boolean = true,
    isSampleSbsContent: Boolean = false,
    headPitch: Float = 0f,
    headYaw: Float = 0f
) {
    val density = LocalDensity.current
    val ipdPx = with(density) { settings.ipdOffsetDp.dp.toPx() }
    val safeMarginPx = with(density) { settings.edgeSafePaddingDp.dp.toPx() }

    // Infinite animation for simulated interactive demo scene
    val infiniteTransition = rememberInfiniteTransition(label = "vr_sim_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val isEffectivelyVr = when (settings.displayMode) {
        VrDisplayMode.AUTO_DETECT -> detectionResult.isAlreadyVr || isSampleSbsContent
        VrDisplayMode.FORCE_ALREADY_VR -> true
        VrDisplayMode.FORCE_2D_TO_SBS -> false
    }

    Box(
        modifier = modifier
            .background(VrBackground)
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val halfWidth = totalWidth / 2f

            if (totalWidth <= 0 || totalHeight <= 0) return@Canvas

            // Compute exact bounds that prevent ANY side-cutting:
            val minLeftX = safeMarginPx
            val maxLeftX = halfWidth - safeMarginPx
            val leftCenterX = (halfWidth / 2f - ipdPx).coerceIn(minLeftX + 20f, maxLeftX - 20f)
            val maxLeftAllowedW = 2f * minOf(leftCenterX - minLeftX, maxLeftX - leftCenterX)

            val minRightX = halfWidth + safeMarginPx
            val maxRightX = totalWidth - safeMarginPx
            val rightCenterX = (halfWidth + halfWidth / 2f + ipdPx).coerceIn(minRightX + 20f, maxRightX - 20f)
            val maxRightAllowedW = 2f * minOf(rightCenterX - minRightX, maxRightX - rightCenterX)

            val safeFitW = minOf(maxLeftAllowedW, maxRightAllowedW) * settings.screenScale
            val safeFitH = (totalHeight - 2 * safeMarginPx) * settings.screenScale

            val leftEyeCenter = Offset(leftCenterX, totalHeight / 2f)
            val rightEyeCenter = Offset(rightCenterX, totalHeight / 2f)

            if (frameBitmap != null && !frameBitmap.isRecycled) {
                val imageBitmap = frameBitmap.asImageBitmap()
                val srcWidth = frameBitmap.width.toFloat()
                val srcHeight = frameBitmap.height.toFloat()

                if (isEffectivelyVr) {
                    // Content is ALREADY VR SBS:
                    // Left half goes to left eye, right half goes to right eye as-is!
                    val halfSrcWidth = srcWidth / 2f

                    // Draw Left Eye (left half of source)
                    drawEyeImageUncut(
                        image = imageBitmap,
                        srcRect = Rect(0f, 0f, halfSrcWidth, srcHeight),
                        eyeCenter = leftEyeCenter,
                        maxFitW = safeFitW,
                        maxFitH = safeFitH,
                        aspectRatioMode = settings.aspectRatioMode
                    )

                    // Draw Right Eye (right half of source)
                    drawEyeImageUncut(
                        image = imageBitmap,
                        srcRect = Rect(halfSrcWidth, 0f, srcWidth, srcHeight),
                        eyeCenter = rightEyeCenter,
                        maxFitW = safeFitW,
                        maxFitH = safeFitH,
                        aspectRatioMode = settings.aspectRatioMode
                    )
                } else {
                    // Standard 2D Content: Clone full screen into both eyes
                    val fullSrcRect = Rect(0f, 0f, srcWidth, srcHeight)

                    // Draw Left Eye (full source)
                    drawEyeImageUncut(
                        image = imageBitmap,
                        srcRect = fullSrcRect,
                        eyeCenter = leftEyeCenter,
                        maxFitW = safeFitW,
                        maxFitH = safeFitH,
                        aspectRatioMode = settings.aspectRatioMode
                    )

                    // Draw Right Eye (full source)
                    drawEyeImageUncut(
                        image = imageBitmap,
                        srcRect = fullSrcRect,
                        eyeCenter = rightEyeCenter,
                        maxFitW = safeFitW,
                        maxFitH = safeFitH,
                        aspectRatioMode = settings.aspectRatioMode
                    )
                }
            } else if (useSimulatedIfNull) {
                // Interactive Cardboard Test Scene
                drawSimulatedScene(
                    eyeCenter = leftEyeCenter,
                    eyeSize = Size(safeFitW, safeFitH),
                    isLeftEye = true,
                    isSbsMode = isEffectivelyVr,
                    animAngle = animProgress,
                    scale = 1.0f,
                    headPitch = headPitch,
                    headYaw = headYaw
                )

                drawSimulatedScene(
                    eyeCenter = rightEyeCenter,
                    eyeSize = Size(safeFitW, safeFitH),
                    isLeftEye = false,
                    isSbsMode = isEffectivelyVr,
                    animAngle = animProgress,
                    scale = 1.0f,
                    headPitch = headPitch,
                    headYaw = headYaw
                )
            }

            // Optional Cardboard Lens Barrel Distortion / Radial Vignette
            if (settings.barrelDistortion) {
                drawLensVignette(center = leftEyeCenter, radius = halfWidth * 0.52f)
                drawLensVignette(center = rightEyeCenter, radius = halfWidth * 0.52f)
            }

            // Alignment Crosshairs
            if (settings.showAlignmentCrosshair) {
                drawCrosshair(leftEyeCenter)
                drawCrosshair(rightEyeCenter)
            }

            // Google Cardboard Center Divider Line & Alignment Marker
            if (settings.showCenterDivider) {
                // Solid center line
                drawLine(
                    color = Color.Black,
                    start = Offset(halfWidth, 0f),
                    end = Offset(halfWidth, totalHeight),
                    strokeWidth = 6f
                )
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(halfWidth, 0f),
                    end = Offset(halfWidth, totalHeight),
                    strokeWidth = 2f
                )

                // Top & Bottom Cardboard alignment notches (for nose and viewer center)
                val notchHeight = 22.dp.toPx()
                val notchWidth = 12.dp.toPx()

                val topNotch = Path().apply {
                    moveTo(halfWidth - notchWidth / 2f, 0f)
                    lineTo(halfWidth + notchWidth / 2f, 0f)
                    lineTo(halfWidth, notchHeight)
                    close()
                }
                drawPath(topNotch, CardboardBrown)

                val bottomNotch = Path().apply {
                    moveTo(halfWidth - notchWidth / 2f, totalHeight)
                    lineTo(halfWidth + notchWidth / 2f, totalHeight)
                    lineTo(halfWidth, totalHeight - notchHeight)
                    close()
                }
                drawPath(bottomNotch, CardboardBrown)
            }
        }
    }
}

/**
 * Calculates exact destination rectangle so content is NEVER clipped or cut on sides.
 */
private fun DrawScope.drawEyeImageUncut(
    image: androidx.compose.ui.graphics.ImageBitmap,
    srcRect: Rect,
    eyeCenter: Offset,
    maxFitW: Float,
    maxFitH: Float,
    aspectRatioMode: AspectRatioMode
) {
    val srcAspect = srcRect.width / srcRect.height
    val finalWidth: Float
    val finalHeight: Float

    when (aspectRatioMode) {
        AspectRatioMode.UNCUT_FIT, AspectRatioMode.FIT -> {
            var w = maxFitW
            var h = w / srcAspect
            if (h > maxFitH) {
                h = maxFitH
                w = h * srcAspect
            }
            finalWidth = w
            finalHeight = h
        }
        AspectRatioMode.FILL -> {
            var w = maxFitW
            var h = w / srcAspect
            if (h < maxFitH) {
                h = maxFitH
                w = h * srcAspect
            }
            finalWidth = w
            finalHeight = h
        }
        AspectRatioMode.EXPAND -> {
            finalWidth = maxFitW
            finalHeight = maxFitH
        }
    }

    val dstLeft = eyeCenter.x - finalWidth / 2f
    val dstTop = eyeCenter.y - finalHeight / 2f

    drawImage(
        image = image,
        srcOffset = IntOffset(srcRect.left.toInt(), srcRect.top.toInt()),
        srcSize = IntSize(srcRect.width.toInt(), srcRect.height.toInt()),
        dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
        dstSize = IntSize(finalWidth.toInt(), finalHeight.toInt())
    )
}

/**
 * Draws an interactive 3D virtual theater scene to visually test and verify
 * 2D clone mode vs SBS pass-through with stereo parallax.
 */
private fun DrawScope.drawSimulatedScene(
    eyeCenter: Offset,
    eyeSize: Size,
    isLeftEye: Boolean,
    isSbsMode: Boolean,
    animAngle: Float,
    scale: Float,
    headPitch: Float,
    headYaw: Float
) {
    val parallaxOffset = if (isLeftEye) -12f else 12f
    val effectiveCenter = Offset(
        eyeCenter.x + (headYaw * 15f),
        eyeCenter.y + (headPitch * 15f)
    )

    val boxWidth = eyeSize.width * 0.95f * scale
    val boxHeight = eyeSize.height * 0.90f * scale

    val screenRect = Rect(
        effectiveCenter.x - boxWidth / 2f,
        effectiveCenter.y - boxHeight / 2f,
        effectiveCenter.x + boxWidth / 2f,
        effectiveCenter.y + boxHeight / 2f
    )

    // Virtual Screen Frame / Bezel
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(screenRect.left - 6f, screenRect.top - 6f),
        size = Size(screenRect.width + 12f, screenRect.height + 12f)
    )

    // Screen Content
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
            startY = screenRect.top,
            endY = screenRect.bottom
        ),
        topLeft = Offset(screenRect.left, screenRect.top),
        size = Size(screenRect.width, screenRect.height)
    )

    // Virtual grid lines inside simulated screen
    val gridCount = 4
    for (i in 1..gridCount) {
        val y = screenRect.top + (screenRect.height / (gridCount + 1)) * i
        drawLine(
            color = Color(0x2238BDF8),
            start = Offset(screenRect.left, y),
            end = Offset(screenRect.right, y),
            strokeWidth = 1.5f
        )
    }

    // Floating 3D Stereoscopic Object with rotation
    val rad = Math.toRadians(animAngle.toDouble())
    val orbDistance = 40f
    val orbX = effectiveCenter.x + parallaxOffset + (cos(rad) * orbDistance).toFloat()
    val orbY = effectiveCenter.y + (sin(rad * 0.7) * 20f).toFloat()

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyanNeon, Color.Transparent),
            center = Offset(orbX, orbY),
            radius = 32f
        ),
        radius = 32f,
        center = Offset(orbX, orbY)
    )
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = Offset(orbX, orbY)
    )

    // Mode Banner
    val bannerColor = if (isSbsMode) AmberNeon else CyanNeon
    val bannerText = if (isSbsMode) "SBS 3D PASS-THROUGH (UNCUT)" else "2D TO CARDBOARD (NO SIDES CUT)"

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            color = bannerColor.hashCode()
            textSize = 26f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.nativeCanvas.drawText(
            bannerText,
            effectiveCenter.x,
            screenRect.top + 36f,
            paint
        )

        val subPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 18f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val eyeLabel = if (isLeftEye) "LEFT EYE (UNCUT)" else "RIGHT EYE (UNCUT)"
        canvas.nativeCanvas.drawText(
            eyeLabel,
            effectiveCenter.x,
            screenRect.bottom - 20f,
            subPaint
        )
    }
}

private fun DrawScope.drawLensVignette(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.75f to Color.Transparent,
            0.93f to Color(0x55000000),
            1.0f to Color(0xCC000000),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawCrosshair(center: Offset) {
    val size = 14f
    drawLine(
        color = Color(0x8800E5FF),
        start = Offset(center.x - size, center.y),
        end = Offset(center.x + size, center.y),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0x8800E5FF),
        start = Offset(center.x, center.y - size),
        end = Offset(center.x, center.y + size),
        strokeWidth = 2f
    )
    drawCircle(
        color = CyanNeon,
        radius = 3f,
        center = center
    )
}
