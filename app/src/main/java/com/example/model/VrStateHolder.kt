package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VrStateHolder {

    private val _settings = MutableStateFlow(VrSettings())
    val settings: StateFlow<VrSettings> = _settings.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    fun updateSettings(transform: (VrSettings) -> VrSettings) {
        _settings.update(transform)
    }

    fun setDisplayMode(mode: VrDisplayMode) {
        _settings.update { it.copy(displayMode = mode) }
    }

    fun setIpdOffset(offset: Float) {
        _settings.update { it.copy(ipdOffsetDp = offset) }
    }

    fun setScreenScale(scale: Float) {
        _settings.update { it.copy(screenScale = scale) }
    }

    fun toggleTouchPassThrough() {
        _settings.update { it.copy(touchPassThroughEnabled = !it.touchPassThroughEnabled) }
    }

    fun setTouchPassThrough(enabled: Boolean) {
        _settings.update { it.copy(touchPassThroughEnabled = enabled) }
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
    }
}
