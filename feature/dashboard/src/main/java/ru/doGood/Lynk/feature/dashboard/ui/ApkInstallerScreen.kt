package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.app.AppItem
import com.example.lynk.core.domain.installer.InstallResult
import com.example.lynk.core.domain.installer.InstallStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInstallerScreen(
    selectedApkPath: String?,
    isInstalling: Boolean,
    installResult: InstallResult?,
    activeStep: InstallStep?,
    stepStatuses: Map<InstallStep, String>,
    installedApps: List<AppItem>,
    onSelectApkClick: () -> Unit,
    onStartWaterfallInstall: () -> Unit,
    onInstallByStep: (InstallStep) -> Unit,
    onToggleAppChecked: (AppItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Target APK Card & Diagnostic Steps
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                item {
                    TargetApkCard(
                        selectedApkPath = selectedApkPath,
                        isInstalling = isInstalling,
                        onSelectApkClick = onSelectApkClick,
                        onStartWaterfallInstall = onStartWaterfallInstall
                    )
                }

                item {
                    Text(
                        text = "Installation Diagnostic Steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )
                }

                items(InstallStep.values()) { step ->
                    val statusText = stepStatuses[step] ?: "Ready"
                    val isActive = activeStep == step

                    InstallStepCard(
                        step = step,
                        statusText = statusText,
                        isActive = isActive,
                        onExecuteStep = { onInstallByStep(step) },
                        enabled = selectedApkPath != null && !isInstalling
                    )
                }

                installResult?.let { result ->
                    item {
                        InstallResultCard(result = result)
                    }
                }
            }

            // Right Column: Installed Applications
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                item {
                    Text(
                        text = "Installed System & User Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                items(installedApps, key = { it.packageName }) { app ->
                    AppItemRow(
                        app = app,
                        onCheckedChange = { checked -> onToggleAppChecked(app, checked) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item {
                TargetApkCard(
                    selectedApkPath = selectedApkPath,
                    isInstalling = isInstalling,
                    onSelectApkClick = onSelectApkClick,
                    onStartWaterfallInstall = onStartWaterfallInstall
                )
            }

            item {
                Text(
                    text = "Installation Strategies Diagnostic",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(InstallStep.values()) { step ->
                val statusText = stepStatuses[step] ?: "Ready"
                val isActive = activeStep == step

                InstallStepCard(
                    step = step,
                    statusText = statusText,
                    isActive = isActive,
                    onExecuteStep = { onInstallByStep(step) },
                    enabled = selectedApkPath != null && !isInstalling
                )
            }

            installResult?.let { result ->
                item {
                    InstallResultCard(result = result)
                }
            }

            item {
                Text(
                    text = "Installed System & User Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(installedApps, key = { it.packageName }) { app ->
                AppItemRow(
                    app = app,
                    onCheckedChange = { checked -> onToggleAppChecked(app, checked) }
                )
            }
        }
    }
}

@Composable
private fun TargetApkCard(
    selectedApkPath: String?,
    isInstalling: Boolean,
    onSelectApkClick: () -> Unit,
    onStartWaterfallInstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Target APK File",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = selectedApkPath ?: "No APK selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectApkClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Browse APK",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = onStartWaterfallInstall,
                    enabled = selectedApkPath != null && !isInstalling,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Installing...",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    } else {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Waterfall Install",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallResultCard(result: InstallResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSuccess) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (result.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = null,
                tint = if (result.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (result.isSuccess) "Installation Succeeded" else "Installation Failed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Step: ${result.step} — ${result.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun InstallStepCard(
    step: InstallStep,
    statusText: String,
    isActive: Boolean,
    onExecuteStep: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onExecuteStep,
                enabled = enabled
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Run step")
            }
        }
    }
}

@Composable
fun AppItemRow(
    app: AppItem,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Checkbox(
                checked = app.isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
