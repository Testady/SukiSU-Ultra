package com.sukisu.ultra.ui.screen.kpm

import androidx.compose.runtime.Immutable
import com.sukisu.ultra.ui.component.SearchStatus
import com.sukisu.ultra.ui.viewmodel.KpmViewModel
import java.io.File

@Immutable
data class KpmUiState(
    val isRefreshing: Boolean = false,
    val moduleList: List<KpmViewModel.ModuleInfo> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus(""),
    val searchResults: List<KpmViewModel.ModuleInfo> = emptyList(),
    val error: Throwable? = null
)

@Immutable
data class KpmActions(
    val onRefresh: () -> Unit = {},
    val onInstallClick: () -> Unit = {},
    val onUninstall: (String) -> Unit = {},
    val onControl: (String) -> Unit = {},
    val onUpdateArgs: (String) -> Unit = {},
    val onConfirmControl: () -> Unit = {},
    val onDismissControlDialog: () -> Unit = {},
    val onClearInstallState: () -> Unit = {},
    val onShowInstallModeDialog: () -> Unit = {},
    val onTempFileForInstallChange: File? = null
)
