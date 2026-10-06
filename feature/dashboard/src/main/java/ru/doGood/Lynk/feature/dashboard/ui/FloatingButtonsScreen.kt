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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.app.AppItem
import com.example.lynk.core.domain.floating.FloatingButtonConfig
import ru.doGood.Lynk.feature.dashboard.FloatingButtonsState

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
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onOpenUsageStatsSettings = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
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
            title = "Добавить в Быстрый Запуск",
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
            title = "Добавить приложение для разворота",
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
        title = "Кнопка быстрого запуска",
        subtitle = "Быстрое меню со списком избранных приложений",
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
                "Приложения меню (${state.config.quickLaunchApps.size}):",
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onShowAddDialog) {
                Icon(Icons.Rounded.Add, contentDescription = "Добавить приложение")
            }
        }

        if (state.config.quickLaunchApps.isEmpty()) {
            Text(
                text = "Приложения не добавлены. Нажмите '+', чтобы добавить.",
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
                                contentDescription = "Удалить",
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
        title = "Авто-оверлей (На весь экран)",
        subtitle = "Кнопка разворота на экран 1003 поверх выбранных ПО",
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
                "Целевые приложения (${state.config.fullscreenApps.size}):",
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onShowAddDialog) {
                Icon(Icons.Rounded.Add, contentDescription = "Добавить приложение")
            }
        }

        if (state.config.fullscreenApps.isEmpty()) {
            Text(
                text = "Приложения не добавлены. Нажмите '+', чтобы выбрать.",
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
                                contentDescription = "Удалить",
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
        title = "Навигатор \"Домой\"",
        subtitle = "Плавающая кнопка быстрого возврата на рабочий стол",
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
        title = "Навигатор \"Назад\"",
        subtitle = "Плавающая кнопка имитации системной клавиши Назад",
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
        title = "Кнопка \"Обновить\"",
        subtitle = "Плавающая кнопка принудительного обновления оверлеев",
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
                            text = if (state.isServiceRunning) "Служба оверлея запущена" else "Служба оверлея остановлена",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (state.isServiceRunning) "Плавающие кнопки отображаются поверх окон" else "Включите службу для активации кнопок",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onToggleService) {
                    Text(if (state.isServiceRunning) "Остановить" else "Запустить")
                }
            }

            if (!state.isOverlayPermissionGranted || !state.isUsageStatsPermissionGranted) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "Требуются разрешения системного уровня:",
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
                        Text(text = "• Отображение поверх других окон", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenOverlaySettings) {
                            Text("Разрешить")
                        }
                    }
                }

                if (!state.isUsageStatsPermissionGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "• Доступ к истории использования", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenUsageStatsSettings) {
                            Text("Разрешить")
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
                                text = "Размер кнопки: $sizeDp dp",
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
                                text = "Прозрачность кнопки: $opacityPercent%",
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
                    text = "Режим оверлея и дисплей",
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
                        text = "Разделять плавающие кнопки",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Управлять кнопками независимо либо единым блоком",
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
                        text = "Размер кнопок в совмещенном блоке: ${config.combinedButtonSize} dp",
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
                        text = "Непрозрачность совмещенного блока: ${config.opacityPercent}%",
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
                        text = "Зеркалирование на Display 1003",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Дублировать оверлей на экран пассажира ГУ",
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
                    label = { Text("Имя пакета (например, com.example.app)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (customPackageInput.isNotBlank()) {
                    Button(
                        onClick = { onSelectApp(customPackageInput.trim()) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.End)
                    ) {
                        Text("Добавить введенный")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Или выберите из установленных ПО:", style = MaterialTheme.typography.labelMedium)

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
                Text("Закрыть")
            }
        }
    )
}
