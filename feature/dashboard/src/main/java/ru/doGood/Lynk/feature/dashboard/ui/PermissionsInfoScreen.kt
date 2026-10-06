package ru.doGood.Lynk.feature.dashboard.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.lazy.grid.GridItemSpan
import ru.doGood.Lynk.feature.dashboard.DashboardViewModel
import ru.doGood.Lynk.feature.dashboard.R

data class PermissionItemInfo(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val openSettingsIntent: () -> Intent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsInfoTab(
    viewModel: DashboardViewModel? = null,
    onRefreshPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val lifecycleOwner = LocalLifecycleOwner.current

    val overlayTitle = stringResource(R.string.perm_overlay_title)
    val overlayDesc = stringResource(R.string.perm_overlay_desc_long)
    val usageTitle = stringResource(R.string.perm_usage_title)
    val usageDesc = stringResource(R.string.perm_usage_desc_long)
    val storageTitle = stringResource(R.string.perm_storage_title)
    val storageDesc = stringResource(R.string.perm_storage_desc_long)
    val installTitle = stringResource(R.string.perm_install_title)
    val installDesc = stringResource(R.string.perm_install_desc_long)

    var permissionsList by remember { mutableStateOf(emptyList<PermissionItemInfo>()) }

    fun checkAllPermissions() {
        val isOverlayGranted = Settings.canDrawOverlays(context)

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        val isUsageStatsGranted = (mode == AppOpsManager.MODE_ALLOWED)

        val isStorageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val isInstallGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        permissionsList = listOf(
            PermissionItemInfo(
                id = "overlay",
                title = overlayTitle,
                description = overlayDesc,
                icon = Icons.Rounded.Layers,
                isGranted = isOverlayGranted,
                openSettingsIntent = {
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                }
            ),
            PermissionItemInfo(
                id = "usage_stats",
                title = usageTitle,
                description = usageDesc,
                icon = Icons.Rounded.QueryStats,
                isGranted = isUsageStatsGranted,
                openSettingsIntent = {
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }
            ),
            PermissionItemInfo(
                id = "storage",
                title = storageTitle,
                description = storageDesc,
                icon = Icons.Rounded.FolderSpecial,
                isGranted = isStorageGranted,
                openSettingsIntent = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    }
                }
            ),
            PermissionItemInfo(
                id = "install_packages",
                title = installTitle,
                description = installDesc,
                icon = Icons.Rounded.InstallMobile,
                isGranted = isInstallGranted,
                openSettingsIntent = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    }
                }
            )
        )
    }

    LaunchedEffect(Unit) {
        checkAllPermissions()
        viewModel?.refreshPermissions(context)
        onRefreshPermissions()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Автоматический перерасчет и проверка прав доступа
                checkAllPermissions()
                viewModel?.refreshPermissions(context)
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isLandscape) 2 else 1),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(permissionsList, key = { it.id }) { permission ->
                PermissionCard(
                    permission = permission,
                    onOpenSettings = {
                        try {
                            val intent = permission.openSettingsIntent().apply {
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                AdbCheatSheetCard(packageName = context.packageName)
            }
        }
    }
}

@Composable
fun PermissionsInfoScreen(
    viewModel: DashboardViewModel? = null,
    onRefreshPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PermissionsInfoTab(
        viewModel = viewModel,
        onRefreshPermissions = onRefreshPermissions,
        modifier = modifier
    )
}

@Composable
fun PermissionCard(
    permission: PermissionItemInfo,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
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
                            imageVector = permission.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = permission.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = if (permission.isGranted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (permission.isGranted) {
                                stringResource(R.string.granted)
                            } else {
                                stringResource(R.string.not_granted)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (permission.isGranted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = if (permission.isGranted) {
                    ButtonDefaults.outlinedButtonColors()
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    imageVector = if (permission.isGranted) Icons.Rounded.Check else Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (permission.isGranted) {
                        stringResource(R.string.permission_settings)
                    } else {
                        stringResource(R.string.grant_access)
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun AdbCheatSheetCard(packageName: String) {
    val clipboardManager = LocalClipboardManager.current
    var copiedMessageVisible by remember { mutableStateOf(false) }

    LaunchedEffect(copiedMessageVisible) {
        if (copiedMessageVisible) {
            kotlinx.coroutines.delay(2000)
            copiedMessageVisible = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.adb_cheat_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.adb_cheat_sheet_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (copiedMessageVisible) {
                Text(
                    text = stringResource(R.string.adb_copied_to_clipboard),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AdbCommandItem(
                title = stringResource(R.string.adb_cmd_overlay_title),
                command = "adb shell appops set $packageName SYSTEM_ALERT_WINDOW allow",
                onCopy = { 
                    clipboardManager.setText(AnnotatedString(it))
                    copiedMessageVisible = true 
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdbCommandItem(
                title = stringResource(R.string.adb_cmd_usage_stats_title),
                command = "adb shell pm grant $packageName android.permission.PACKAGE_USAGE_STATS",
                onCopy = { 
                    clipboardManager.setText(AnnotatedString(it))
                    copiedMessageVisible = true 
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdbCommandItem(
                title = stringResource(R.string.adb_cmd_install_title),
                command = "adb shell appops set $packageName REQUEST_INSTALL_PACKAGES allow",
                onCopy = { 
                    clipboardManager.setText(AnnotatedString(it))
                    copiedMessageVisible = true 
                }
            )
        }
    }
}

@Composable
private fun AdbCommandItem(
    title: String,
    command: String,
    onCopy: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = androidx.compose.ui.graphics.Color(0xFF2B2B2B),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = androidx.compose.ui.graphics.Color(0xFFA9B7C6),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onCopy(command) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.adb_copy),
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
