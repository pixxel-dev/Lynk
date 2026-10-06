package ru.doGood.Lynk.feature.dashboard.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.lynk.core.domain.file.FileItem
import ru.doGood.Lynk.feature.dashboard.DashboardViewModel
import ru.doGood.Lynk.feature.dashboard.R
import ru.doGood.Lynk.feature.dashboard.ThemeMode

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardMainScreen(
    viewModel: DashboardViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    val useSideNav = isWideScreen || isLandscape

    val context = LocalContext.current

    BackHandler {
        if (state.selectedTab != 0) {
            viewModel.setSelectedTab(0)
        } else {
            context.findActivity()?.finish()
        }
    }

    var filePropertiesToShow by remember { mutableStateOf<FileItem?>(null) }

    val themeIcon = when (state.themeMode) {
        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }

    val themeLabel = when (state.themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = "Lynk",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state.selectedTab) {
                                0 -> stringResource(R.string.title_backlog)
                                1 -> stringResource(R.string.title_files)
                                2 -> stringResource(R.string.title_installer)
                                3 -> stringResource(R.string.title_system)
                                4 -> stringResource(R.string.title_buttons)
                                5 -> stringResource(R.string.title_update)
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick App Update Shortcut
                    IconButton(onClick = { viewModel.setSelectedTab(5) }) {
                        if (state.updateState.updateInfo?.state == com.example.lynk.core.domain.update.UpdateInfo.UpdateState.UPDATE_AVAILABLE) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("!")
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = stringResource(R.string.tab_update)
                        )
                    }

                    // Theme Mode Switcher
                    IconButton(onClick = { viewModel.cycleThemeMode() }) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = themeLabel
                        )
                    }
                }
            )
        }
    ) { topPaddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(topPaddingValues)
        ) {
            // Navigation Rail for Wide Screens / Landscape Mode
            if (useSideNav) {
                NavigationRail(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp, androidx.compose.ui.Alignment.CenterVertically)
                    ) {
                        val railItems = listOf(
                            Triple(0, R.string.tab_backlog, Icons.Rounded.Task),
                            Triple(1, R.string.tab_files, Icons.Rounded.Folder),
                            Triple(2, R.string.tab_installer, Icons.Rounded.Android),
                            Triple(3, R.string.tab_system, Icons.Rounded.Info),
                            Triple(4, R.string.tab_buttons, Icons.Rounded.Widgets),
                            Triple(5, R.string.tab_update, Icons.Rounded.SystemUpdate)
                        )

                        railItems.forEach { (tabIndex, stringResId, iconVector) ->
                            val labelText = stringResource(stringResId)
                            NavigationRailItem(
                                selected = state.selectedTab == tabIndex,
                                onClick = { viewModel.setSelectedTab(tabIndex) },
                                icon = {
                                    if (tabIndex == 5 && state.updateState.updateInfo?.state == com.example.lynk.core.domain.update.UpdateInfo.UpdateState.UPDATE_AVAILABLE) {
                                        BadgedBox(
                                            badge = { Badge { Text("!") } }
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = labelText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = labelText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(vertical = 0.dp)
                            )
                        }
                    }
                }
            }

            Scaffold(
                bottomBar = {
                    // Bottom Navigation Bar for Mobile Portrait Screen
                    if (!useSideNav) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = state.selectedTab == 0,
                                onClick = { viewModel.setSelectedTab(0) },
                                icon = { Icon(Icons.Rounded.Task, contentDescription = stringResource(R.string.tab_backlog)) },
                                label = { Text(stringResource(R.string.tab_backlog)) }
                            )
                            NavigationBarItem(
                                selected = state.selectedTab == 1,
                                onClick = { viewModel.setSelectedTab(1) },
                                icon = { Icon(Icons.Rounded.Folder, contentDescription = stringResource(R.string.tab_files)) },
                                label = { Text(stringResource(R.string.tab_files)) }
                            )
                            NavigationBarItem(
                                selected = state.selectedTab == 2,
                                onClick = { viewModel.setSelectedTab(2) },
                                icon = { Icon(Icons.Rounded.Android, contentDescription = stringResource(R.string.tab_installer)) },
                                label = { Text(stringResource(R.string.tab_installer)) }
                            )
                            NavigationBarItem(
                                selected = state.selectedTab == 3,
                                onClick = { viewModel.setSelectedTab(3) },
                                icon = { Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.tab_system)) },
                                label = { Text(stringResource(R.string.tab_system)) }
                            )
                            NavigationBarItem(
                                selected = state.selectedTab == 4,
                                onClick = { viewModel.setSelectedTab(4) },
                                icon = { Icon(Icons.Rounded.Widgets, contentDescription = stringResource(R.string.tab_buttons)) },
                                label = { Text(stringResource(R.string.tab_buttons)) }
                            )
                            NavigationBarItem(
                                selected = state.selectedTab == 5,
                                onClick = { viewModel.setSelectedTab(5) },
                                icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = stringResource(R.string.tab_update)) },
                                label = { Text(stringResource(R.string.tab_update)) }
                            )
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (state.selectedTab) {
                        0 -> BacklogScreen(
                            backlogItems = state.backlogItems,
                            auditResults = state.auditResults,
                            onItemClick = { item -> onNavigateToDetail(item.id) },
                            onRunAuditClick = { viewModel.runAudit() }
                        )

                        1 -> FileManagerScreen(
                            currentPath = state.fileManagerState.currentPath,
                            files = state.fileManagerState.filteredFiles,
                            searchQuery = state.fileManagerState.searchQuery,
                            sortType = state.fileManagerState.sortType,
                            sortAscending = state.fileManagerState.sortAscending,
                            selectedFiles = state.fileManagerState.selectedFiles,
                            clipboardCount = state.fileManagerState.clipboardFiles.size,
                            isCutOperation = state.fileManagerState.isCutOperation,
                            freeSpaceFormatted = formatFileSize(state.fileManagerState.freeSpaceBytes),
                            totalSpaceFormatted = formatFileSize(state.fileManagerState.totalSpaceBytes),
                            usedPercentage = if (state.fileManagerState.totalSpaceBytes > 0) {
                                1f - (state.fileManagerState.freeSpaceBytes.toFloat() / state.fileManagerState.totalSpaceBytes.toFloat())
                            } else 0f,
                            onPathClick = { path -> viewModel.loadDirectory(path) },
                            onNavigateUp = { viewModel.navigateUp() },
                            onFileClick = { file ->
                                if (file.isUpNavigation) {
                                    viewModel.navigateUp()
                                } else if (file.isDirectory) {
                                    viewModel.loadDirectory(file.path)
                                } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                    viewModel.selectApkForInstallation(file.path)
                                } else {
                                    filePropertiesToShow = file
                                }
                            },
                            onFileLongClick = { file ->
                                if (!file.isUpNavigation) {
                                    viewModel.toggleFileSelection(file.path)
                                }
                            },
                            onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                            onSortTypeChange = { sortType -> viewModel.setSortType(sortType) },
                            onToggleSortDirection = { viewModel.toggleSortDirection() },
                            onCopySelected = { viewModel.copySelectedFiles() },
                            onCutSelected = { viewModel.cutSelectedFiles() },
                            onPaste = { viewModel.pasteFiles() },
                            onDeleteSelected = { viewModel.deleteSelectedFiles() },
                            onClearSelection = { viewModel.clearSelection() },
                            onInstallApk = { file -> viewModel.selectApkForInstallation(file.path) },
                            onShowProperties = { file -> filePropertiesToShow = file }
                        )

                        2 -> ApkInstallerScreen(
                            selectedApkPath = state.installerState.selectedApkPath,
                            isInstalling = state.installerState.isInstalling,
                            installResult = state.installerState.installResult,
                            activeStep = state.installerState.activeStep,
                            stepStatuses = state.installerState.stepStatuses,
                            installedApps = state.installerState.installedApps,
                            onSelectApkClick = { viewModel.setSelectedTab(1) },
                            onStartWaterfallInstall = { viewModel.runWaterfallInstall() },
                            onInstallByStep = { step -> viewModel.runSingleInstallStep(step) },
                            onToggleAppChecked = { app, checked -> viewModel.toggleAppChecked(app, checked) }
                        )

                        3 -> SystemInfoScreen(
                            deviceInfo = state.systemInfoState.deviceInfo,
                            isRecordingLogs = state.systemInfoState.isRecordingLogs,
                            logsText = state.systemInfoState.logsText,
                            onStartRecording = { viewModel.startRecordingLogs() },
                            onStopRecording = { viewModel.stopRecordingLogs() },
                            onClearLogs = { viewModel.clearLogs() },
                            viewModel = viewModel,
                            onRefreshPermissions = { viewModel.refreshPermissions() }
                        )

                        4 -> FloatingButtonsScreen(
                            state = state.floatingButtonsState,
                            onToggleQuickLaunch = { viewModel.toggleQuickLaunchEnabled(it) },
                            onSetQuickLaunchSize = { viewModel.setQuickLaunchSize(it) },
                            onSetQuickLaunchOpacity = { viewModel.setQuickLaunchOpacityPercent(it) },
                            onToggleFullscreenOverlay = { viewModel.toggleFullscreenOverlayEnabled(it) },
                            onSetFullscreenSize = { viewModel.setFullscreenSize(it) },
                            onSetFullscreenOpacity = { viewModel.setFullscreenOpacityPercent(it) },
                            onToggleHomeNavigator = { viewModel.toggleHomeNavigatorEnabled(it) },
                            onSetHomeSize = { viewModel.setHomeSize(it) },
                            onSetHomeOpacity = { viewModel.setHomeOpacityPercent(it) },
                            onToggleBackNavigator = { viewModel.toggleBackNavigatorEnabled(it) },
                            onSetBackSize = { viewModel.setBackSize(it) },
                            onSetBackOpacity = { viewModel.setBackOpacityPercent(it) },
                            onToggleRefreshNavigator = { viewModel.toggleRefreshNavigatorEnabled(it) },
                            onSetRefreshSize = { viewModel.setRefreshSize(it) },
                            onSetRefreshOpacity = { viewModel.setRefreshOpacityPercent(it) },
                            onToggleSeparateButtons = { viewModel.toggleSeparateButtonsEnabled(it) },
                            onSetCombinedSize = { viewModel.setCombinedSize(it) },
                            onSetCombinedOpacity = { viewModel.setOpacityPercent(it) },
                            onToggleSecondaryMirroring = { viewModel.toggleSecondaryMirroring(it) },
                            onAddQuickLaunchApp = { viewModel.addQuickLaunchApp(it) },
                            onRemoveQuickLaunchApp = { viewModel.removeQuickLaunchApp(it) },
                            onAddFullscreenApp = { viewModel.addFullscreenApp(it) },
                            onRemoveFullscreenApp = { viewModel.removeFullscreenApp(it) },
                            onCheckPermissions = { context -> viewModel.checkFloatingPermissions(context) },
                            onToggleOverlayService = { context -> viewModel.toggleOverlayService(context) }
                        )

                        5 -> AppUpdateScreen(
                            currentVersion = "1.0.0",
                            updateInfo = state.updateState.updateInfo,
                            isChecking = state.updateState.isChecking,
                            isDownloading = state.updateState.isDownloading,
                            downloadProgress = state.updateState.downloadProgress,
                            statusMessage = state.updateState.statusMessage,
                            onCheckForUpdates = { viewModel.checkForUpdates() },
                            onStartDownload = { viewModel.startDownloadUpdate() }
                        )
                    }
                }
            }
        }
    }

    // File Properties Dialog
    filePropertiesToShow?.let { file ->
        AlertDialog(
            onDismissRequest = { filePropertiesToShow = null },
            title = { Text(stringResource(R.string.file_properties_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.file_prop_name, file.name), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.file_prop_path, file.path), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.file_prop_size, formatFileSize(file.size)), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.file_prop_type, if (file.isDirectory) stringResource(R.string.file_type_folder) else stringResource(R.string.file_type_file)), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { filePropertiesToShow = null }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}
