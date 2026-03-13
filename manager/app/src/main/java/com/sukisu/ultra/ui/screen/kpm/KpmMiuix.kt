package com.sukisu.ultra.ui.screen.kpm

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
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
import com.sukisu.ultra.ui.component.miuix.SearchBox
import com.sukisu.ultra.ui.component.miuix.SearchPager
import com.sukisu.ultra.ui.theme.LocalEnableBlur
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun KpmMiuix(
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
    val enableBlur = LocalEnableBlur.current
    val listState = rememberLazyListState()

    val showEmptyState by remember {
        derivedStateOf {
            state.moduleList.isEmpty() && state.searchStatus.searchText.isEmpty() && !state.isRefreshing
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }

    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

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
        KpmInstallModeDialogMiuix(
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
        floatingActionButton = {
            AnimatedVisibility(visible = fabVisible) {
                top.yukonga.miuix.kmp.basic.FloatingActionButton(
                    modifier = Modifier
                        .offset(y = offsetHeight)
                        .padding(bottom = bottomInnerPadding + 20.dp, end = 20.dp)
                        .border(0.05.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                    shadowElevation = 0.dp,
                    onClick = actions.onInstallClick,
                    content = {
                        Icon(
                            painter = painterResource(id = R.drawable.package_import),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                )
            }
        },
        popupHost = {
            state.searchStatus.SearchPager(
                onSearchStatusChange = { /* handled in KpmScreen */ },
                defaultResult = {
                    LazyColumn {
                        item {
                            Spacer(Modifier.height(6.dp))
                        }
                        items(state.moduleList) { module ->
                            KpmModuleItemMiuix(
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
                            val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                            Spacer(Modifier.height(maxOf(bottomInnerPadding, imeBottomPadding)))
                        }
                    }
                },
                searchBarTopPadding = dynamicTopPadding,
                result = {
                    LazyColumn {
                        item {
                            Spacer(Modifier.height(6.dp))
                        }
                        items(state.moduleList) { module ->
                            KpmModuleItemMiuix(
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
                            val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                            Spacer(Modifier.height(maxOf(bottomInnerPadding, imeBottomPadding)))
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current

        if (showEmptyState) {
            EmptyStateViewMiuix(
                innerPadding = innerPadding,
                bottomInnerPadding = bottomInnerPadding,
                layoutDirection = layoutDirection
            )
        } else {
            state.searchStatus.SearchBox(
                onSearchStatusChange = { /* handled in KpmScreen */ },
                searchBarTopPadding = dynamicTopPadding,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection)
                ),
                hazeState = hazeState,
                hazeStyle = hazeStyle
            ) { boxHeightState ->
                KpmListMiuix(
                    state = state,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    nestedScrollConnection = nestedScrollConnection,
                    hazeState = hazeState,
                    innerPadding = innerPadding,
                    bottomInnerPadding = bottomInnerPadding,
                    boxHeight = boxHeightState.value,
                    layoutDirection = layoutDirection,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun KpmInstallModeDialogMiuix(
    moduleName: String?,
    kpmInstallMode: String,
    kpmInstallModeLoad: String,
    kpmInstallModeEmbed: String,
    cancel: String,
    onClearInstallState: () -> Unit,
    onInstall: (Boolean) -> Unit
) {
    val showDialogState = remember { mutableStateOf(true) }

    SuperDialog(
        show = showDialogState,
        title = kpmInstallMode,
        onDismissRequest = onClearInstallState,
        content = {
            Column {
                moduleName?.let {
                    Text(
                        text = stringResource(R.string.kpm_install_mode_description, it),
                        color = colorScheme.onBackground
                    )
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
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                        Text(kpmInstallModeLoad)
                    }

                    Button(
                        onClick = { onInstall(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
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
        }
    )
}

@Composable
private fun KpmListMiuix(
    state: KpmUiState,
    actions: KpmActions,
    scrollBehavior: ScrollBehavior,
    nestedScrollConnection: NestedScrollConnection,
    hazeState: HazeState,
    innerPadding: PaddingValues,
    bottomInnerPadding: Dp,
    boxHeight: Dp,
    layoutDirection: LayoutDirection,
    context: Context
) {
    val enableBlur = LocalEnableBlur.current
    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    var isNoticeClosed by remember { mutableStateOf(sharedPreferences.getBoolean("is_notice_closed", false)) }

    val refreshPulling = stringResource(R.string.refresh_pulling)
    val refreshRelease = stringResource(R.string.refresh_release)
    val refreshRefresh = stringResource(R.string.refresh_refresh)
    val refreshComplete = stringResource(R.string.refresh_complete)

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val refreshTexts = remember {
        listOf(
            refreshPulling,
            refreshRelease,
            refreshRefresh,
            refreshComplete,
        )
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(350)
            actions.onRefresh()
            isRefreshing = false
        }
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        pullToRefreshState = pullToRefreshState,
        onRefresh = { if (!isRefreshing) isRefreshing = true },
        refreshTexts = refreshTexts,
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + boxHeight + 6.dp,
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
        ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(nestedScrollConnection)
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it },
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + boxHeight + 6.dp,
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
            overscrollEffect = null,
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
                                tint = colorScheme.onBackground
                            )

                            Text(
                                text = stringResource(R.string.kernel_module_notice),
                                modifier = Modifier.weight(1f),
                                color = colorScheme.onBackground
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
                                    tint = colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            items(state.moduleList) { module ->
                KpmModuleItemMiuix(
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

@Composable
private fun KpmModuleItemMiuix(
    module: com.sukisu.ultra.ui.viewmodel.KpmViewModel.ModuleInfo,
    onUninstall: () -> Unit,
    onControl: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val onSurface = colorScheme.onSurface
    val secondaryContainer = colorScheme.secondaryContainer.copy(alpha = 0.8f)
    val actionIconTint = remember(isDark) { onSurface.copy(alpha = if (isDark) 0.7f else 0.9f) }

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                val kpmVersion = stringResource(R.string.kpm_version)
                val kpmAuthor = stringResource(R.string.kpm_author)
                val kpmArgs = stringResource(R.string.kpm_args)

                SubcomposeLayout { constraints ->
                    val namePlaceable = subcompose("name") {
                        Text(
                            text = module.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight(550),
                            color = colorScheme.onSurface,
                            onTextLayout = { }
                        )
                    }.first().measure(constraints)

                    layout(namePlaceable.width, namePlaceable.height) {
                        namePlaceable.placeRelative(0, 0)
                    }
                }
                Text(
                    text = "$kpmVersion: ${module.version}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary
                )
                Text(
                    text = "$kpmAuthor: ${module.author}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 1.dp),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary
                )
                if (module.args.isNotEmpty()) {
                    Text(
                        text = "$kpmArgs: ${module.args}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }

        if (module.description.isNotBlank()) {
            Text(
                text = module.description,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 4
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp,
            color = colorScheme.outline.copy(alpha = 0.5f)
        )

        Row {
            AnimatedVisibility(
                visible = module.hasAction,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    backgroundColor = secondaryContainer,
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    onClick = onControl,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Filled.Settings,
                        tint = actionIconTint,
                        contentDescription = stringResource(R.string.kpm_control)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                onClick = onUninstall,
                backgroundColor = secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Filled.Delete,
                        tint = actionIconTint,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 3.dp),
                        text = stringResource(R.string.kpm_uninstall),
                        color = actionIconTint,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateViewMiuix(
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
                tint = colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                stringResource(R.string.kpm_empty),
                textAlign = TextAlign.Center,
                color = colorScheme.onBackground
            )
        }
    }
}
