package com.sukisu.ultra.ui.screen.kpm

import androidx.compose.runtime.Immutable
import com.sukisu.ultra.ui.component.SearchStatus
import com.sukisu.ultra.ui.viewmodel.KpmViewModel

@Immutable
data class KpmUiState(
    val isRefreshing: Boolean = false,
    val moduleList: List<KpmViewModel.ModuleInfo> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus(""),
    val searchResults: List<KpmViewModel.ModuleInfo> = emptyList(),
    val error: Throwable? = null
)