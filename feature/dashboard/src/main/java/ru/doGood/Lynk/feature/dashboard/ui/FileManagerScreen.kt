package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.file.FileItem
import ru.doGood.Lynk.feature.dashboard.R
import ru.doGood.Lynk.feature.dashboard.SortType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    currentPath: String,
    files: List<FileItem>,
    searchQuery: String,
    sortType: SortType,
    sortAscending: Boolean,
    selectedFiles: Set<String>,
    clipboardCount: Int,
    isCutOperation: Boolean,
    freeSpaceFormatted: String,
    totalSpaceFormatted: String,
    usedPercentage: Float,
    onPathClick: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onToggleSortDirection: () -> Unit,
    onCopySelected: () -> Unit,
    onCutSelected: () -> Unit,
    onPaste: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit,
    onInstallApk: (FileItem) -> Unit,
    onShowProperties: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedItemForMenu by remember { mutableStateOf<FileItem?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Compact Directory Navigation Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.file_up_dir)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.file_sort_files))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_name)) },
                            onClick = { onSortTypeChange(SortType.NAME); showSortMenu = false },
                            leadingIcon = {
                                if (sortType == SortType.NAME) Icon(Icons.Rounded.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_size)) },
                            onClick = { onSortTypeChange(SortType.SIZE); showSortMenu = false },
                            leadingIcon = {
                                if (sortType == SortType.SIZE) Icon(Icons.Rounded.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_date)) },
                            onClick = { onSortTypeChange(SortType.DATE); showSortMenu = false },
                            leadingIcon = {
                                if (sortType == SortType.DATE) Icon(Icons.Rounded.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_type)) },
                            onClick = { onSortTypeChange(SortType.TYPE); showSortMenu = false },
                            leadingIcon = {
                                if (sortType == SortType.TYPE) Icon(Icons.Rounded.Check, null)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (sortAscending) stringResource(R.string.sort_ascending) else stringResource(R.string.sort_descending)) },
                            onClick = { onToggleSortDirection(); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.SwapVert, null) }
                        )
                    }
                }

                if (clipboardCount > 0) {
                    IconButton(
                        onClick = onPaste,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("$clipboardCount")
                        }
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.file_paste))
                    }
                }
            }
        }
            // Storage & Search Controls
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Storage Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.storage_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.storage_free_total, freeSpaceFormatted, totalSpaceFormatted),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { usedPercentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = if (usedPercentage > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_files_hint)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            } else {
                // Storage Stats Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.storage_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = stringResource(R.string.storage_free_total, freeSpaceFormatted, totalSpaceFormatted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { usedPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (usedPercentage > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.search_files_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Selection Action Bar
            AnimatedVisibility(visible = selectedFiles.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.selected_count, selectedFiles.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row {
                            IconButton(onClick = onCopySelected) {
                                Icon(Icons.Rounded.ContentCopy, stringResource(R.string.btn_copy))
                            }
                            IconButton(onClick = onCutSelected) {
                                Icon(Icons.Rounded.ContentCut, stringResource(R.string.btn_cut))
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    stringResource(R.string.btn_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.btn_deselect))
                            }
                        }
                    }
                }
            }

            // File List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    FileItemRow(
                        item = item,
                        isSelected = selectedFiles.contains(item.path),
                        onClick = { onFileClick(item) },
                        onLongClick = { onFileLongClick(item) },
                        onMoreClick = { selectedItemForMenu = item }
                    )
                }
            }

            // Context Menu / Options Dialog for an item
            selectedItemForMenu?.let { file ->
                val isApk = file.name.endsWith(".apk", ignoreCase = true)
                AlertDialog(
                    onDismissRequest = { selectedItemForMenu = null },
                    title = {
                        Text(
                            text = file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    text = {
                        Column {
                            if (isApk) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.install_apk)) },
                                    leadingContent = { Icon(Icons.Rounded.Android, null) },
                                    modifier = Modifier.combinedClickable {
                                        selectedItemForMenu = null
                                        onInstallApk(file)
                                    }
                                )
                            }
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.file_properties)) },
                                leadingContent = { Icon(Icons.Rounded.Info, null) },
                                modifier = Modifier.combinedClickable {
                                    selectedItemForMenu = null
                                    onShowProperties(file)
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedItemForMenu = null }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val icon = when {
                item.isUpNavigation -> Icons.AutoMirrored.Rounded.ArrowBack
                item.isDirectory -> Icons.Rounded.Folder
                item.name.endsWith(".apk", ignoreCase = true) -> Icons.Rounded.Android
                item.name.endsWith(".zip", ignoreCase = true) || item.name.endsWith(".rar", ignoreCase = true) -> Icons.Rounded.FolderZip
                item.name.endsWith(".jpg", ignoreCase = true) || item.name.endsWith(".png", ignoreCase = true) -> Icons.Rounded.Image
                item.name.endsWith(".mp4", ignoreCase = true) || item.name.endsWith(".mkv", ignoreCase = true) -> Icons.Rounded.Movie
                item.name.endsWith(".mp3", ignoreCase = true) || item.name.endsWith(".wav", ignoreCase = true) -> Icons.Rounded.AudioFile
                else -> Icons.Rounded.InsertDriveFile
            }

            val iconTint = when {
                item.isDirectory || item.isUpNavigation -> MaterialTheme.colorScheme.primary
                item.name.endsWith(".apk", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.secondary
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.isUpNavigation) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (item.isDirectory) stringResource(R.string.file_folder) else formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.lastModified > 0) {
                            Text(
                                text = dateFormat.format(Date(item.lastModified)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!item.isUpNavigation) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
