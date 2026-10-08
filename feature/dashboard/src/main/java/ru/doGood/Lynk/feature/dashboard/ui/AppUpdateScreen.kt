package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.update.UpdateInfo
import ru.doGood.Lynk.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    currentVersion: String,
    updateInfo: UpdateInfo?,
    isChecking: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean = false,
    downloadProgress: Float,
    statusMessage: String?,
    onCheckForUpdates: () -> Unit,
    onStartDownload: () -> Unit,
    onInstallUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val showUpdateDetails = updateInfo != null &&
            (updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE ||
                    updateInfo.state == UpdateInfo.UpdateState.DOWNLOADING ||
                    updateInfo.state == UpdateInfo.UpdateState.DOWNLOADED ||
                    isDownloading || isDownloaded)

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Version Info & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrentVersionCard(
                    currentVersion = currentVersion,
                    updateInfo = updateInfo,
                    isChecking = isChecking,
                    onCheckForUpdates = onCheckForUpdates
                )

                if (showUpdateDetails && updateInfo != null) {
                    LatestVersionCard(
                        updateInfo = updateInfo,
                        isDownloading = isDownloading,
                        isDownloaded = isDownloaded,
                        downloadProgress = downloadProgress,
                        statusMessage = statusMessage,
                        onStartDownload = onStartDownload,
                        onInstallUpdate = onInstallUpdate
                    )
                }
            }

            // Right Column: Changelog
            if (showUpdateDetails) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ChangelogCard(
                        updateInfo = updateInfo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CurrentVersionCard(
                    currentVersion = currentVersion,
                    updateInfo = updateInfo,
                    isChecking = isChecking,
                    onCheckForUpdates = onCheckForUpdates
                )
            }

            if (showUpdateDetails && updateInfo != null) {
                item {
                    LatestVersionCard(
                        updateInfo = updateInfo,
                        isDownloading = isDownloading,
                        isDownloaded = isDownloaded,
                        downloadProgress = downloadProgress,
                        statusMessage = statusMessage,
                        onStartDownload = onStartDownload,
                        onInstallUpdate = onInstallUpdate
                    )
                }

                item {
                    ChangelogCard(updateInfo = updateInfo)
                }
            }
        }
    }
}

@Composable
private fun CurrentVersionCard(
    currentVersion: String,
    updateInfo: UpdateInfo?,
    isChecking: Boolean,
    onCheckForUpdates: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_full_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.current_version_format, currentVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when {
                        isChecking -> stringResource(R.string.checking_updates)
                        updateInfo == null -> stringResource(R.string.version_status_unchecked)
                        updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE -> stringResource(R.string.update_available_subtitle)
                        else -> stringResource(R.string.latest_version_installed_format, currentVersion)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Button(
                    onClick = onCheckForUpdates,
                    enabled = !isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_checking))
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_check))
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestVersionCard(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float,
    statusMessage: String?,
    onStartDownload: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE || isDownloaded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.latest_release_format, updateInfo.latestVersion.removePrefix("v")),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDownloaded || updateInfo.state == UpdateInfo.UpdateState.DOWNLOADED) {
                    Button(
                        onClick = onInstallUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Rounded.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_install_update))
                    }
                } else if (updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE) {
                    Button(
                        onClick = onStartDownload,
                        enabled = !isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDownloading) stringResource(R.string.btn_downloading) else stringResource(R.string.btn_download_install))
                    }
                }
            }

            AnimatedVisibility(visible = isDownloading || !statusMessage.isNullOrEmpty()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = downloadProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.download_progress_format, (downloadProgress * 100).toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogCard(
    updateInfo: UpdateInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.ListAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.changelog_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            val placeholderChangelog = stringResource(R.string.changelog_placeholder)
            val changelog = updateInfo?.changelog?.split("\n")?.filter { it.isNotBlank() } ?: listOf(
                placeholderChangelog
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(changelog) { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.removePrefix("•").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
