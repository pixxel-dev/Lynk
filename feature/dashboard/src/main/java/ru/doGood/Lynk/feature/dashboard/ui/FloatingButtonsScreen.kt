package ru.doGood.Lynk.feature.dashboard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lynk.core.domain.agent.OverlayProfile
import com.example.lynk.core.domain.app.AppItem
import com.example.lynk.core.domain.floating.FloatingButtonConfig
import ru.doGood.Lynk.feature.dashboard.FloatingButtonsState
import ru.doGood.Lynk.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingButtonsScreen(
    state: FloatingButtonsState,
    onSelectProfile: (Int) -> Unit = {},
    onToggleProfileEnabled: (Int, Boolean) -> Unit = { _, _ -> },
    onToggleQuickLaunch: (Boolean) -> Unit,
    onSetQuickLaunchSize: (Int) -> Unit,
    onSetQuickLaunchOpacity: (Int) -> Unit,
    onSetQuickLaunchColor: (String) -> Unit = {},
    onSetQuickLaunchShape: (String) -> Unit = {},
    onToggleFullscreenOverlay: (Boolean) -> Unit,
    onSetFullscreenSize: (Int) -> Unit,
    onSetFullscreenOpacity: (Int) -> Unit,
    onSetFullscreenColor: (String) -> Unit = {},
    onSetFullscreenShape: (String) -> Unit = {},
    onToggleHomeNavigator: (Boolean) -> Unit,
    onSetHomeSize: (Int) -> Unit,
    onSetHomeOpacity: (Int) -> Unit,
    onSetHomeColor: (String) -> Unit = {},
    onSetHomeShape: (String) -> Unit = {},
    onToggleBackNavigator: (Boolean) -> Unit,
    onSetBackSize: (Int) -> Unit,
    onSetBackOpacity: (Int) -> Unit,
    onSetBackColor: (String) -> Unit = {},
    onSetBackShape: (String) -> Unit = {},
    onToggleRefreshNavigator: (Boolean) -> Unit,
    onSetRefreshSize: (Int) -> Unit,
    onSetRefreshOpacity: (Int) -> Unit,
    onSetRefreshColor: (String) -> Unit = {},
    onSetRefreshShape: (String) -> Unit = {},
    onToggleFreeformWindow: (Boolean) -> Unit = {},
    onSetFreeformSize: (Int) -> Unit = {},
    onSetFreeformOpacity: (Int) -> Unit = {},
    onSetFreeformColor: (String) -> Unit = {},
    onSetFreeformShape: (String) -> Unit = {},
    onToggleSeparateButtons: (Boolean) -> Unit,
    onSetCombinedSize: (Int) -> Unit,
    onSetCombinedOpacity: (Int) -> Unit,
    onToggleSecondaryMirroring: (Boolean) -> Unit,
    onAddQuickLaunchApp: (String) -> Unit,
    onRemoveQuickLaunchApp: (String) -> Unit,
    onSetQuickLaunchApps: (List<String>) -> Unit = {},
    onAddFullscreenApp: (String) -> Unit,
    onRemoveFullscreenApp: (String) -> Unit,
    onSetFullscreenApps: (List<String>) -> Unit = {},
    onCheckPermissions: (Context) -> Unit,
    onToggleOverlayService: (Context) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val isServiceRunning = state.isServiceRunning
    val hasOverlayPermission = state.isOverlayPermissionGranted

    var showAddQuickLaunchDialog by remember { mutableStateOf(false) }
    var showAddFullscreenDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onCheckPermissions(context)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 2 else 1),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Service Status Card (Spans full width)
        item(span = { GridItemSpan(if (isLandscape) 2 else 1) }) {
            ServiceStatusCard(
                state = state,
                onToggleService = { onToggleOverlayService(context) },
                onOpenOverlaySettings = {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    }
                },
                onOpenUsageStatsSettings = {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    }
                }
            )
        }

        // Overlay Profiles TabRow (Spans full width)
        item(span = { GridItemSpan(if (isLandscape) 2 else 1) }) {
            TabRow(
                selectedTabIndex = (state.selectedProfileId - 1).coerceIn(0, 2),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val profileTabs = listOf(
                    1 to stringResource(R.string.overlay_profile_1),
                    2 to stringResource(R.string.overlay_profile_2),
                    3 to stringResource(R.string.overlay_profile_3)
                )
                profileTabs.forEach { (profileId, label) ->
                    val isSelected = state.selectedProfileId == profileId
                    Tab(
                        selected = isSelected,
                        onClick = { onSelectProfile(profileId) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }
        }

        // Active Profile Status Card & Button Configuration Cards
        item(span = { GridItemSpan(if (isLandscape) 2 else 1) }) {
            AnimatedVisibility(visible = isServiceRunning && hasOverlayPermission) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileStatusCard(
                        profile = state.activeProfile,
                        onToggleProfileEnabled = { enabled ->
                            onToggleProfileEnabled(state.selectedProfileId, enabled)
                        }
                    )

                    AnimatedVisibility(visible = state.activeProfile.isEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isLandscape) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                QuickLaunchCard(
                                    state = state,
                                    onToggleQuickLaunch = onToggleQuickLaunch,
                                    onSetQuickLaunchSize = onSetQuickLaunchSize,
                                    onSetQuickLaunchOpacity = onSetQuickLaunchOpacity,
                                    onSetQuickLaunchColor = onSetQuickLaunchColor,
                                    onSetQuickLaunchShape = onSetQuickLaunchShape,
                                    onRemoveQuickLaunchApp = onRemoveQuickLaunchApp,
                                    onShowAddDialog = { showAddQuickLaunchDialog = true }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FullscreenOverlayCard(
                                    state = state,
                                    onToggleFullscreenOverlay = onToggleFullscreenOverlay,
                                    onSetFullscreenSize = onSetFullscreenSize,
                                    onSetFullscreenOpacity = onSetFullscreenOpacity,
                                    onSetFullscreenColor = onSetFullscreenColor,
                                    onSetFullscreenShape = onSetFullscreenShape,
                                    onRemoveFullscreenApp = onRemoveFullscreenApp,
                                    onShowAddDialog = { showAddFullscreenDialog = true }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                HomeNavigatorCard(
                                    state = state,
                                    onToggleHomeNavigator = onToggleHomeNavigator,
                                    onSetHomeSize = onSetHomeSize,
                                    onSetHomeOpacity = onSetHomeOpacity,
                                    onSetHomeColor = onSetHomeColor,
                                    onSetHomeShape = onSetHomeShape
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BackNavigatorCard(
                                    state = state,
                                    onToggleBackNavigator = onToggleBackNavigator,
                                    onSetBackSize = onSetBackSize,
                                    onSetBackOpacity = onSetBackOpacity,
                                    onSetBackColor = onSetBackColor,
                                    onSetBackShape = onSetBackShape
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                RefreshButtonCard(
                                    state = state,
                                    onToggleRefreshNavigator = onToggleRefreshNavigator,
                                    onSetRefreshSize = onSetRefreshSize,
                                    onSetRefreshOpacity = onSetRefreshOpacity,
                                    onSetRefreshColor = onSetRefreshColor,
                                    onSetRefreshShape = onSetRefreshShape
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FreeformWindowCard(
                                    state = state,
                                    onToggleFreeformWindow = onToggleFreeformWindow,
                                    onSetFreeformSize = onSetFreeformSize,
                                    onSetFreeformOpacity = onSetFreeformOpacity,
                                    onSetFreeformColor = onSetFreeformColor,
                                    onSetFreeformShape = onSetFreeformShape
                                )
                            }
                        }
                        ButtonLayoutCard(
                            config = state.config,
                            onToggleSeparateButtons = onToggleSeparateButtons,
                            onSetCombinedSize = onSetCombinedSize,
                            onSetCombinedOpacity = onSetCombinedOpacity,
                            onToggleSecondaryMirroring = onToggleSecondaryMirroring
                        )
                    } else {
                        QuickLaunchCard(
                            state = state,
                            onToggleQuickLaunch = onToggleQuickLaunch,
                            onSetQuickLaunchSize = onSetQuickLaunchSize,
                            onSetQuickLaunchOpacity = onSetQuickLaunchOpacity,
                            onSetQuickLaunchColor = onSetQuickLaunchColor,
                            onSetQuickLaunchShape = onSetQuickLaunchShape,
                            onRemoveQuickLaunchApp = onRemoveQuickLaunchApp,
                            onShowAddDialog = { showAddQuickLaunchDialog = true }
                        )
                        FullscreenOverlayCard(
                            state = state,
                            onToggleFullscreenOverlay = onToggleFullscreenOverlay,
                            onSetFullscreenSize = onSetFullscreenSize,
                            onSetFullscreenOpacity = onSetFullscreenOpacity,
                            onSetFullscreenColor = onSetFullscreenColor,
                            onSetFullscreenShape = onSetFullscreenShape,
                            onRemoveFullscreenApp = onRemoveFullscreenApp,
                            onShowAddDialog = { showAddFullscreenDialog = true }
                        )
                        HomeNavigatorCard(
                            state = state,
                            onToggleHomeNavigator = onToggleHomeNavigator,
                            onSetHomeSize = onSetHomeSize,
                            onSetHomeOpacity = onSetHomeOpacity,
                            onSetHomeColor = onSetHomeColor,
                            onSetHomeShape = onSetHomeShape
                        )
                        BackNavigatorCard(
                            state = state,
                            onToggleBackNavigator = onToggleBackNavigator,
                            onSetBackSize = onSetBackSize,
                            onSetBackOpacity = onSetBackOpacity,
                            onSetBackColor = onSetBackColor,
                            onSetBackShape = onSetBackShape
                        )
                        RefreshButtonCard(
                            state = state,
                            onToggleRefreshNavigator = onToggleRefreshNavigator,
                            onSetRefreshSize = onSetRefreshSize,
                            onSetRefreshOpacity = onSetRefreshOpacity,
                            onSetRefreshColor = onSetRefreshColor,
                            onSetRefreshShape = onSetRefreshShape
                        )
                        FreeformWindowCard(
                            state = state,
                            onToggleFreeformWindow = onToggleFreeformWindow,
                            onSetFreeformSize = onSetFreeformSize,
                            onSetFreeformOpacity = onSetFreeformOpacity,
                            onSetFreeformColor = onSetFreeformColor,
                            onSetFreeformShape = onSetFreeformShape
                        )
                        ButtonLayoutCard(
                            config = state.config,
                            onToggleSeparateButtons = onToggleSeparateButtons,
                            onSetCombinedSize = onSetCombinedSize,
                            onSetCombinedOpacity = onSetCombinedOpacity,
                            onToggleSecondaryMirroring = onToggleSecondaryMirroring
                        )
                    }
                }
            }
        }
    }
}
}

    // Add App to Quick Launch Dialog
    if (showAddQuickLaunchDialog) {
        SelectAppDialog(
            title = stringResource(R.string.add_quick_launch_title),
            availableApps = state.availableApps,
            existingApps = state.config.quickLaunchApps,
            onDismiss = { showAddQuickLaunchDialog = false },
            onConfirm = { selectedApps ->
                onSetQuickLaunchApps(selectedApps)
                showAddQuickLaunchDialog = false
            }
        )
    }

    // Add App to Fullscreen Targets Dialog
    if (showAddFullscreenDialog) {
        SelectAppDialog(
            title = stringResource(R.string.add_fullscreen_app_title),
            availableApps = state.availableApps,
            existingApps = state.config.fullscreenApps,
            onDismiss = { showAddFullscreenDialog = false },
            onConfirm = { selectedApps ->
                onSetFullscreenApps(selectedApps)
                showAddFullscreenDialog = false
            }
        )
    }
}

@Composable
private fun QuickLaunchCard(
    state: FloatingButtonsState,
    onToggleQuickLaunch: (Boolean) -> Unit,
    onSetQuickLaunchSize: (Int) -> Unit,
    onSetQuickLaunchOpacity: (Int) -> Unit,
    onSetQuickLaunchColor: (String) -> Unit,
    onSetQuickLaunchShape: (String) -> Unit,
    onRemoveQuickLaunchApp: (String) -> Unit,
    onShowAddDialog: () -> Unit
) {
    val context = LocalContext.current
    FloatingButtonCard(
        icon = Icons.Rounded.Menu,
        title = stringResource(R.string.quick_launch_btn_title),
        subtitle = stringResource(R.string.quick_launch_btn_subtitle),
        enabled = state.config.isQuickLaunchEnabled,
        onToggleEnabled = onToggleQuickLaunch,
        isSeparateButtons = state.config.isSeparateButtonsEnabled,
        sizeDp = state.config.quickLaunchButtonSize,
        onSetSize = onSetQuickLaunchSize,
        opacityPercent = state.config.quickLaunchOpacityPercent,
        onSetOpacity = onSetQuickLaunchOpacity,
        selectedColorHex = state.config.quickLaunchColorHex,
        onSetColorHex = onSetQuickLaunchColor,
        selectedShape = state.config.quickLaunchShape,
        onSetShape = onSetQuickLaunchShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.quick_launch_apps_label, state.config.quickLaunchApps.size),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onShowAddDialog) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_application))
            }
        }

        if (state.config.quickLaunchApps.isEmpty()) {
            Text(
                text = stringResource(R.string.no_apps_added),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(state.config.quickLaunchApps) { pkg ->
                    val appLabel = remember(pkg, state.availableApps) {
                        getAppLabel(context, pkg, state.availableApps)
                    }
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = { Text(appLabel) },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.btn_remove),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveQuickLaunchApp(pkg) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenOverlayCard(
    state: FloatingButtonsState,
    onToggleFullscreenOverlay: (Boolean) -> Unit,
    onSetFullscreenSize: (Int) -> Unit,
    onSetFullscreenOpacity: (Int) -> Unit,
    onSetFullscreenColor: (String) -> Unit,
    onSetFullscreenShape: (String) -> Unit,
    onRemoveFullscreenApp: (String) -> Unit,
    onShowAddDialog: () -> Unit
) {
    val context = LocalContext.current
    FloatingButtonCard(
        icon = Icons.Rounded.Fullscreen,
        title = stringResource(R.string.fullscreen_overlay_title),
        subtitle = stringResource(R.string.fullscreen_overlay_subtitle),
        enabled = state.config.isFullscreenOverlayEnabled,
        onToggleEnabled = onToggleFullscreenOverlay,
        isSeparateButtons = state.config.isSeparateButtonsEnabled,
        sizeDp = state.config.fullscreenButtonSize,
        onSetSize = onSetFullscreenSize,
        opacityPercent = state.config.fullscreenOpacityPercent,
        onSetOpacity = onSetFullscreenOpacity,
        selectedColorHex = state.config.fullscreenColorHex,
        onSetColorHex = onSetFullscreenColor,
        selectedShape = state.config.fullscreenShape,
        onSetShape = onSetFullscreenShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.target_apps_label, state.config.fullscreenApps.size),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onShowAddDialog) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_application))
            }
        }

        if (state.config.fullscreenApps.isEmpty()) {
            Text(
                text = stringResource(R.string.no_target_apps),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(state.config.fullscreenApps) { pkg ->
                    val appLabel = remember(pkg, state.availableApps) {
                        getAppLabel(context, pkg, state.availableApps)
                    }
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = { Text(appLabel) },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.btn_remove),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveFullscreenApp(pkg) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeNavigatorCard(
    state: FloatingButtonsState,
    onToggleHomeNavigator: (Boolean) -> Unit,
    onSetHomeSize: (Int) -> Unit,
    onSetHomeOpacity: (Int) -> Unit,
    onSetHomeColor: (String) -> Unit,
    onSetHomeShape: (String) -> Unit
) {
    FloatingButtonCard(
        icon = Icons.Rounded.Home,
        title = stringResource(R.string.home_navigator_title),
        subtitle = stringResource(R.string.home_navigator_subtitle),
        enabled = state.config.isHomeNavigatorEnabled,
        onToggleEnabled = onToggleHomeNavigator,
        isSeparateButtons = state.config.isSeparateButtonsEnabled,
        sizeDp = state.config.homeButtonSize,
        onSetSize = onSetHomeSize,
        opacityPercent = state.config.homeOpacityPercent,
        onSetOpacity = onSetHomeOpacity,
        selectedColorHex = state.config.homeColorHex,
        onSetColorHex = onSetHomeColor,
        selectedShape = state.config.homeShape,
        onSetShape = onSetHomeShape
    )
}

@Composable
private fun BackNavigatorCard(
    state: FloatingButtonsState,
    onToggleBackNavigator: (Boolean) -> Unit,
    onSetBackSize: (Int) -> Unit,
    onSetBackOpacity: (Int) -> Unit,
    onSetBackColor: (String) -> Unit,
    onSetBackShape: (String) -> Unit
) {
    FloatingButtonCard(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        title = stringResource(R.string.back_navigator_title),
        subtitle = stringResource(R.string.back_navigator_subtitle),
        enabled = state.config.isBackNavigatorEnabled,
        onToggleEnabled = onToggleBackNavigator,
        isSeparateButtons = state.config.isSeparateButtonsEnabled,
        sizeDp = state.config.backButtonSize,
        onSetSize = onSetBackSize,
        opacityPercent = state.config.backOpacityPercent,
        onSetOpacity = onSetBackOpacity,
        selectedColorHex = state.config.backColorHex,
        onSetColorHex = onSetBackColor,
        selectedShape = state.config.backShape,
        onSetShape = onSetBackShape
    )
}

@Composable
private fun RefreshButtonCard(
    state: FloatingButtonsState,
    onToggleRefreshNavigator: (Boolean) -> Unit,
    onSetRefreshSize: (Int) -> Unit,
    onSetRefreshOpacity: (Int) -> Unit,
    onSetRefreshColor: (String) -> Unit,
    onSetRefreshShape: (String) -> Unit
) {
    FloatingButtonCard(
        icon = Icons.Rounded.Refresh,
        title = stringResource(R.string.refresh_button_title),
        subtitle = stringResource(R.string.refresh_button_subtitle),
        enabled = state.config.isRefreshNavigatorEnabled,
        onToggleEnabled = onToggleRefreshNavigator,
        isSeparateButtons = state.config.isSeparateButtonsEnabled,
        sizeDp = state.config.refreshButtonSize,
        onSetSize = onSetRefreshSize,
        opacityPercent = state.config.refreshOpacityPercent,
        onSetOpacity = onSetRefreshOpacity,
        selectedColorHex = state.config.refreshColorHex,
        onSetColorHex = onSetRefreshColor,
        selectedShape = state.config.refreshShape,
        onSetShape = onSetRefreshShape
    )
}

@Composable
private fun FreeformWindowCard(
    state: FloatingButtonsState,
    onToggleFreeformWindow: (Boolean) -> Unit,
    onSetFreeformSize: (Int) -> Unit,
    onSetFreeformOpacity: (Int) -> Unit,
    onSetFreeformColor: (String) -> Unit,
    onSetFreeformShape: (String) -> Unit
) {
    FloatingButtonCard(
        icon = Icons.Rounded.PictureInPicture,
        title = stringResource(R.string.freeform_window_title),
        subtitle = stringResource(R.string.freeform_window_subtitle),
        enabled = state.config.isFreeformWindowEnabled,
        onToggleEnabled = onToggleFreeformWindow,
        isSeparateButtons = state.config.isSeparateButtonsEnabled,
        sizeDp = state.config.freeformButtonSize,
        onSetSize = onSetFreeformSize,
        opacityPercent = state.config.freeformOpacityPercent,
        onSetOpacity = onSetFreeformOpacity,
        selectedColorHex = state.config.freeformColorHex,
        onSetColorHex = onSetFreeformColor,
        selectedShape = state.config.freeformShape,
        onSetShape = onSetFreeformShape
    )
}

@Composable
private fun ServiceStatusCard(
    state: FloatingButtonsState,
    onToggleService: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenUsageStatsSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isServiceRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (state.isServiceRunning) Icons.Rounded.PlayArrow else Icons.Rounded.Stop,
                        contentDescription = null,
                        tint = if (state.isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isServiceRunning) stringResource(R.string.service_running) else stringResource(R.string.service_stopped),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (state.isServiceRunning) stringResource(R.string.service_running_desc) else stringResource(R.string.service_stopped_desc),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onToggleService) {
                    Text(if (state.isServiceRunning) stringResource(R.string.btn_stop_service) else stringResource(R.string.btn_start_service))
                }
            }

            if (!state.isOverlayPermissionGranted || !state.isUsageStatsPermissionGranted) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = stringResource(R.string.system_permissions_required),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!state.isOverlayPermissionGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.perm_overlay_display), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenOverlaySettings) {
                            Text(stringResource(R.string.btn_allow))
                        }
                    }
                }

                if (!state.isUsageStatsPermissionGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.perm_usage_history), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenUsageStatsSettings) {
                            Text(stringResource(R.string.btn_allow))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingButtonCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    isSeparateButtons: Boolean,
    sizeDp: Int,
    onSetSize: (Int) -> Unit,
    opacityPercent: Int,
    onSetOpacity: (Int) -> Unit,
    selectedColorHex: String = "#6750A4",
    onSetColorHex: (String) -> Unit = {},
    selectedShape: String = "CIRCLE",
    onSetShape: (String) -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (content != null) {
                        content()
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // Color Selector
                    Text(
                        text = stringResource(R.string.button_color_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val colorPresets = remember {
                        listOf("#6750A4", "#1976D2", "#388E3C", "#D32F2F", "#1C1B1F", "#FFA000")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorPresets.forEach { hex ->
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            val parsedColor = remember(hex) {
                                try {
                                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                                } catch (_: Exception) {
                                    androidx.compose.ui.graphics.Color.Magenta
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.5.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            )
                                        } else Modifier
                                    )
                                    .clickable { onSetColorHex(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shape Selector
                    Text(
                        text = stringResource(R.string.button_shape_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val shapes = remember {
                        listOf(
                            "CIRCLE" to ("●" to R.string.shape_circle),
                            "ROUNDED_SQUARE" to ("▢" to R.string.shape_rounded_square),
                            "SQUARE" to ("■" to R.string.shape_square),
                            "STAR" to ("★" to R.string.shape_star),
                            "OCTAGON" to ("🛑" to R.string.shape_octagon),
                            "HEART" to ("♥" to R.string.shape_heart)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        shapes.forEach { (shapeKey, iconAndRes) ->
                            val (symbol, stringRes) = iconAndRes
                            val isSelected = selectedShape == shapeKey
                            val labelDescription = stringResource(stringRes)

                            val backgroundColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val borderColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSetShape(shapeKey) }
                                    .semantics {
                                        contentDescription = labelDescription
                                    }
                            ) {
                                Text(
                                    text = symbol,
                                    fontSize = 26.sp,
                                    color = contentColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSeparateButtons) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Text(
                                text = stringResource(R.string.button_size_format, sizeDp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = sizeDp.toFloat(),
                                onValueChange = { onSetSize(it.toInt()) },
                                valueRange = 30f..120f
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(R.string.button_opacity_format, opacityPercent),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = opacityPercent.toFloat(),
                                onValueChange = { onSetOpacity(it.toInt()) },
                                valueRange = 10f..100f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonLayoutCard(
    config: FloatingButtonConfig,
    onToggleSeparateButtons: (Boolean) -> Unit,
    onSetCombinedSize: (Int) -> Unit,
    onSetCombinedOpacity: (Int) -> Unit,
    onToggleSecondaryMirroring: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.overlay_display_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sep_buttons_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.sep_buttons_subtitle),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = config.isSeparateButtonsEnabled,
                    onCheckedChange = onToggleSeparateButtons
                )
            }

            AnimatedVisibility(visible = !config.isSeparateButtonsEnabled) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = stringResource(R.string.combined_button_size_format, config.combinedButtonSize),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = config.combinedButtonSize.toFloat(),
                        onValueChange = { onSetCombinedSize(it.toInt()) },
                        valueRange = 30f..120f
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.combined_opacity_format, config.opacityPercent),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = config.opacityPercent.toFloat(),
                        onValueChange = { onSetCombinedOpacity(it.toInt()) },
                        valueRange = 10f..100f
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.mirroring_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.mirroring_subtitle),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = config.isSecondaryDisplayMirroring,
                    onCheckedChange = onToggleSecondaryMirroring
                )
            }
        }
    }
}

private fun getAppLabel(
    context: Context,
    packageName: String,
    availableApps: List<AppItem> = emptyList()
): String {
    val foundApp = availableApps.find { it.packageName == packageName }
    if (foundApp != null && !foundApp.label.isNullOrBlank()) {
        return foundApp.label
    }
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}

@Composable
private fun SelectAppDialog(
    title: String,
    availableApps: List<AppItem>,
    existingApps: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(existingApps.toSet()) }

    val filteredList = remember(searchQuery, availableApps) {
        if (searchQuery.isBlank()) {
            availableApps
        } else {
            val query = searchQuery.trim().lowercase()
            availableApps.filter { app ->
                (app.label ?: "").lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val trimmedQuery = searchQuery.trim()
                if (trimmedQuery.isNotEmpty() && filteredList.none { it.packageName.equals(trimmedQuery, ignoreCase = true) }) {
                    val isCustomSelected = selectedPackages.contains(trimmedQuery)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPackages = if (isCustomSelected) {
                                    selectedPackages - trimmedQuery
                                } else {
                                    selectedPackages + trimmedQuery
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = isCustomSelected,
                                onCheckedChange = { checked ->
                                    selectedPackages = if (checked) {
                                        selectedPackages + trimmedQuery
                                    } else {
                                        selectedPackages - trimmedQuery
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.btn_add_custom) + ": $trimmedQuery",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = trimmedQuery,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.select_from_installed),
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (selectedPackages.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.selected_apps_count, selectedPackages.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredList.isEmpty() && trimmedQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_apps_added),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(filteredList, key = { it.packageName }) { app ->
                            val isChecked = selectedPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPackages = if (isChecked) {
                                            selectedPackages - app.packageName
                                        } else {
                                            selectedPackages + app.packageName
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedPackages = if (checked) {
                                            selectedPackages + app.packageName
                                        } else {
                                            selectedPackages - app.packageName
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label ?: app.packageName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPackages.toList()) }
            ) {
                Text(stringResource(R.string.btn_save_apps))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun ProfileStatusCard(
    profile: OverlayProfile,
    onToggleProfileEnabled: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (profile.isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${profile.name}: ${stringResource(R.string.enable_overlay_profile)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.overlay_profile_status_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = profile.isEnabled,
                onCheckedChange = onToggleProfileEnabled
            )
        }
    }
}
