package com.example.capture

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.VrDetectionResult
import kotlin.math.abs

/**
 * Intelligent detector that inspects captured screen frames to determine
 * whether the content is already in Side-by-Side (SBS) VR mode or standard 2D.
 */
object VrDetector {

    /**
     * Samples points across the left half and right half of the bitmap.
     * Computes luminance and color difference between corresponding stereo coordinates.
     */
    fun analyzeFrame(bitmap: Bitmap?): VrDetectionResult {
        if (bitmap == null || bitmap.isRecycled) {
            return VrDetectionResult(
                isAlreadyVr = false,
                confidence = 0f,
                statusMessage = "No active video stream"
            )
        }

        val width = bitmap.width
        val height = bitmap.height
        if (width < 32 || height < 32) {
            return VrDetectionResult(isAlreadyVr = false, confidence = 0f, statusMessage = "Frame too small")
        }

        val halfWidth = width / 2
        val sampleGridX = 16
        val sampleGridY = 16
        var totalSamples = 0
        var matchingSamples = 0
        var totalDiffSum = 0.0

        val stepX = (halfWidth - 10) / sampleGridX
        val stepY = (height - 10) / sampleGridY

        if (stepX <= 0 || stepY <= 0) {
            return VrDetectionResult(isAlreadyVr = false, confidence = 0f, statusMessage = "Sampling error")
        }

        // Compare corresponding pixels: (x, y) with (x + halfWidth, y)
        for (gy in 1..sampleGridY) {
            val y = (gy * stepY).coerceIn(0, height - 1)
            for (gx in 1..sampleGridX) {
                val xLeft = (gx * stepX).coerceIn(0, halfWidth - 1)
                val xRight = (xLeft + halfWidth).coerceIn(halfWidth, width - 1)

                val pixelLeft = bitmap.getPixel(xLeft, y)
                val pixelRight = bitmap.getPixel(xRight, y)

                val rDiff = abs(Color.red(pixelLeft) - Color.red(pixelRight))
                val gDiff = abs(Color.green(pixelLeft) - Color.green(pixelRight))
                val bDiff = abs(Color.blue(pixelLeft) - Color.blue(pixelRight))
                val avgDiff = (rDiff + gDiff + bDiff) / 3.0

                totalDiffSum += avgDiff
                totalSamples++

                // Pixel difference under threshold is considered a stereo pair match
                if (avgDiff < 28.0) {
                    matchingSamples++
                }
            }
        }

        if (totalSamples == 0) {
            return VrDetectionResult(isAlreadyVr = false, confidence = 0f, statusMessage = "No samples")
        }

        val matchRatio = matchingSamples.toFloat() / totalSamples.toFloat()
        val avgDiff = (totalDiffSum / totalSamples).toFloat()

        // SBS VR content typically has matchRatio > 0.65 and average pixel difference < 25
        val isAlreadyVr = matchRatio >= 0.65f && avgDiff < 32f
        val confidence = if (isAlreadyVr) {
            ((matchRatio - 0.65f) / 0.35f).coerceIn(0.5f, 1.0f)
        } else {
            ((0.65f - matchRatio) / 0.65f).coerceIn(0.5f, 1.0f)
        }

        val message = if (isAlreadyVr) {
            "Already VR Content Detected (Match: ${(matchRatio * 100).toInt()}%) -> Direct Pass-Through"
        } else {
            "Standard 2D Screen Detected (Match: ${(matchRatio * 100).toInt()}%) -> Duplicating to Cardboard SBS"
        }

        return VrDetectionResult(
            isAlreadyVr = isAlreadyVr,
            confidence = confidence,
            statusMessage = message,
            timestamp = System.currentTimeMillis()
        )
    }
}
