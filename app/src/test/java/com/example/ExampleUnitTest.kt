package com.example

import com.example.capture.VrDetector
import com.example.model.AspectRatioMode
import com.example.model.VrDisplayMode
import com.example.model.VrSettings
import com.example.model.VrStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun vrDetector_handlesNullBitmapGracefully() {
        val result = VrDetector.analyzeFrame(null)
        assertNotNull(result)
        assertFalse(result.isAlreadyVr)
        assertEquals(0f, result.confidence, 0.001f)
    }

    @Test
    fun vrStateHolder_togglesTouchPassThrough() {
        val initial = VrStateHolder.settings.value.touchPassThroughEnabled
        VrStateHolder.toggleTouchPassThrough()
        val toggled = VrStateHolder.settings.value.touchPassThroughEnabled
        assertEquals(!initial, toggled)

        // Restore
        VrStateHolder.setTouchPassThrough(initial)
    }

    @Test
    fun vrStateHolder_edgeSafePaddingAndControls() {
        val settings = VrStateHolder.settings.value
        assertTrue("Edge safe padding must be positive to prevent side cuts", settings.edgeSafePaddingDp >= 0f)
        assertTrue("Touch pass through should be enabled by default", settings.touchPassThroughEnabled)
        assertTrue("Volume key controls should be enabled by default", settings.volumeKeyControls)
        assertEquals(AspectRatioMode.UNCUT_FIT, settings.aspectRatioMode)
    }

    @Test
    fun vrStateHolder_updatesModeAndIpd() {
        VrStateHolder.setDisplayMode(VrDisplayMode.FORCE_ALREADY_VR)
        assertEquals(VrDisplayMode.FORCE_ALREADY_VR, VrStateHolder.settings.value.displayMode)

        VrStateHolder.setIpdOffset(12.5f)
        assertEquals(12.5f, VrStateHolder.settings.value.ipdOffsetDp, 0.001f)

        VrStateHolder.setScreenScale(1.15f)
        assertEquals(1.15f, VrStateHolder.settings.value.screenScale, 0.001f)

        // Reset to default
        VrStateHolder.setDisplayMode(VrDisplayMode.AUTO_DETECT)
        VrStateHolder.setIpdOffset(0f)
        VrStateHolder.setScreenScale(0.90f)
    }
}
