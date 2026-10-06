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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.update.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    currentVersion: String,
    updateInfo: UpdateInfo?,
    isChecking: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    statusMessage: String?,
    onCheckForUpdates: () -> Unit,
    onStartDownload: () -> Unit,
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

                updateInfo?.let { info ->
                    LatestVersionCard(
                        updateInfo = info,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        statusMessage = statusMessage,
                        onStartDownload = onStartDownload
                    )
                }
            }

            // Right Column: Changelog
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

            updateInfo?.let { info ->
                item {
                    LatestVersionCard(
                        updateInfo = info,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        statusMessage = statusMessage,
                        onStartDownload = onStartDownload
                    )
                }

                item {
                    ChangelogCard(updateInfo = info)
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
                        text = "Lynk Application",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Текущая версия: v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isChecking -> "Проверка обновлений..."
                        updateInfo == null -> "Статус версии не проверен"
                        updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE -> "Доступна новая версия v${updateInfo.latestVersion}!"
                        else -> "Установлена актуальная версия"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Button(
                    onClick = onCheckForUpdates,
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Проверка...")
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Проверить")
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
    downloadProgress: Float,
    statusMessage: String?,
    onStartDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Свежий релиз: v${updateInfo.latestVersion}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Доступно обновление",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (updateInfo.state == UpdateInfo.UpdateState.UPDATE_AVAILABLE) {
                    Button(
                        onClick = onStartDownload,
                        enabled = !isDownloading
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDownloading) "Скачивание..." else "Скачать и установить")
                    }
                }
            }

            AnimatedVisibility(visible = isDownloading || !statusMessage.isNullOrEmpty()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Прогресс: ${(downloadProgress * 100).toInt()}%",
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
                    text = "Журнал изменений (Changelog)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            val changelog = updateInfo?.changelog?.split("\n")?.filter { it.isNotBlank() } ?: listOf(
                "• Нажмите 'Проверить обновления', чтобы загрузить актуальный список изменений."
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
