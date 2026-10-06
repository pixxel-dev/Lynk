package ru.doGood.Lynk.feature.dashboard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.lynk.core.domain.file.FileItem
import com.example.lynk.core.domain.installer.InstallStep
import ru.doGood.Lynk.feature.dashboard.InlineInstallState
import ru.doGood.Lynk.feature.dashboard.R
import ru.doGood.Lynk.feature.dashboard.SortType
import java.io.File
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
    inlineInstallState: InlineInstallState? = null,
    isLoading: Boolean = false,
    cloudConnections: List<com.example.lynk.core.domain.cloud.CloudConnection> = emptyList(),
    activeCloudConnection: com.example.lynk.core.domain.cloud.CloudConnection? = null,
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
    onDeleteFiles: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onInstallApk: (FileItem) -> Unit,
    onShowProperties: (FileItem) -> Unit,
    onRenameFile: (String, String) -> Unit = { _, _ -> },
    onStartInlineWaterfallInstall: (FileItem) -> Unit = {},
    onDismissInlineInstall: () -> Unit = {},
    onSaveCloudConnection: (name: String, url: String, username: String, passwordToken: String) -> Unit = { _, _, _, _ -> },
    onSelectCloudConnection: (com.example.lynk.core.domain.cloud.CloudConnection) -> Unit = {},
    onRemoveCloudConnection: (String) -> Unit = {},
    onDownloadCloudFile: (FileItem, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var showStorageMenu by remember { mutableStateOf(false) }
    var showCloudModal by remember { mutableStateOf(false) }
    var showAddCloudDialog by remember { mutableStateOf(false) }

    var selectedItemForMenu by remember { mutableStateOf<FileItem?>(null) }
    var filePropertiesToShow by remember { mutableStateOf<FileItem?>(null) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var filesPendingDelete by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val rootPath = Environment.getExternalStorageDirectory().absolutePath
    val isAtRoot = currentPath == rootPath

    val displayPath = if (currentPath.startsWith("webdav://")) {
        val cloudName = activeCloudConnection?.name ?: "WebDAV Cloud"
        val subPath = currentPath.substring(9)
        "☁️ $cloudName$subPath"
    } else if (currentPath == rootPath) {
        stringResource(R.string.internal_storage)
    } else if (currentPath.startsWith(rootPath)) {
        stringResource(R.string.internal_storage) + currentPath.removePrefix(rootPath)
    } else {
        currentPath
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                if (!isAtRoot) {
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
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = displayPath,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Sort Button & Dropdown
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
                            leadingIcon = { if (sortType == SortType.NAME) Icon(Icons.Rounded.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_size)) },
                            onClick = { onSortTypeChange(SortType.SIZE); showSortMenu = false },
                            leadingIcon = { if (sortType == SortType.SIZE) Icon(Icons.Rounded.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_date)) },
                            onClick = { onSortTypeChange(SortType.DATE); showSortMenu = false },
                            leadingIcon = { if (sortType == SortType.DATE) Icon(Icons.Rounded.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_type)) },
                            onClick = { onSortTypeChange(SortType.TYPE); showSortMenu = false },
                            leadingIcon = { if (sortType == SortType.TYPE) Icon(Icons.Rounded.Check, null) }
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
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
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
                // Storage Card with Dropdown Switcher
                StorageCard(
                    freeSpaceFormatted = freeSpaceFormatted,
                    totalSpaceFormatted = totalSpaceFormatted,
                    usedPercentage = usedPercentage,
                    showStorageMenu = showStorageMenu,
                    currentPath = currentPath,
                    activeCloudConnection = activeCloudConnection,
                    cloudConnections = cloudConnections,
                    onToggleStorageMenu = { showStorageMenu = !showStorageMenu },
                    onDismissStorageMenu = { showStorageMenu = false },
                    onSelectInternal = { onPathClick(Environment.getExternalStorageDirectory().absolutePath) },
                    onSelectRoot = { onPathClick("/") },
                    onSelectExternal = { onPathClick("/storage") },
                    onAddCloudClick = { showAddCloudDialog = true },
                    onSelectCloudConnection = { conn -> onSelectCloudConnection(conn) },
                    onRemoveCloudConnection = { id -> onRemoveCloudConnection(id) },
                    modifier = Modifier.weight(1f)
                )

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
            // Storage Stats Card with Dropdown Switcher
            StorageCard(
                freeSpaceFormatted = freeSpaceFormatted,
                totalSpaceFormatted = totalSpaceFormatted,
                usedPercentage = usedPercentage,
                showStorageMenu = showStorageMenu,
                currentPath = currentPath,
                activeCloudConnection = activeCloudConnection,
                cloudConnections = cloudConnections,
                onToggleStorageMenu = { showStorageMenu = !showStorageMenu },
                onDismissStorageMenu = { showStorageMenu = false },
                onSelectInternal = { onPathClick(Environment.getExternalStorageDirectory().absolutePath) },
                onSelectRoot = { onPathClick("/") },
                onSelectExternal = { onPathClick("/storage") },
                onAddCloudClick = { showAddCloudDialog = true },
                onSelectCloudConnection = { conn -> onSelectCloudConnection(conn) },
                onRemoveCloudConnection = { id -> onRemoveCloudConnection(id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

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
                        IconButton(onClick = {
                            val selectedItems = files.filter { selectedFiles.contains(it.path) }
                            if (selectedItems.isNotEmpty()) {
                                filesPendingDelete = selectedItems
                            }
                        }) {
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

        // File List Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.loading_files),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (files.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.directory_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(files, key = { it.path }) { item ->
                        FileItemRow(
                            item = item,
                            isSelected = selectedFiles.contains(item.path),
                            onClick = {
                                if (selectedFiles.isNotEmpty() && !item.isUpNavigation) {
                                    onFileLongClick(item)
                                } else if (item.isUpNavigation) {
                                    onNavigateUp()
                                } else if (item.isDirectory) {
                                    onPathClick(item.path)
                                } else if (item.isCloud || item.path.startsWith("webdav://")) {
                                    selectedItemForMenu = item
                                } else if (item.name.endsWith(".apk", ignoreCase = true)) {
                                    onStartInlineWaterfallInstall(item)
                                } else {
                                    openFileWithIntent(context, item)
                                }
                            },
                            onLongClick = {
                                if (!item.isUpNavigation) {
                                    onFileLongClick(item)
                                }
                            },
                            onMoreClick = { selectedItemForMenu = item }
                        )
                    }
                }
            }
        }

        // --- Dialogs ---

        if (showAddCloudDialog) {
            AddCloudConnectionDialog(
                onDismissRequest = { showAddCloudDialog = false },
                onConnect = { name, url, username, password ->
                    onSaveCloudConnection(name, url, username, password)
                    showAddCloudDialog = false
                }
            )
        }

        // Cloud Storage Info Modal
        if (showCloudModal) {
            AlertDialog(
                onDismissRequest = { showCloudModal = false },
                title = { Text(stringResource(R.string.cloud_dialog_title)) },
                text = { Text(stringResource(R.string.cloud_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = { showCloudModal = false }) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            )
        }

        // 1.4 Context Menu / Actions Dialog for an item
        selectedItemForMenu?.let { file ->
            val isCloud = file.isCloud || file.path.startsWith("webdav://")
            val isApk = file.name.endsWith(".apk", ignoreCase = true)

            AlertDialog(
                onDismissRequest = { selectedItemForMenu = null },
                title = null,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header card with file type icon and name
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCloud) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val fileIcon = when {
                                            file.isDirectory -> Icons.Rounded.Folder
                                            isApk -> Icons.Rounded.Android
                                            file.name.endsWith(".zip", ignoreCase = true) || file.name.endsWith(".rar", ignoreCase = true) -> Icons.Rounded.FolderZip
                                            file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true) -> Icons.Rounded.Image
                                            file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mkv", ignoreCase = true) -> Icons.Rounded.Movie
                                            file.name.endsWith(".mp3", ignoreCase = true) || file.name.endsWith(".wav", ignoreCase = true) -> Icons.Rounded.AudioFile
                                            else -> Icons.Rounded.InsertDriveFile
                                        }
                                        Icon(
                                            imageVector = fileIcon,
                                            contentDescription = null,
                                            tint = if (isCloud) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCloud) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (isCloud) "☁️ " + stringResource(R.string.cloud_file_badge) else "📁 " + stringResource(R.string.local_file_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (!file.isDirectory) {
                                            Text(
                                                text = formatFileSize(file.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        if (isCloud) {
                            // --- CLOUD FILE ACTIONS ---
                            if (isApk) {
                                // Highlighted Card 1: "Скачать и установить"
                                Card(
                                    onClick = {
                                        val target = selectedItemForMenu
                                        selectedItemForMenu = null
                                        if (target != null) {
                                            onDownloadCloudFile(target, true)
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.RocketLaunch,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.download_and_install),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = stringResource(R.string.download_and_install_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Highlighted Card 2: "Скачать на устройство"
                            Card(
                                onClick = {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    if (target != null) {
                                        onDownloadCloudFile(target, false)
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isApk) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isApk) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.download_to_device),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isApk) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = stringResource(R.string.download_to_device_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = (if (isApk) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // Properties
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.file_properties)) },
                                leadingContent = { Icon(Icons.Rounded.Info, null) },
                                modifier = Modifier.combinedClickable {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    filePropertiesToShow = target
                                }
                            )
                        } else {
                            // --- LOCAL FILE ACTIONS ---
                            if (isApk) {
                                Card(
                                    onClick = {
                                        val target = selectedItemForMenu
                                        selectedItemForMenu = null
                                        if (target != null) {
                                            onStartInlineWaterfallInstall(target)
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Android,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.install_apk),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            ListItem(
                                headlineContent = { Text(stringResource(R.string.file_properties)) },
                                leadingContent = { Icon(Icons.Rounded.Info, null) },
                                modifier = Modifier.combinedClickable {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    filePropertiesToShow = target
                                }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.rename_file)) },
                                leadingContent = { Icon(Icons.Rounded.Edit, null) },
                                modifier = Modifier.combinedClickable {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    fileToRename = target
                                }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.btn_copy)) },
                                leadingContent = { Icon(Icons.Rounded.ContentCopy, null) },
                                modifier = Modifier.combinedClickable {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    if (target != null) {
                                        onFileLongClick(target)
                                        onCopySelected()
                                    }
                                }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.btn_cut)) },
                                leadingContent = { Icon(Icons.Rounded.ContentCut, null) },
                                modifier = Modifier.combinedClickable {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    if (target != null) {
                                        onFileLongClick(target)
                                        onCutSelected()
                                    }
                                }
                            )
                            if (clipboardCount > 0) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.file_paste)) },
                                    leadingContent = { Icon(Icons.Rounded.ContentPaste, null) },
                                    modifier = Modifier.combinedClickable {
                                        selectedItemForMenu = null
                                        onPaste()
                                    }
                                )
                            }
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.btn_delete)) },
                                leadingContent = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.combinedClickable {
                                    val target = selectedItemForMenu
                                    selectedItemForMenu = null
                                    fileToDelete = target
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedItemForMenu = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // Rename Dialog
        fileToRename?.let { file ->
            var newName by remember { mutableStateOf(file.name) }
            AlertDialog(
                onDismissRequest = { fileToRename = null },
                title = { Text(stringResource(R.string.rename_file_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.enter_new_name), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val path = file.path
                            fileToRename = null
                            if (newName.isNotBlank() && newName != file.name) {
                                onRenameFile(path, newName)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.btn_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToRename = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // File Detailed Properties Dialog
        filePropertiesToShow?.let { fileItem ->
            val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
            val isCloud = fileItem.isCloud || fileItem.path.startsWith("webdav://")

            AlertDialog(
                onDismissRequest = { filePropertiesToShow = null },
                title = { Text(stringResource(R.string.file_properties_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.file_prop_name, fileItem.name), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                        if (isCloud) {
                            val directUrl = fileItem.downloadUrl?.ifBlank { null } ?: fileItem.path
                            Text(
                                text = stringResource(R.string.file_direct_link, directUrl),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(stringResource(R.string.file_prop_path, fileItem.path), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = stringResource(R.string.file_prop_size, if (fileItem.isDirectory) stringResource(R.string.file_type_folder) else formatFileSize(fileItem.size)),
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (fileItem.lastModified > 0) {
                            Text(stringResource(R.string.file_prop_date, dateFormat.format(Date(fileItem.lastModified))), style = MaterialTheme.typography.bodySmall)
                        }

                        if (isCloud) {
                            Text(stringResource(R.string.file_permissions) + ": " + stringResource(R.string.cloud_storage_location), style = MaterialTheme.typography.bodySmall)
                        } else {
                            val javaFile = remember(fileItem.path) { File(fileItem.path) }
                            val readable = if (javaFile.canRead()) "r" else "-"
                            val writable = if (javaFile.canWrite()) "w" else "-"
                            val executable = if (javaFile.canExecute()) "x" else "-"
                            val permsString = "$readable$writable$executable"
                            Text(stringResource(R.string.file_permissions) + ": $permsString", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { filePropertiesToShow = null }) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            )
        }

        // Delete Confirmation Dialog for single file
        fileToDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text(stringResource(R.string.delete_file_title)) },
                text = { Text(stringResource(R.string.delete_file_message, file.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val path = file.path
                            fileToDelete = null
                            onDeleteFiles(listOf(path))
                        }
                    ) {
                        Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // Delete Confirmation Dialog for multiple files
        if (filesPendingDelete.isNotEmpty()) {
            val titleText = stringResource(R.string.delete_file_title)
            val messageText = stringResource(R.string.delete_file_message, "${filesPendingDelete.size} items")

            AlertDialog(
                onDismissRequest = { filesPendingDelete = emptyList() },
                title = { Text(titleText) },
                text = { Text(messageText) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pathsToDelete = filesPendingDelete.map { it.path }
                            filesPendingDelete = emptyList()
                            onDeleteFiles(pathsToDelete)
                        }
                    ) {
                        Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { filesPendingDelete = emptyList() }
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // 1.5 Inline Waterfall Installation Dialog
        inlineInstallState?.let { inlineState ->
            AlertDialog(
                onDismissRequest = onDismissInlineInstall,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.waterfall_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = inlineState.apkFile.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        InstallStep.entries.forEach { step ->
                            val statusPair = inlineState.stepStatuses[step]
                            val isSuccess = statusPair?.first
                            val statusMsg = statusPair?.second ?: stringResource(R.string.ready)
                            val isActive = inlineState.activeStep == step

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (isSuccess) {
                                    true -> Text("✅", modifier = Modifier.size(20.dp))
                                    false -> Text("❌", modifier = Modifier.size(20.dp))
                                    null -> {
                                        if (isActive && inlineState.isInstalling) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text("⏳", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismissInlineInstall) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            )
        }
    }
}

@Composable
private fun StorageCard(
    freeSpaceFormatted: String,
    totalSpaceFormatted: String,
    usedPercentage: Float,
    showStorageMenu: Boolean,
    currentPath: String,
    activeCloudConnection: com.example.lynk.core.domain.cloud.CloudConnection?,
    cloudConnections: List<com.example.lynk.core.domain.cloud.CloudConnection>,
    onToggleStorageMenu: () -> Unit,
    onDismissStorageMenu: () -> Unit,
    onSelectInternal: () -> Unit,
    onSelectRoot: () -> Unit,
    onSelectExternal: () -> Unit,
    onAddCloudClick: () -> Unit,
    onSelectCloudConnection: (com.example.lynk.core.domain.cloud.CloudConnection) -> Unit,
    onRemoveCloudConnection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCloudActive = currentPath.startsWith("webdav://") || activeCloudConnection != null

    Card(
        modifier = modifier,
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
                Box {
                    TextButton(
                        onClick = onToggleStorageMenu,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (isCloudActive) Icons.Rounded.Cloud else Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCloudActive) {
                                activeCloudConnection?.name ?: "WebDAV Cloud"
                            } else {
                                stringResource(R.string.storage_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 1.1 Storage Switcher Dropdown
                    DropdownMenu(
                        expanded = showStorageMenu,
                        onDismissRequest = onDismissStorageMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.storage_internal)) },
                            onClick = { onSelectInternal(); onDismissStorageMenu() },
                            leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.storage_root)) },
                            onClick = { onSelectRoot(); onDismissStorageMenu() },
                            leadingIcon = { Icon(Icons.Rounded.FolderSpecial, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.storage_external)) },
                            onClick = { onSelectExternal(); onDismissStorageMenu() },
                            leadingIcon = { Icon(Icons.Rounded.Usb, null) }
                        )
                        
                        if (cloudConnections.isNotEmpty()) {
                            HorizontalDivider()
                            cloudConnections.forEach { cloudConn ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "☁️ ${cloudConn.name}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { onRemoveCloudConnection(cloudConn.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = stringResource(R.string.delete_cloud),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onSelectCloudConnection(cloudConn); onDismissStorageMenu() }
                                )
                            }
                        }

                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.storage_add_cloud)) },
                            onClick = { onAddCloudClick(); onDismissStorageMenu() },
                            leadingIcon = { Icon(Icons.Rounded.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }

                if (isCloudActive) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "WebDAV",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = activeCloudConnection?.webDavUrl ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.storage_free, freeSpaceFormatted),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.storage_total, totalSpaceFormatted),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            if (!isCloudActive) {
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
    }
}

@Composable
fun AddCloudConnectionDialog(
    onDismissRequest: () -> Unit,
    onConnect: (name: String, url: String, username: String, passwordToken: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var passwordToken by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.add_cloud_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.cloud_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.cloud_url_label)) },
                    placeholder = { Text("https://webdav.yandex.ru/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.cloud_anonymous_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it }
                    )
                }
                if (!isAnonymous) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.cloud_username_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordToken,
                        onValueChange = { passwordToken = it },
                        label = { Text(stringResource(R.string.cloud_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.isBlank()) "WebDAV Cloud" else name.trim()
                    var finalUrl = url.trim()
                    if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                        finalUrl = "https://$finalUrl"
                    }
                    val finalUsername = if (isAnonymous) "" else username.trim()
                    val finalPassword = if (isAnonymous) "" else passwordToken.trim()
                    onConnect(finalName, finalUrl, finalUsername, finalPassword)
                },
                enabled = url.isNotBlank()
            ) {
                Text(stringResource(R.string.cloud_btn_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
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
            // Checkbox indicator if selected
            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
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
            }

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

fun openFileWithIntent(context: Context, fileItem: FileItem) {
    try {
        val file = File(fileItem.path)
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val extension = MimeTypeMap.getFileExtensionFromUrl(fileItem.path)
        val mimeType = if (!extension.isNullOrEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
        } else {
            context.contentResolver.getType(uri)
        } ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.open_with)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
