package ru.doGood.Lynk.feature.dashboard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.app.AppItem
import com.example.lynk.core.domain.floating.FloatingButtonConfig
import ru.doGood.Lynk.feature.dashboard.FloatingButtonsState
import ru.doGood.Lynk.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingButtonsScreen(
    state: FloatingButtonsState,
    onToggleQuickLaunch: (Boolean) -> Unit,
    onSetQuickLaunchSize: (Int) -> Unit,
    onSetQuickLaunchOpacity: (Int) -> Unit,
    onToggleFullscreenOverlay: (Boolean) -> Unit,
    onSetFullscreenSize: (Int) -> Unit,
    onSetFullscreenOpacity: (Int) -> Unit,
    onToggleHomeNavigator: (Boolean) -> Unit,
    onSetHomeSize: (Int) -> Unit,
    onSetHomeOpacity: (Int) -> Unit,
    onToggleBackNavigator: (Boolean) -> Unit,
    onSetBackSize: (Int) -> Unit,
    onSetBackOpacity: (Int) -> Unit,
    onToggleRefreshNavigator: (Boolean) -> Unit,
    onSetRefreshSize: (Int) -> Unit,
    onSetRefreshOpacity: (Int) -> Unit,
    onToggleSeparateButtons: (Boolean) -> Unit,
    onSetCombinedSize: (Int) -> Unit,
    onSetCombinedOpacity: (Int) -> Unit,
    onToggleSecondaryMirroring: (Boolean) -> Unit,
    onAddQuickLaunchApp: (String) -> Unit,
    onRemoveQuickLaunchApp: (String) -> Unit,
    onAddFullscreenApp: (String) -> Unit,
    onRemoveFullscreenApp: (String) -> Unit,
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

        // All floating button configuration cards wrapped in AnimatedVisibility
        item(span = { GridItemSpan(if (isLandscape) 2 else 1) }) {
            AnimatedVisibility(visible = isServiceRunning && hasOverlayPermission) {
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
                                    onSetHomeOpacity = onSetHomeOpacity
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BackNavigatorCard(
                                    state = state,
                                    onToggleBackNavigator = onToggleBackNavigator,
                                    onSetBackSize = onSetBackSize,
                                    onSetBackOpacity = onSetBackOpacity
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
                                    onSetRefreshOpacity = onSetRefreshOpacity
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ButtonLayoutCard(
                                    config = state.config,
                                    onToggleSeparateButtons = onToggleSeparateButtons,
                                    onSetCombinedSize = onSetCombinedSize,
                                    onSetCombinedOpacity = onSetCombinedOpacity,
                                    onToggleSecondaryMirroring = onToggleSecondaryMirroring
                                )
                            }
                        }
                    } else {
                        QuickLaunchCard(
                            state = state,
                            onToggleQuickLaunch = onToggleQuickLaunch,
                            onSetQuickLaunchSize = onSetQuickLaunchSize,
                            onSetQuickLaunchOpacity = onSetQuickLaunchOpacity,
                            onRemoveQuickLaunchApp = onRemoveQuickLaunchApp,
                            onShowAddDialog = { showAddQuickLaunchDialog = true }
                        )
                        FullscreenOverlayCard(
                            state = state,
                            onToggleFullscreenOverlay = onToggleFullscreenOverlay,
                            onSetFullscreenSize = onSetFullscreenSize,
                            onSetFullscreenOpacity = onSetFullscreenOpacity,
                            onRemoveFullscreenApp = onRemoveFullscreenApp,
                            onShowAddDialog = { showAddFullscreenDialog = true }
                        )
                        HomeNavigatorCard(
                            state = state,
                            onToggleHomeNavigator = onToggleHomeNavigator,
                            onSetHomeSize = onSetHomeSize,
                            onSetHomeOpacity = onSetHomeOpacity
                        )
                        BackNavigatorCard(
                            state = state,
                            onToggleBackNavigator = onToggleBackNavigator,
                            onSetBackSize = onSetBackSize,
                            onSetBackOpacity = onSetBackOpacity
                        )
                        RefreshButtonCard(
                            state = state,
                            onToggleRefreshNavigator = onToggleRefreshNavigator,
                            onSetRefreshSize = onSetRefreshSize,
                            onSetRefreshOpacity = onSetRefreshOpacity
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

    // Add App to Quick Launch Dialog
    if (showAddQuickLaunchDialog) {
        SelectAppDialog(
            title = stringResource(R.string.add_quick_launch_title),
            availableApps = state.availableApps,
            existingApps = state.config.quickLaunchApps,
            onDismiss = { showAddQuickLaunchDialog = false },
            onSelectApp = { pkg ->
                onAddQuickLaunchApp(pkg)
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
            onSelectApp = { pkg ->
                onAddFullscreenApp(pkg)
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
    onRemoveQuickLaunchApp: (String) -> Unit,
    onShowAddDialog: () -> Unit
) {
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
        onSetOpacity = onSetQuickLaunchOpacity
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
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = { Text(pkg.substringAfterLast('.')) },
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
    onRemoveFullscreenApp: (String) -> Unit,
    onShowAddDialog: () -> Unit
) {
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
        onSetOpacity = onSetFullscreenOpacity
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
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = { Text(pkg.substringAfterLast('.')) },
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
    onSetHomeOpacity: (Int) -> Unit
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
        onSetOpacity = onSetHomeOpacity
    )
}

@Composable
private fun BackNavigatorCard(
    state: FloatingButtonsState,
    onToggleBackNavigator: (Boolean) -> Unit,
    onSetBackSize: (Int) -> Unit,
    onSetBackOpacity: (Int) -> Unit
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
        onSetOpacity = onSetBackOpacity
    )
}

@Composable
private fun RefreshButtonCard(
    state: FloatingButtonsState,
    onToggleRefreshNavigator: (Boolean) -> Unit,
    onSetRefreshSize: (Int) -> Unit,
    onSetRefreshOpacity: (Int) -> Unit
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
        onSetOpacity = onSetRefreshOpacity
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
        modifier = Modifier.fillMaxWidth(),
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
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                    }

                    AnimatedVisibility(visible = isSeparateButtons) {
                        Column {
                            if (content != null) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

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
    Card(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun SelectAppDialog(
    title: String,
    availableApps: List<AppItem>,
    existingApps: List<String>,
    onDismiss: () -> Unit,
    onSelectApp: (String) -> Unit
) {
    var customPackageInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customPackageInput,
                    onValueChange = { customPackageInput = it },
                    label = { Text(stringResource(R.string.package_name_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (customPackageInput.isNotBlank()) {
                    Button(
                        onClick = { onSelectApp(customPackageInput.trim()) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.btn_add_custom))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.select_from_installed), style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    val filteredList = availableApps.filter { !existingApps.contains(it.packageName) }
                    items(filteredList) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectApp(app.packageName) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Android, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
