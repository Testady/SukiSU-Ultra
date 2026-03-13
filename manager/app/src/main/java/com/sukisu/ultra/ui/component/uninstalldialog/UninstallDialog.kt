package com.sukisu.ultra.ui.component.uninstalldialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode

@Composable
fun UninstallDialog(showDialog: MutableState<Boolean>) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> UninstallDialogMiuix(showDialog)
        UiMode.Material -> UninstallDialogMaterial(showDialog)
    }
}
