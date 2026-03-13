package com.sukisu.ultra.ui.screen.kpm

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.sukisu.ultra.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TextButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KpmMaterial(
    state: KpmUiState,
    actions: KpmActions,
    bottomInnerPadding: Dp,
    showInstallModeDialog: Boolean,
    moduleName: String?,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    showToast: suspend (String) -> Unit,
    kpmInstallSuccess: String,
    kpmInstallFailed: String,
    kpmInstallMode: String,
    kpmInstallModeLoad: String,
    kpmInstallModeEmbed: String,
    cancel: String,
) {
    val layoutDirection = LocalLayoutDirection.current

    val listState = rememberLazyListState()

    val showEmptyState by remember {
        derivedStateOf {
            state.moduleList.isEmpty() && state.searchStatus.searchText.isEmpty() && !state.isRefreshing
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        while(true) {
            actions.onRefresh()
            delay(5000)
        }
    }

    val scrollDistance = remember { mutableFloatStateOf(0f) }
    var fabVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isScrolledToEnd(listState)) return Offset.Zero

                scrollDistance.floatValue += available.y

                if (scrollDistance.floatValue <= -50f && fabVisible) {
                    fabVisible = false
                    scrollDistance.floatValue = 0f
                    return Offset(0f, available.y)
                }

                if (scrollDistance.floatValue >= 50f && !fabVisible) {
                    fabVisible = true
                    scrollDistance.floatValue = 0f
                    return Offset(0f, available.y)
                }

                return Offset.Zero
            }
        }
    }
    val offsetHeight by animateDpAsState(
        targetValue = if (fabVisible) 0.dp else 180.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        animationSpec = tween(durationMillis = 350)
    )

    if (showInstallModeDialog) {
        KpmInstallModeDialogMaterial(
            moduleName = moduleName,
            kpmInstallMode = kpmInstallMode,
            kpmInstallModeLoad = kpmInstallModeLoad,
            kpmInstallModeEmbed = kpmInstallModeEmbed,
            cancel = cancel,
            onClearInstallState = actions.onClearInstallState,
            onInstall = { isEmbed ->
                scope.launch {
                    val tempFile = actions.onTempFileForInstallChange
                    tempFile?.let {
                        handleModuleInstall(
                            tempFile = it,
                            isEmbed = isEmbed,
                            showToast = showToast,
                            kpmInstallSuccess = kpmInstallSuccess,
                            kpmInstallFailed = kpmInstallFailed,
                            onRefresh = actions.onRefresh
                        )
                    }
                    actions.onClearInstallState()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kpm_title)) },
                actions = {
                    IconButton(
                        onClick = actions.onRefresh
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = fabVisible) {
                FloatingActionButton(
                    modifier = Modifier
                        .offset(y = offsetHeight)
                        .padding(bottom = bottomInnerPadding + 20.dp, end = 20.dp),
                    onClick = actions.onInstallClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.package_import),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        if (showEmptyState) {
            EmptyStateViewMaterial(
                innerPadding = innerPadding,
                bottomInnerPadding = bottomInnerPadding,
                layoutDirection = layoutDirection
            )
        } else {
            KpmListMaterial(
                state = state,
                actions = actions,
                scrollBehavior = scrollBehavior,
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                bottomInnerPadding = bottomInnerPadding,
                layoutDirection = layoutDirection,
                context = context
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KpmInstallModeDialogMaterial(
    moduleName: String?,
    kpmInstallMode: String,
    kpmInstallModeLoad: String,
    kpmInstallModeEmbed: String,
    cancel: String,
    onClearInstallState: () -> Unit,
    onInstall: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClearInstallState,
        title = { Text(kpmInstallMode) },
        text = {
            Column {
                moduleName?.let {
                    Text(text = stringResource(R.string.kpm_install_mode_description, it))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onInstall(false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(kpmInstallModeLoad)
                    }

                    Button(
                        onClick = { onInstall(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(kpmInstallModeEmbed)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        text = cancel,
                        onClick = onClearInstallState,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KpmListMaterial(
    state: KpmUiState,
    actions: KpmActions,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    nestedScrollConnection: NestedScrollConnection,
    innerPadding: PaddingValues,
    bottomInnerPadding: Dp,
    layoutDirection: LayoutDirection,
    context: Context
) {
    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    var isNoticeClosed by remember { mutableStateOf(sharedPreferences.getBoolean("is_notice_closed", false)) }

    var isRefreshing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(350)
            actions.onRefresh()
            isRefreshing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { if (!isRefreshing) isRefreshing = true },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
            ) {
                if (!isNoticeClosed) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = stringResource(R.string.kernel_module_notice),
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = {
                                        isNoticeClosed = true
                                        sharedPreferences.edit { putBoolean("is_notice_closed", true) }
                                    },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.close_notice),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                items(state.moduleList) { module ->
                    KpmModuleItemMaterial(
                        module = module,
                        onUninstall = {
                            actions.onUninstall(module.id)
                        },
                        onControl = {
                            actions.onControl(module.id)
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun KpmModuleItemMaterial(
    module: com.sukisu.ultra.ui.viewmodel.KpmViewModel.ModuleInfo,
    onUninstall: () -> Unit,
    onControl: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = module.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.kpm_version)}: ${module.version}",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                        fontWeight = FontWeight(550),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stringResource(R.string.kpm_author)}: ${module.author}",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 1.dp),
                        fontWeight = FontWeight(550),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (module.args.isNotEmpty()) {
                        Text(
                            text = "${stringResource(R.string.kpm_args)}: ${module.args}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight(550),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (module.description.isNotBlank()) {
                Text(
                    text = module.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
            )

            Row {
                AnimatedVisibility(
                    visible = module.hasAction,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = onControl,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.kpm_control),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onUninstall,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.kpm_uninstall))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateViewMaterial(
    innerPadding: PaddingValues,
    bottomInnerPadding: Dp,
    layoutDirection: LayoutDirection
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottomInnerPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                stringResource(R.string.kpm_empty),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
