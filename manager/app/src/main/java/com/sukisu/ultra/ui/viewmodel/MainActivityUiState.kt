package com.sukisu.ultra.ui.viewmodel

import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.theme.AppSettings

data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val uiMode: UiMode,
)
