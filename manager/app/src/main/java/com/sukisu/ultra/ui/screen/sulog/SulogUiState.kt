package com.sukisu.ultra.ui.screen.sulog

import androidx.compose.runtime.Immutable

@Immutable
data class SulogUiState(
    val entries: List<SulogEntry> = emptyList()
)

@Immutable
data class SulogEntry(val uptime: Int, val uid: Int, val sym: Char, val raw: String)

@Immutable
data class SulogActions(
    val onBack: () -> Unit = {},
)
