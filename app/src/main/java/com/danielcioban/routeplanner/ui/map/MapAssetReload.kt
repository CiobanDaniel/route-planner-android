package com.danielcioban.routeplanner.ui.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bump to clear the WebView asset cache and reload `map.html`.
 * Needed after shipping a new map asset; Android otherwise keeps the old file.
 */
object MapAssetReload {
    private val _epoch = MutableStateFlow(0L)
    val epoch: StateFlow<Long> = _epoch.asStateFlow()

    fun bump() {
        val next = System.currentTimeMillis()
        _epoch.value = if (next > _epoch.value) next else _epoch.value + 1L
    }
}
