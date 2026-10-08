package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.system.DeviceInfo
import ru.doGood.Lynk.feature.dashboard.AppLanguage
import ru.doGood.Lynk.feature.dashboard.DashboardViewModel
import ru.doGood.Lynk.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfoScreen(
    deviceInfo: DeviceInfo,
    isRecordingLogs: Boolean,
    logsText: String,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearLogs: () -> Unit,
    viewModel: DashboardViewModel? = null,
    onRefreshPermissions: () -> Unit = {},
    updateState: ru.doGood.Lynk.feature.dashboard.AppUpdateUiState? = null,
    onCheckForUpdates: () -> Unit = {},
    onStartDownload: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: Specs, 1: System Logs, 2: Rights, 3: Update

    val dashboardState by viewModel?.state?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentLanguage = dashboardState?.appLanguage ?: AppLanguage.RU

    LaunchedEffect(activeTab) {
        if (activeTab == 2) {
            viewModel?.refreshPermissions()
            onRefreshPermissions()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(stringResource(R.string.subtab_specs), maxLines = 1) },
                icon = { Icon(Icons.Rounded.PhoneAndroid, null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(stringResource(R.string.subtab_logs), maxLines = 1) },
                icon = { Icon(Icons.Rounded.Terminal, null) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(stringResource(R.string.subtab_rights), maxLines = 1) },
                icon = { Icon(Icons.Rounded.Security, null) }
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text(stringResource(R.string.tab_update), maxLines = 1) },
                icon = { Icon(Icons.Rounded.SystemUpdate, null) }
            )
        }

        when (activeTab) {
            0 -> DeviceInfoTab(
                deviceInfo = deviceInfo,
                currentLanguage = currentLanguage,
                onLanguageSelected = { lang -> viewModel?.setAppLanguage(lang) },
                viewModel = viewModel
            )
            1 -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                SystemLogsTab(
                    isRecording = isRecordingLogs,
                    logsText = logsText,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onClearLogs = onClearLogs,
                    onCopyLogs = { clipboardManager.setText(AnnotatedString(logsText)) },
                    onSaveLogs = { viewModel?.saveLogsToFile(context) }
                )
            }
            2 -> PermissionsInfoTab(
                viewModel = viewModel,
                onRefreshPermissions = onRefreshPermissions
            )
            3 -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val packageInfo = try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: Exception) {
                    null
                }
                val versionName = packageInfo?.versionName ?: "1.0.0"

                if (updateState != null) {
                    AppUpdateScreen(
                        currentVersion = versionName,
                        updateInfo = updateState.updateInfo,
                        isChecking = updateState.isChecking,
                        isDownloading = updateState.isDownloading,
                        isDownloaded = updateState.isDownloaded,
                        downloadProgress = updateState.downloadProgress,
                        statusMessage = updateState.statusMessage,
                        onCheckForUpdates = onCheckForUpdates,
                        onStartDownload = onStartDownload,
                        onInstallUpdate = onInstallUpdate,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfoTab(
    deviceInfo: DeviceInfo,
    currentLanguage: AppLanguage = AppLanguage.RU,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    viewModel: DashboardViewModel? = null
) {
    val dashboardState by viewModel?.state?.collectAsState() ?: remember { mutableStateOf(null) }
    val sysState = dashboardState?.systemInfoState

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LanguageSelectionCard(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected
                    )
                }

                item {
                    WirelessAdbCard(
                        ipAddress = sysState?.adbIpAddress ?: "127.0.0.1",
                        port = sysState?.adbPort ?: "5555",
                        isConnected = sysState?.isAdbConnected == true,
                        isLoading = sysState?.isAdbLoading == true,
                        statusMessage = sysState?.adbStatusMessage,
                        onIpChange = { viewModel?.updateAdbIpAddress(it) },
                        onPortChange = { viewModel?.updateAdbPort(it) },
                        onConnect = { viewModel?.connectAdb() },
                        onDisconnect = { viewModel?.disconnectAdb() },
                        onRefreshIp = { viewModel?.refreshAdbIp() }
                    )
                }

                item {
                    InfoSectionCard(
                        title = stringResource(R.string.hardware_os_title),
                        icon = Icons.Rounded.DeveloperBoard
                    ) {
                        InfoRow(stringResource(R.string.manufacturer), deviceInfo.manufacturer)
                        InfoRow(stringResource(R.string.brand), deviceInfo.brand)
                        InfoRow(stringResource(R.string.model), deviceInfo.model)
                        InfoRow(stringResource(R.string.device_code), deviceInfo.device)
                        InfoRow(stringResource(R.string.android_version), deviceInfo.androidRelease)
                        InfoRow(stringResource(R.string.sdk_level), deviceInfo.androidSdk.toString())
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                InfoSectionCard(
                    title = stringResource(R.string.car_props_title),
                    icon = Icons.Rounded.DirectionsCar
                ) {
                    val carProps = deviceInfo.carSystemProperties.filter { !it.key.contains("ip", ignoreCase = true) }
                    if (carProps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_car_props),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        carProps.forEach { (key, value) ->
                            InfoRow(key, value)
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item {
                LanguageSelectionCard(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }

            item {
                WirelessAdbCard(
                    ipAddress = sysState?.adbIpAddress ?: "127.0.0.1",
                    port = sysState?.adbPort ?: "5555",
                    isConnected = sysState?.isAdbConnected == true,
                    isLoading = sysState?.isAdbLoading == true,
                    statusMessage = sysState?.adbStatusMessage,
                    onIpChange = { viewModel?.updateAdbIpAddress(it) },
                    onPortChange = { viewModel?.updateAdbPort(it) },
                    onConnect = { viewModel?.connectAdb() },
                    onDisconnect = { viewModel?.disconnectAdb() },
                    onRefreshIp = { viewModel?.refreshAdbIp() }
                )
            }

            item {
                InfoSectionCard(
                    title = stringResource(R.string.hardware_os_title),
                    icon = Icons.Rounded.DeveloperBoard
                ) {
                    InfoRow(stringResource(R.string.manufacturer), deviceInfo.manufacturer)
                    InfoRow(stringResource(R.string.brand), deviceInfo.brand)
                    InfoRow(stringResource(R.string.model), deviceInfo.model)
                    InfoRow(stringResource(R.string.device_code), deviceInfo.device)
                    InfoRow(stringResource(R.string.android_version), deviceInfo.androidRelease)
                    InfoRow(stringResource(R.string.sdk_level), deviceInfo.androidSdk.toString())
                }
            }

            item {
                InfoSectionCard(
                    title = stringResource(R.string.car_props_title),
                    icon = Icons.Rounded.DirectionsCar
                ) {
                    val carProps = deviceInfo.carSystemProperties.filter { !it.key.contains("ip", ignoreCase = true) }
                    if (carProps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_car_props),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        carProps.forEach { (key, value) ->
                            InfoRow(key, value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionCard(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.lang_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.lang_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentLanguage == AppLanguage.RU,
                    onClick = { onLanguageSelected(AppLanguage.RU) },
                    label = {
                        Text(
                            text = stringResource(R.string.lang_ru),
                            fontWeight = if (currentLanguage == AppLanguage.RU) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (currentLanguage == AppLanguage.RU) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = currentLanguage == AppLanguage.EN,
                    onClick = { onLanguageSelected(AppLanguage.EN) },
                    label = {
                        Text(
                            text = stringResource(R.string.lang_en),
                            fontWeight = if (currentLanguage == AppLanguage.EN) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (currentLanguage == AppLanguage.EN) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SystemLogsTab(
    isRecording: Boolean,
    logsText: String,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearLogs: () -> Unit,
    onCopyLogs: () -> Unit,
    onSaveLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Controls Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.recording_logs),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.logcat_idle),
                            softWrap = true
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isRecording) {
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_stop),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartRecording,
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_start),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onSaveLogs) {
                            Icon(Icons.Rounded.Save, stringResource(R.string.btn_save))
                        }
                        IconButton(onClick = onCopyLogs) {
                            Icon(Icons.Rounded.ContentCopy, stringResource(R.string.btn_copy_logs))
                        }
                        IconButton(onClick = onClearLogs) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.btn_clear_logs))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log Output Display
        Card(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            SelectionContainer(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    item {
                        Text(
                            text = if (logsText.isEmpty()) stringResource(R.string.no_logs) else logsText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WirelessAdbCard(
    ipAddress: String,
    port: String,
    isConnected: Boolean,
    isLoading: Boolean,
    statusMessage: String?,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshIp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.wireless_adb_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.wireless_adb_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.wireless_adb_status),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) 
                                stringResource(R.string.wireless_adb_status_connected) 
                            else 
                                stringResource(R.string.wireless_adb_status_disconnected),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Fields Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = onIpChange,
                    label = { Text(stringResource(R.string.wireless_adb_ip_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(0.65f),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = onRefreshIp) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh IP"
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = { Text(stringResource(R.string.wireless_adb_port_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.35f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnect,
                    enabled = !isLoading && ipAddress.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.wireless_adb_connect))
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.wireless_adb_disconnect))
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            var showInstructions by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showInstructions = !showInstructions },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showInstructions) stringResource(R.string.wireless_adb_hide_instructions) else stringResource(R.string.wireless_adb_show_instructions))
            }
            if (showInstructions) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wireless_adb_instructions_text),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
