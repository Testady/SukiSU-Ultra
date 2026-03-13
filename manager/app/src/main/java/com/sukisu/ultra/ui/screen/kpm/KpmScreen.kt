package com.sukisu.ultra.ui.screen.kpm

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.component.dialog.ConfirmResult
import com.sukisu.ultra.ui.component.dialog.rememberConfirmDialog
import com.sukisu.ultra.ui.util.getRootShell
import com.sukisu.ultra.ui.util.unloadKpmModule
import com.sukisu.ultra.ui.viewmodel.KpmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

@Composable
fun KpmScreen() {
    val bottomInnerPadding = 0.dp
    val viewModel = viewModel<KpmViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val confirmDialog = rememberConfirmDialog()

    val searchStatus = uiState.searchStatus

    LaunchedEffect(searchStatus.searchText) {
        viewModel.updateSearchText(searchStatus.searchText)
    }

    val kpmInstallSuccess = stringResource(R.string.kpm_install_success)
    val kpmInstallFailed = stringResource(R.string.kpm_install_failed)
    val cancel = stringResource(R.string.cancel)
    val uninstall = stringResource(R.string.uninstall)
    val failedToCheckModuleFile = stringResource(R.string.snackbar_failed_to_check_module_file)
    val kpmUninstallSuccess = stringResource(R.string.kpm_uninstall_success)
    val kpmUninstallFailed = stringResource(R.string.kpm_uninstall_failed)
    val kpmInstallMode = stringResource(R.string.kpm_install_mode)
    val kpmInstallModeLoad = stringResource(R.string.kpm_install_mode_load)
    val kpmInstallModeEmbed = stringResource(R.string.kpm_install_mode_embed)
    val invalidFileTypeMessage = stringResource(R.string.invalid_file_type)
    val confirmTitle = stringResource(R.string.confirm_uninstall_title_with_filename)
    val control = stringResource(R.string.kpm_control_success)
    val controlFailed = stringResource(R.string.kpm_control_failed)

    val showToast: suspend (String) -> Unit = { msg ->
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val moduleConfirmContentMap = uiState.moduleList.associate { module ->
        module.id to stringResource(R.string.confirm_uninstall_content, module.id)
    }

    var tempFileForInstall by remember { mutableStateOf<File?>(null) }
    var showInstallModeDialog by remember { mutableStateOf(false) }
    var moduleName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tempFileForInstall) {
        moduleName = tempFileForInstall?.let { extractModuleName(it) }
    }

    fun clearInstallState() {
        runCatching {
            showInstallModeDialog = false
            runCatching { tempFileForInstall?.delete() }
            tempFileForInstall = null
            moduleName = null
        }.onFailure {
            Log.e("KsuCli", "clearInstallState: ${it.message}", it)
        }
    }

    val selectPatchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        scope.launch {
            val fileName = uri.lastPathSegment ?: "unknown.kpm"
            val encodedFileName = URLEncoder.encode(fileName, "UTF-8")
            val tempFile = File(context.cacheDir, encodedFileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!isValidKpmFile(tempFile, context.contentResolver.getType(uri))) {
                showToast(invalidFileTypeMessage)
                tempFile.delete()
                return@launch
            }

            tempFileForInstall = tempFile
            showInstallModeDialog = true
        }
    }

    val actions = KpmActions(
        onRefresh = { viewModel.fetchModuleList() },
        onInstallClick = {
            selectPatchLauncher.launch(
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/octet-stream"
                }
            )
        },
        onUninstall = { moduleId ->
            scope.launch {
                val module = uiState.moduleList.find { it.id == moduleId } ?: return@launch
                handleModuleUninstall(
                    module = module,
                    viewModel = viewModel,
                    showToast = showToast,
                    kpmUninstallSuccess = kpmUninstallSuccess,
                    kpmUninstallFailed = kpmUninstallFailed,
                    failedToCheckModuleFile = failedToCheckModuleFile,
                    uninstall = uninstall,
                    cancel = cancel,
                    confirmDialog = confirmDialog,
                    confirmTitle = confirmTitle,
                    confirmContent = moduleConfirmContentMap[moduleId] ?: ""
                )
            }
        },
        onControl = { moduleId ->
            viewModel.showInputDialog(moduleId)
        },
        onUpdateArgs = { args ->
            viewModel.updateInputArgs(args)
        },
        onConfirmControl = {
            scope.launch {
                val result = viewModel.executeControl()
                val message = when (result) {
                    0 -> control
                    else -> controlFailed
                }
                showToast(message)
            }
        },
        onDismissControlDialog = {
            viewModel.hideInputDialog()
        },
        onClearInstallState = { clearInstallState() },
        onShowInstallModeDialog = { showInstallModeDialog = true },
        onTempFileForInstallChange = tempFileForInstall
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> KpmMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            showInstallModeDialog = showInstallModeDialog,
            moduleName = moduleName,
            context = context,
            scope = scope,
            showToast = showToast,
            kpmInstallSuccess = kpmInstallSuccess,
            kpmInstallFailed = kpmInstallFailed,
            kpmInstallMode = kpmInstallMode,
            kpmInstallModeLoad = kpmInstallModeLoad,
            kpmInstallModeEmbed = kpmInstallModeEmbed,
            cancel = cancel,
        )

        UiMode.Material -> KpmMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            showInstallModeDialog = showInstallModeDialog,
            moduleName = moduleName,
            context = context,
            scope = scope,
            showToast = showToast,
            kpmInstallSuccess = kpmInstallSuccess,
            kpmInstallFailed = kpmInstallFailed,
            kpmInstallMode = kpmInstallMode,
            kpmInstallModeLoad = kpmInstallModeLoad,
            kpmInstallModeEmbed = kpmInstallModeEmbed,
            cancel = cancel,
        )
    }
}

private suspend fun handleModuleUninstall(
    module: KpmViewModel.ModuleInfo,
    viewModel: KpmViewModel,
    showToast: suspend (String) -> Unit,
    kpmUninstallSuccess: String,
    kpmUninstallFailed: String,
    failedToCheckModuleFile: String,
    uninstall: String,
    cancel: String,
    confirmTitle: String,
    confirmContent: String,
    confirmDialog: com.sukisu.ultra.ui.component.dialog.ConfirmDialogHandle
) {
    val moduleFileName = "${module.id}.kpm"
    val moduleFilePath = "/data/adb/kpm/$moduleFileName"

    val fileExists = try {
        val shell = getRootShell()
        val result = shell.newJob().add("ls /data/adb/kpm/$moduleFileName").exec()
        result.isSuccess
    } catch (e: Exception) {
        Log.e("KsuCli", "Failed to check module file existence: ${e.message}", e)
        showToast(failedToCheckModuleFile)
        false
    }

    val confirmResult = confirmDialog.awaitConfirm(
        title = confirmTitle,
        content = confirmContent,
        confirm = uninstall,
        dismiss = cancel
    )

    if (confirmResult == ConfirmResult.Confirmed) {
        try {
            val unloadResult = unloadKpmModule(module.id)
            if (!unloadResult) {
                Log.e("KsuCli", "Failed to unload KPM module")
                showToast(kpmUninstallFailed)
                return
            }

            if (fileExists) {
                val shell = getRootShell()
                shell.newJob().add("rm $moduleFilePath").exec()
            }

            viewModel.fetchModuleList()
            showToast(kpmUninstallSuccess)
        } catch (e: Exception) {
            Log.e("KsuCli", "Failed to unload KPM module: ${e.message}", e)
            showToast(kpmUninstallFailed)
        }
    }
}
