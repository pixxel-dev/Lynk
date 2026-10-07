package ru.doGood.Lynk.feature.dashboard

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lynk.core.domain.agent.OverlayProfile
import com.example.lynk.core.domain.app.AppItem
import com.example.lynk.core.domain.file.FileItem
import com.example.lynk.core.domain.floating.FloatingButtonAction
import com.example.lynk.core.domain.floating.FloatingButtonConfig
import com.example.lynk.core.domain.installer.ApkInstaller
import com.example.lynk.core.domain.installer.InstallResult
import com.example.lynk.core.domain.installer.InstallStep
import com.example.lynk.core.domain.installer.WaterfallInstallStrategy
import com.example.lynk.core.domain.system.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.*

enum class SortType { NAME, SIZE, DATE, TYPE }

data class InlineInstallState(
    val apkFile: FileItem,
    val isInstalling: Boolean = false,
    val activeStep: InstallStep? = null,
    val stepStatuses: Map<InstallStep, Pair<Boolean?, String>> = emptyMap(),
    val installResult: InstallResult? = null
)

data class FileManagerState(
    val currentPath: String = Environment.getExternalStorageDirectory()?.absolutePath ?: "/",
    val rawFiles: List<FileItem> = emptyList(),
    val filteredFiles: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val sortType: SortType = SortType.NAME,
    val sortAscending: Boolean = true,
    val selectedFiles: Set<String> = emptySet(),
    val clipboardFiles: List<String> = emptyList(),
    val isCutOperation: Boolean = false,
    val freeSpaceBytes: Long = 0L,
    val totalSpaceBytes: Long = 0L,
    val message: String? = null,
    val inlineInstallState: InlineInstallState? = null,
    val isLoading: Boolean = false,
    val cloudConnections: List<com.example.lynk.core.domain.cloud.CloudConnection> = emptyList(),
    val activeCloudConnection: com.example.lynk.core.domain.cloud.CloudConnection? = null
)

data class ApkInstallerState(
    val selectedApkPath: String? = null,
    val isInstalling: Boolean = false,
    val installResult: InstallResult? = null,
    val activeStep: InstallStep? = null,
    val stepStatuses: Map<InstallStep, String> = emptyMap(),
    val installedApps: List<AppItem> = emptyList()
)

data class SystemInfoState(
    val deviceInfo: DeviceInfo = DeviceInfo("Unknown", "Unknown", "Unknown", "Unknown", 0, "Unknown", emptyMap()),
    val isRecordingLogs: Boolean = false,
    val logsText: String = "",
    val adbIpAddress: String = "127.0.0.1",
    val adbPort: String = "5555",
    val isAdbConnected: Boolean = false,
    val adbStatusMessage: String? = null,
    val isAdbLoading: Boolean = false
)

data class FloatingButtonsState(
    val selectedProfileId: Int = 1,
    val profiles: List<OverlayProfile> = emptyList(),
    val config: FloatingButtonConfig = FloatingButtonConfig(),
    val isOverlayPermissionGranted: Boolean = false,
    val isUsageStatsPermissionGranted: Boolean = false,
    val isServiceRunning: Boolean = false,
    val availableApps: List<AppItem> = emptyList()
) {
    val activeProfile: OverlayProfile
        get() = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull() ?: OverlayProfile()
}

data class AppUpdateUiState(
    val updateInfo: com.example.lynk.core.domain.update.UpdateInfo? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val statusMessage: String? = null
)

data class DashboardState(
    val selectedTab: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.RU,
    val fileManagerState: FileManagerState = FileManagerState(),
    val installerState: ApkInstallerState = ApkInstallerState(),
    val systemInfoState: SystemInfoState = SystemInfoState(),
    val floatingButtonsState: FloatingButtonsState = FloatingButtonsState(),
    val updateState: AppUpdateUiState = AppUpdateUiState()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // private val appUpdateManager = com.example.lynk.core.domain.update.AppUpdateManager("1.0.0")

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var activeCloudConnection: com.example.lynk.core.domain.cloud.CloudConnection? = null
    private val savedCloudConnections = mutableListOf<com.example.lynk.core.domain.cloud.CloudConnection>()

    private var logcatProcess: Process? = null
    private var logReaderThread: Thread? = null
    private val recordedLogsBuilder = StringBuilder()

    init {
        // Load initial app language
        val prefs = application.getSharedPreferences("${application.packageName}_preferences", Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString("app_language", AppLanguage.RU.code) ?: AppLanguage.RU.code
        val initialLang = AppLanguage.fromCode(savedLangCode)
        ru.doGood.Lynk.feature.dashboard.util.LocaleHelper.applyLanguage(application, initialLang)
        _state.update { it.copy(appLanguage = initialLang) }

        loadDirectory(Environment.getExternalStorageDirectory()?.absolutePath ?: "/")
        loadDeviceInfo()
        loadAdbInfo()
        loadInstalledApps()
        loadFloatingConfig()
        checkForUpdates()
        loadCloudConnections()
    }
    
    private fun loadCloudConnections() {
        val prefs = getApplication<Application>().getSharedPreferences("lynk_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("cloud_connections_count", 0)
        savedCloudConnections.clear()
        for (i in 0 until count) {
            val id = prefs.getString("cloud_id_$i", "") ?: ""
            val name = prefs.getString("cloud_name_$i", "") ?: ""
            val url = prefs.getString("cloud_url_$i", "") ?: ""
            val username = prefs.getString("cloud_username_$i", "") ?: ""
            val token = prefs.getString("cloud_token_$i", "") ?: ""
            
            if (id.isNotEmpty()) {
                val conn = com.example.lynk.core.domain.cloud.CloudConnection(id, name, url, username, token)
                savedCloudConnections.add(conn)
            }
        }
        
        _state.update {
            it.copy(
                fileManagerState = it.fileManagerState.copy(
                    cloudConnections = savedCloudConnections.toList(),
                    activeCloudConnection = activeCloudConnection
                )
            )
        }
    }
    
    fun saveCloudConnection(connection: com.example.lynk.core.domain.cloud.CloudConnection) {
        val existingIndex = savedCloudConnections.indexOfFirst { it.id == connection.id }
        if (existingIndex >= 0) {
            savedCloudConnections[existingIndex] = connection
        } else {
            savedCloudConnections.add(connection)
        }
        saveCloudConnectionsToPrefs()
        selectCloudConnection(connection)
    }

    fun removeCloudConnection(id: String) {
        savedCloudConnections.removeAll { it.id == id }
        saveCloudConnectionsToPrefs()
        if (activeCloudConnection?.id == id) {
            activeCloudConnection = null
            _state.update {
                it.copy(
                    fileManagerState = it.fileManagerState.copy(
                        cloudConnections = savedCloudConnections.toList(),
                        activeCloudConnection = null
                    )
                )
            }
            loadDirectory(Environment.getExternalStorageDirectory()?.absolutePath ?: "/")
        } else {
            _state.update {
                it.copy(
                    fileManagerState = it.fileManagerState.copy(
                        cloudConnections = savedCloudConnections.toList()
                    )
                )
            }
        }
    }

    fun selectCloudConnection(connection: com.example.lynk.core.domain.cloud.CloudConnection) {
        activeCloudConnection = connection
        _state.update {
            it.copy(
                fileManagerState = it.fileManagerState.copy(
                    activeCloudConnection = connection
                )
            )
        }
        loadDirectory("webdav:///")
    }

    private fun saveCloudConnectionsToPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("lynk_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("cloud_connections_count", savedCloudConnections.size)
            for ((index, conn) in savedCloudConnections.withIndex()) {
                putString("cloud_id_$index", conn.id)
                putString("cloud_name_$index", conn.name)
                putString("cloud_url_$index", conn.webDavUrl)
                putString("cloud_username_$index", conn.username)
                putString("cloud_token_$index", conn.passwordToken)
            }
        }.apply()
        _state.update {
            it.copy(
                fileManagerState = it.fileManagerState.copy(
                    cloudConnections = savedCloudConnections.toList(),
                    activeCloudConnection = activeCloudConnection
                )
            )
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _state.update { it.copy(selectedTab = tabIndex) }
    }

    // --- Theme Switching ---
    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
    }

    fun cycleThemeMode() {
        _state.update { current ->
            val nextMode = when (current.themeMode) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
            current.copy(themeMode = nextMode)
        }
    }

    // --- Language Switching ---
    fun setAppLanguage(language: AppLanguage) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", language.code).apply()

        ru.doGood.Lynk.feature.dashboard.util.LocaleHelper.applyLanguage(app, language)

        _state.update { it.copy(appLanguage = language) }
    }

    // --- App Updates ---
    fun checkForUpdates() {
        _state.update { it.copy(updateState = it.updateState.copy(isChecking = true, statusMessage = "Проверка обновлений...")) }
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                try {
                    val app = getApplication<Application>()
                    val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
                    val currentVersion = pInfo.versionName
                    val repoPath = app.getString(R.string.github_repo_path)
                    val appUpdateManager = com.example.lynk.core.domain.update.AppUpdateManager(currentVersion, repoPath)
                    appUpdateManager.checkForUpdates()
                } catch(e: Exception) {
                    com.example.lynk.core.domain.update.UpdateInfo("1.0.0", "", "", com.example.lynk.core.domain.update.UpdateInfo.UpdateState.ERROR)
                }
            }
            _state.update {
                it.copy(
                    updateState = it.updateState.copy(
                        updateInfo = info,
                        isChecking = false,
                        statusMessage = if (info.state == com.example.lynk.core.domain.update.UpdateInfo.UpdateState.UPDATE_AVAILABLE) "Доступна новая версия v${info.latestVersion}!" else "У вас установлена актуальная версия."
                    )
                )
            }
        }
    }

    fun startDownloadUpdate() {
        val info = _state.value.updateState.updateInfo ?: return
        val url = info.downloadUrl ?: return
        
        viewModelScope.launch {
            _state.update {
                it.copy(
                    updateState = it.updateState.copy(
                        isDownloading = true,
                        isDownloaded = false,
                        downloadProgress = 0f,
                        statusMessage = "Загрузка пакета v${info.latestVersion}..."
                    )
                )
            }

            val app = getApplication<Application>()
            val downloader = ru.doGood.Lynk.feature.dashboard.utils.ApkDownloader(app)
            downloader.downloadFile(
                url = url,
                fileName = "Lynk-update.apk",
                scope = viewModelScope,
                onProgress = { progress ->
                    val percentage = (progress * 100).toInt()
                    _state.update { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                downloadProgress = progress,
                                statusMessage = "Скачивание... $percentage%"
                            )
                        )
                    }
                },
                onComplete = { _ ->
                    _state.update { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isDownloading = false,
                                isDownloaded = true,
                                downloadProgress = 1f,
                                statusMessage = "Загрузка завершена. Запуск установки..."
                            )
                        )
                    }
                }
            )
        }
    }

    fun installDownloadedUpdate() {
        val app = getApplication<Application>()
        val downloader = ru.doGood.Lynk.feature.dashboard.utils.ApkDownloader(app)
        val success = downloader.installDownloadedApk("Lynk-update.apk")
        if (!success) {
            _state.update {
                it.copy(
                    updateState = it.updateState.copy(
                        statusMessage = "Файл обновления не найден. Запустите загрузку повторно."
                    )
                )
            }
        }
    }

    // --- File Explorer ---
    fun loadDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(fileManagerState = it.fileManagerState.copy(isLoading = true, message = null))
            }
            val filesList = mutableListOf<FileItem>()
            var freeBytes = 0L
            var totalBytes = 0L
            var currentPath = path
            
            if (path.startsWith("webdav://")) {
                if (activeCloudConnection == null && savedCloudConnections.isNotEmpty()) {
                    activeCloudConnection = savedCloudConnections.first()
                }
                val activeConn = activeCloudConnection
                if (activeConn != null) {
                    try {
                        val client = com.example.lynk.core.domain.cloud.WebDavClient(activeConn)
                        filesList.addAll(client.listFiles(path))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                activeCloudConnection = null
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    val childFiles = dir.listFiles() ?: emptyArray()
                    for (file in childFiles) {
                        filesList.add(
                            FileItem(
                                file.name,
                                file.absolutePath,
                                file.length(),
                                file.lastModified(),
                                file.isDirectory
                            )
                        )
                    }

                    try {
                        val stat = StatFs(dir.absolutePath)
                        freeBytes = stat.availableBytes
                        totalBytes = stat.totalBytes
                    } catch (ignored: Exception) { }
                    currentPath = dir.absolutePath
                }
            }

            withContext(Dispatchers.Main) {
                _state.update {
                    val currentFm = it.fileManagerState
                    val newFm = currentFm.copy(
                        currentPath = currentPath,
                        rawFiles = filesList,
                        freeSpaceBytes = freeBytes,
                        totalSpaceBytes = totalBytes,
                        selectedFiles = emptySet(),
                        isLoading = false,
                        activeCloudConnection = activeCloudConnection
                    )
                    it.copy(fileManagerState = applyFilterAndSort(newFm))
                }
            }
        }
    }

    fun navigateUp() {
        val currentPath = _state.value.fileManagerState.currentPath
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        if (currentPath == rootPath) return
        
        if (currentPath.startsWith("webdav://")) {
            val withoutPrefix = currentPath.substring(9)
            val withoutTrailing = if (withoutPrefix.endsWith("/")) withoutPrefix.substring(0, withoutPrefix.length - 1) else withoutPrefix
            val lastSlash = withoutTrailing.lastIndexOf('/')
            
            if (lastSlash > 0) {
                val parentPath = "webdav://" + withoutTrailing.substring(0, lastSlash)
                loadDirectory(parentPath)
            } else if (lastSlash == 0) {
                val parentPath = "webdav://" + withoutTrailing.substring(0, 1) // just "/"
                loadDirectory(parentPath)
            }
            return
        }

        val parent = File(currentPath).parentFile
        if (parent?.exists() == true) {
            loadDirectory(parent.absolutePath)
        }
    }

    fun downloadCloudFile(
        fileItem: FileItem,
        installAfterDownload: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(fileManagerState = it.fileManagerState.copy(isLoading = true))
            }
            val activeConn = activeCloudConnection
            val client = com.example.lynk.core.domain.cloud.WebDavClient(activeConn)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val lynkDir = File(downloadsDir, "Lynk")
            if (!lynkDir.exists()) {
                lynkDir.mkdirs()
            }
            val destFile = File(lynkDir, fileItem.name)

            try {
                client.downloadFile(fileItem, destFile)

                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(fileManagerState = it.fileManagerState.copy(isLoading = false))
                    }
                    val localFileItem = FileItem(
                        destFile.name,
                        destFile.absolutePath,
                        destFile.length(),
                        destFile.lastModified(),
                        false
                    )

                    val app = getApplication<Application>()
                    if (installAfterDownload) {
                        android.widget.Toast.makeText(
                            app,
                            app.getString(R.string.cloud_download_success_install, fileItem.name),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        startInlineWaterfallInstall(localFileItem)
                    } else {
                        android.widget.Toast.makeText(
                            app,
                            app.getString(R.string.cloud_download_success, fileItem.name),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val app = getApplication<Application>()
                    _state.update {
                        it.copy(fileManagerState = it.fileManagerState.copy(isLoading = false))
                    }
                    android.widget.Toast.makeText(
                        app,
                        app.getString(R.string.cloud_download_failed, e.message ?: ""),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update {
            val updatedFm = it.fileManagerState.copy(searchQuery = query)
            it.copy(fileManagerState = applyFilterAndSort(updatedFm))
        }
    }

    fun setSortType(sortType: SortType) {
        _state.update {
            val updatedFm = it.fileManagerState.copy(sortType = sortType)
            it.copy(fileManagerState = applyFilterAndSort(updatedFm))
        }
    }

    fun toggleSortDirection() {
        _state.update {
            val updatedFm = it.fileManagerState.copy(sortAscending = !it.fileManagerState.sortAscending)
            it.copy(fileManagerState = applyFilterAndSort(updatedFm))
        }
    }

    fun toggleFileSelection(path: String) {
        _state.update {
            val currentSelected = it.fileManagerState.selectedFiles.toMutableSet()
            if (currentSelected.contains(path)) {
                currentSelected.remove(path)
            } else {
                currentSelected.add(path)
            }
            it.copy(fileManagerState = it.fileManagerState.copy(selectedFiles = currentSelected))
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(fileManagerState = it.fileManagerState.copy(selectedFiles = emptySet()))
        }
    }

    fun copySelectedFiles() {
        val selected = _state.value.fileManagerState.selectedFiles.toList()
        _state.update {
            it.copy(
                fileManagerState = it.fileManagerState.copy(
                    clipboardFiles = selected,
                    isCutOperation = false,
                    selectedFiles = emptySet()
                )
            )
        }
    }

    fun cutSelectedFiles() {
        val selected = _state.value.fileManagerState.selectedFiles.toList()
        _state.update {
            it.copy(
                fileManagerState = it.fileManagerState.copy(
                    clipboardFiles = selected,
                    isCutOperation = true,
                    selectedFiles = emptySet()
                )
            )
        }
    }

    fun pasteFiles() {
        val fmState = _state.value.fileManagerState
        val targetDir = File(fmState.currentPath)
        val filesToPaste = fmState.clipboardFiles

        viewModelScope.launch(Dispatchers.IO) {
            for (filePath in filesToPaste) {
                val src = File(filePath)
                if (!src.exists()) continue
                val dest = File(targetDir, src.name)
                if (fmState.isCutOperation) {
                    src.renameTo(dest)
                } else {
                    src.copyTo(dest, overwrite = true)
                }
            }
            loadDirectory(targetDir.absolutePath)
        }
    }

    fun deleteFiles(paths: List<String>) {
        val currentPath = _state.value.fileManagerState.currentPath
        viewModelScope.launch(Dispatchers.IO) {
            for (filePath in paths) {
                File(filePath).deleteRecursively()
            }
            _state.update { state ->
                val newSelected = state.fileManagerState.selectedFiles - paths.toSet()
                state.copy(
                    fileManagerState = state.fileManagerState.copy(
                        selectedFiles = newSelected
                    )
                )
            }
            loadDirectory(currentPath)
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val src = File(oldPath)
            if (src.exists() && newName.isNotBlank()) {
                val dest = File(src.parentFile, newName)
                if (src.renameTo(dest)) {
                    loadDirectory(_state.value.fileManagerState.currentPath)
                }
            }
        }
    }

    fun startInlineWaterfallInstall(fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) return

        val initialStatuses = InstallStep.values().associateWith<InstallStep, Pair<Boolean?, String>> {
            null to "Waiting..."
        }.toMutableMap()

        _state.update {
            it.copy(
                fileManagerState = it.fileManagerState.copy(
                    inlineInstallState = InlineInstallState(
                        apkFile = fileItem,
                        isInstalling = true,
                        stepStatuses = initialStatuses
                    )
                )
            )
        }

        val updateStep = { step: InstallStep, isSuccess: Boolean?, status: String ->
            _state.update { state ->
                val currentInline = state.fileManagerState.inlineInstallState ?: return@update state
                val newStatuses = currentInline.stepStatuses.toMutableMap()
                newStatuses[step] = isSuccess to status
                state.copy(
                    fileManagerState = state.fileManagerState.copy(
                        inlineInstallState = currentInline.copy(
                            activeStep = step,
                            stepStatuses = newStatuses
                        )
                    )
                )
            }
        }

        val pine = ApkInstaller { _, callback ->
            updateStep(InstallStep.PINE, null, "Hook checking...")
            updateStep(InstallStep.PINE, false, "Pine hook unavailable")
            callback.onResult(InstallResult(false, "Pine hook unavailable", InstallStep.PINE))
        }

        val shizuku = ApkInstaller { _, callback ->
            updateStep(InstallStep.SHIZUKU, null, "Checking Shizuku API...")
            updateStep(InstallStep.SHIZUKU, false, "Shizuku permission not granted")
            callback.onResult(InstallResult(false, "Shizuku permission not granted", InstallStep.SHIZUKU))
        }

        val nativeAdb = ApkInstaller { _, callback ->
            updateStep(InstallStep.NATIVE_ADB, null, "Connecting Native ADB...")
            updateStep(InstallStep.NATIVE_ADB, false, "Native ADB port closed")
            callback.onResult(InstallResult(false, "Native ADB port closed", InstallStep.NATIVE_ADB))
        }

        val localAdb = ApkInstaller { _, callback ->
            updateStep(InstallStep.LOCAL_ADB, null, "Connecting Local ADB...")
            updateStep(InstallStep.LOCAL_ADB, false, "Local ADB pairing required")
            callback.onResult(InstallResult(false, "Local ADB pairing required", InstallStep.LOCAL_ADB))
        }

        val packageInstaller = ApkInstaller { apkFile, callback ->
            updateStep(InstallStep.PACKAGE_INSTALLER, null, "Launching Package Installer...")
            try {
                val application = getApplication<Application>()
                val apkUri = androidx.core.content.FileProvider.getUriForFile(
                    application,
                    application.packageName + ".fileprovider",
                    apkFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
                updateStep(InstallStep.PACKAGE_INSTALLER, true, "Sent to Package Installer")
                callback.onResult(InstallResult(true, "Sent to Android Package Installer via FileProvider", InstallStep.PACKAGE_INSTALLER))
            } catch (e: Exception) {
                updateStep(InstallStep.PACKAGE_INSTALLER, false, "FileProvider error: ${e.message}")
                callback.onResult(InstallResult(false, "FileProvider error: ${e.message}", InstallStep.PACKAGE_INSTALLER))
            }
        }

        val strategy = WaterfallInstallStrategy(pine, shizuku, nativeAdb, localAdb, packageInstaller)
        strategy.executeInstall(file) { result ->
            _state.update { state ->
                val currentInline = state.fileManagerState.inlineInstallState ?: return@update state
                state.copy(
                    fileManagerState = state.fileManagerState.copy(
                        inlineInstallState = currentInline.copy(
                            isInstalling = false,
                            installResult = result
                        )
                    )
                )
            }
        }
    }

    fun dismissInlineInstall() {
        _state.update {
            it.copy(fileManagerState = it.fileManagerState.copy(inlineInstallState = null))
        }
    }

    private fun applyFilterAndSort(fmState: FileManagerState): FileManagerState {
        val query = fmState.searchQuery.trim().lowercase(Locale.getDefault())

        val filtered = fmState.rawFiles.filter { item ->
            item.isUpNavigation || query.isEmpty() || item.name.lowercase(Locale.getDefault()).contains(query)
        }

        val upItem = filtered.find { it.isUpNavigation }
        val restItems = filtered.filter { !it.isUpNavigation }

        val sorted = restItems.sortedWith { f1, f2 ->
            // Directories always first
            if (f1.isDirectory && !f2.isDirectory) return@sortedWith -1
            if (!f1.isDirectory && f2.isDirectory) return@sortedWith 1

            val comp = when (fmState.sortType) {
                SortType.NAME -> f1.name.compareTo(f2.name, ignoreCase = true)
                SortType.SIZE -> f1.size.compareTo(f2.size)
                SortType.DATE -> f1.lastModified.compareTo(f2.lastModified)
                SortType.TYPE -> {
                    val ext1 = f1.name.substringAfterLast('.', "")
                    val ext2 = f2.name.substringAfterLast('.', "")
                    ext1.compareTo(ext2, ignoreCase = true)
                }
            }
            if (fmState.sortAscending) comp else -comp
        }

        val finalResult = mutableListOf<FileItem>()
        if (upItem != null) finalResult.add(upItem)
        finalResult.addAll(sorted)

        return fmState.copy(filteredFiles = finalResult)
    }

    // --- APK Installer ---
    fun runWaterfallInstall() {
        val apkPath = _state.value.installerState.selectedApkPath ?: return
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return

        _state.update {
            it.copy(
                installerState = it.installerState.copy(
                    isInstalling = true,
                    installResult = null,
                    stepStatuses = emptyMap()
                )
            )
        }

        // Concrete ApkInstallers for domain WaterfallInstallStrategy
        val pine = ApkInstaller { _, callback ->
            updateStepStatus(InstallStep.PINE, "Hook checking...")
            callback.onResult(InstallResult(false, "Pine hook unavailable", InstallStep.PINE))
        }

        val shizuku = ApkInstaller { _, callback ->
            updateStepStatus(InstallStep.SHIZUKU, "Checking Shizuku API...")
            callback.onResult(InstallResult(false, "Shizuku permission not granted", InstallStep.SHIZUKU))
        }

        val nativeAdb = ApkInstaller { _, callback ->
            updateStepStatus(InstallStep.NATIVE_ADB, "Connecting Native ADB...")
            callback.onResult(InstallResult(false, "Native ADB port closed", InstallStep.NATIVE_ADB))
        }

        val localAdb = ApkInstaller { _, callback ->
            updateStepStatus(InstallStep.LOCAL_ADB, "Connecting Local ADB...")
            callback.onResult(InstallResult(false, "Local ADB pairing required", InstallStep.LOCAL_ADB))
        }

        val packageInstaller = ApkInstaller { file, callback ->
            updateStepStatus(InstallStep.PACKAGE_INSTALLER, "Launching Package Installer...")
            try {
                val application = getApplication<Application>()
                val apkUri = androidx.core.content.FileProvider.getUriForFile(
                    application,
                    application.packageName + ".fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
                callback.onResult(InstallResult(true, "Sent to Android Package Installer via FileProvider", InstallStep.PACKAGE_INSTALLER))
            } catch (e: Exception) {
                callback.onResult(InstallResult(false, "FileProvider error: ${e.message}", InstallStep.PACKAGE_INSTALLER))
            }
        }

        val strategy = WaterfallInstallStrategy(pine, shizuku, nativeAdb, localAdb, packageInstaller)
        strategy.executeInstall(apkFile) { result ->
            _state.update {
                it.copy(
                    installerState = it.installerState.copy(
                        isInstalling = false,
                        installResult = result,
                        activeStep = result.step
                    )
                )
            }
        }
    }

    fun runSingleInstallStep(step: InstallStep) {
        val apkPath = _state.value.installerState.selectedApkPath ?: return
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return

        updateStepStatus(step, "Executing $step...")
        _state.update {
            it.copy(
                installerState = it.installerState.copy(
                    activeStep = step,
                    installResult = InstallResult(true, "Executed $step directly", step)
                )
            )
        }
    }

    private fun updateStepStatus(step: InstallStep, status: String) {
        _state.update {
            val currentMap = it.installerState.stepStatuses.toMutableMap()
            currentMap[step] = status
            it.copy(
                installerState = it.installerState.copy(
                    activeStep = step,
                    stepStatuses = currentMap
                )
            )
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appItems = mutableListOf<AppItem>()

            for (appInfo in packages) {
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    appItems.add(AppItem(label, appInfo.packageName, false))
                }
            }

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(installerState = it.installerState.copy(installedApps = appItems))
                }
            }
        }
    }

    fun toggleAppChecked(app: AppItem, isChecked: Boolean) {
        app.isChecked = isChecked
        _state.update {
            val updatedList = it.installerState.installedApps.toList()
            it.copy(installerState = it.installerState.copy(installedApps = updatedList))
        }
    }

    // --- System Info, Wireless ADB & Logger ---
    private val adbWirelessManager = com.example.lynk.core.domain.system.AdbWirelessManager()

    fun loadAdbInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val autoIp = adbWirelessManager.wifiIpAddress
            val currentIp = _state.value.systemInfoState.adbIpAddress
            val effectiveIp = if (currentIp == "127.0.0.1" || currentIp.isBlank()) autoIp else currentIp
            val portStr = _state.value.systemInfoState.adbPort.ifBlank { "5555" }
            val port = portStr.toIntOrNull() ?: 5555
            val connected = adbWirelessManager.isConnected(effectiveIp, port)
            _state.update {
                it.copy(
                    systemInfoState = it.systemInfoState.copy(
                        adbIpAddress = effectiveIp,
                        isAdbConnected = connected
                    )
                )
            }
        }
    }

    fun updateAdbIpAddress(ip: String) {
        _state.update {
            it.copy(systemInfoState = it.systemInfoState.copy(adbIpAddress = ip))
        }
    }

    fun updateAdbPort(port: String) {
        _state.update {
            it.copy(systemInfoState = it.systemInfoState.copy(adbPort = port))
        }
    }

    fun connectAdb() {
        val ip = _state.value.systemInfoState.adbIpAddress
        val portStr = _state.value.systemInfoState.adbPort
        val port = portStr.toIntOrNull() ?: 5555
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(systemInfoState = it.systemInfoState.copy(isAdbLoading = true, adbStatusMessage = null))
            }
            val result = adbWirelessManager.connect(ip, port)
            val connected = adbWirelessManager.isConnected(ip, port)
            _state.update {
                it.copy(
                    systemInfoState = it.systemInfoState.copy(
                        isAdbConnected = connected,
                        adbStatusMessage = result,
                        isAdbLoading = false
                    )
                )
            }
        }
    }

    fun disconnectAdb() {
        val ip = _state.value.systemInfoState.adbIpAddress
        val portStr = _state.value.systemInfoState.adbPort
        val port = portStr.toIntOrNull() ?: 5555
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(systemInfoState = it.systemInfoState.copy(isAdbLoading = true, adbStatusMessage = null))
            }
            val result = adbWirelessManager.disconnect(ip, port)
            val connected = adbWirelessManager.isConnected(ip, port)
            _state.update {
                it.copy(
                    systemInfoState = it.systemInfoState.copy(
                        isAdbConnected = connected,
                        adbStatusMessage = result,
                        isAdbLoading = false
                    )
                )
            }
        }
    }

    fun refreshAdbIp() {
        viewModelScope.launch(Dispatchers.IO) {
            val autoIp = adbWirelessManager.wifiIpAddress
            _state.update {
                it.copy(systemInfoState = it.systemInfoState.copy(adbIpAddress = autoIp))
            }
        }
    }

    private fun loadDeviceInfo() {
        val carProps = mutableMapOf<String, String>()
        val propKeys = arrayOf("ro.vin", "ro.car.vin", "ro.product.model", "ro.flyme.version", "persist.sys.locale")

        for (key in propKeys) {
            val propVal = getSystemProperty(key)
            if (!propVal.isNullOrEmpty()) {
                carProps[key] = propVal
            }
        }

        val info = DeviceInfo(
            Build.MANUFACTURER,
            Build.BRAND,
            Build.MODEL,
            Build.DEVICE,
            Build.VERSION.SDK_INT,
            Build.VERSION.RELEASE,
            carProps
        )

        _state.update {
            it.copy(systemInfoState = it.systemInfoState.copy(deviceInfo = info))
        }
    }

    fun startRecordingLogs() {
        if (_state.value.systemInfoState.isRecordingLogs) return

        _state.update {
            it.copy(systemInfoState = it.systemInfoState.copy(isRecordingLogs = true))
        }

        recordedLogsBuilder.setLength(0)
        recordedLogsBuilder.append("=== LOGCAT RECORDING STARTED ===\n")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec("logcat -c").waitFor()
                logcatProcess = Runtime.getRuntime().exec("logcat -v time")

                logReaderThread = Thread {
                    try {
                        val reader = BufferedReader(InputStreamReader(logcatProcess?.inputStream))
                        var line: String? = reader.readLine()
                        while (_state.value.systemInfoState.isRecordingLogs && line != null) {
                            synchronized(recordedLogsBuilder) {
                                recordedLogsBuilder.append(line).append("\n")
                            }
                            if (recordedLogsBuilder.length % 1000 < 100) {
                                val currentLogs = recordedLogsBuilder.toString()
                                _state.update {
                                    it.copy(systemInfoState = it.systemInfoState.copy(logsText = currentLogs))
                                }
                            }
                            line = reader.readLine()
                        }
                    } catch (ignored: Exception) { }
                }
                logReaderThread?.start()
            } catch (e: Exception) {
                recordedLogsBuilder.append("Error starting logcat: ${e.message}\n")
            }
        }
    }

    fun stopRecordingLogs() {
        _state.update {
            it.copy(systemInfoState = it.systemInfoState.copy(isRecordingLogs = false))
        }

        logcatProcess?.destroy()
        logcatProcess = null

        synchronized(recordedLogsBuilder) {
            recordedLogsBuilder.append("=== LOGCAT RECORDING STOPPED ===\n")
            val finalLogs = recordedLogsBuilder.toString()
            _state.update {
                it.copy(systemInfoState = it.systemInfoState.copy(logsText = finalLogs))
            }
        }
    }

    fun clearLogs() {
        recordedLogsBuilder.setLength(0)
        _state.update {
            it.copy(systemInfoState = it.systemInfoState.copy(logsText = ""))
        }
    }

    fun saveLogsToFile(context: Context) {
        val currentLogs = _state.value.systemInfoState.logsText
        if (currentLogs.isEmpty()) {
            android.widget.Toast.makeText(context, "No logs to save", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "logcat_${System.currentTimeMillis()}.txt"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "Lynk")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            val file = File(appDir, fileName)
            file.writeText(currentLogs)
            android.widget.Toast.makeText(context, "Logs saved to Downloads/Lynk/$fileName", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to save logs: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod: Method = clazz.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }

    // --- Floating Buttons & Multi-Overlay Manager ---

    fun getLatestProfile(profileId: Int): OverlayProfile {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)

        val pPrefix = "overlay_${profileId}_"
        val hasProfileKeys = prefs.contains("${pPrefix}enabled") || prefs.contains("${pPrefix}quick_launch_enabled")

        val defaultEnabled = if (hasProfileKeys) {
            prefs.getBoolean("${pPrefix}enabled", false)
        } else if (profileId == 1) {
            prefs.getBoolean("quick_launch_enabled", false) ||
            prefs.getBoolean("home_navigator_enabled", false) ||
            prefs.getBoolean("back_navigator_enabled", false) ||
            prefs.getBoolean("fullscreen_overlay_enabled", false) ||
            true
        } else {
            false
        }

        val name = prefs.getString("${pPrefix}name", "Оверлей $profileId") ?: "Оверлей $profileId"
        val isSeparateButtons = if (hasProfileKeys) {
            prefs.getBoolean("${pPrefix}separate_buttons", false)
        } else if (profileId == 1) {
            prefs.getBoolean("separate_buttons_enabled", false)
        } else {
            false
        }

        val config = FloatingButtonConfig()

        fun getBool(key: String, legacyKey: String, def: Boolean): Boolean {
            return if (prefs.contains("${pPrefix}$key")) prefs.getBoolean("${pPrefix}$key", def)
                   else if (profileId == 1) prefs.getBoolean(legacyKey, def)
                   else def
        }
        fun getInt(key: String, legacyKey: String, def: Int): Int {
            return if (prefs.contains("${pPrefix}$key")) prefs.getInt("${pPrefix}$key", def)
                   else if (profileId == 1) prefs.getInt(legacyKey, def)
                   else def
        }
        fun getString(key: String, legacyKey: String, def: String): String {
            return if (prefs.contains("${pPrefix}$key")) prefs.getString("${pPrefix}$key", def) ?: def
                   else if (profileId == 1) prefs.getString(legacyKey, def) ?: def
                   else def
        }
        fun getStringSet(key: String, legacyKey: String): List<String> {
            val set = if (prefs.contains("${pPrefix}$key")) prefs.getStringSet("${pPrefix}$key", emptySet())
                      else if (profileId == 1) prefs.getStringSet(legacyKey, emptySet())
                      else emptySet()
            return ArrayList(set ?: emptySet())
        }

        config.isQuickLaunchEnabled = getBool("quick_launch_enabled", "quick_launch_enabled", false)
        config.quickLaunchApps = getStringSet("quick_launch_apps", "quick_launch_apps")
        config.quickLaunchButtonSize = getInt("ql_button_size", "ql_button_size", 48)
        config.quickLaunchOpacityPercent = getInt("ql_opacity_percent", "ql_opacity_percent", 85)

        config.isFullscreenOverlayEnabled = getBool("fullscreen_overlay_enabled", "fullscreen_overlay_enabled", false)
        config.fullscreenApps = getStringSet("fullscreen_apps", "fullscreen_apps")
        config.fullscreenButtonSize = getInt("fs_button_size", "fs_button_size", 48)
        config.fullscreenOpacityPercent = getInt("fs_opacity_percent", "fs_opacity_percent", 85)

        config.isHomeNavigatorEnabled = getBool("home_navigator_enabled", "home_navigator_enabled", false)
        config.homeButtonSize = getInt("home_button_size", "home_button_size", 48)
        config.homeOpacityPercent = getInt("home_opacity_percent", "home_opacity_percent", 85)

        config.isBackNavigatorEnabled = getBool("back_navigator_enabled", "back_navigator_enabled", false)
        config.backButtonSize = getInt("back_button_size", "back_button_size", 48)
        config.backOpacityPercent = getInt("back_opacity_percent", "back_opacity_percent", 85)

        config.isRefreshNavigatorEnabled = getBool("refresh_navigator_enabled", "refresh_navigator_enabled", false)
        config.refreshButtonSize = getInt("refresh_button_size", "refresh_button_size", 48)
        config.refreshOpacityPercent = getInt("refresh_opacity_percent", "refresh_opacity_percent", 85)

        config.isFreeformWindowEnabled = getBool("freeform_window_enabled", "freeform_window_enabled", false)
        config.freeformButtonSize = getInt("freeform_button_size", "freeform_button_size", 48)
        config.freeformOpacityPercent = getInt("freeform_opacity_percent", "freeform_opacity_percent", 85)

        config.isSeparateButtonsEnabled = isSeparateButtons
        config.combinedButtonSize = getInt("combined_button_size", "combined_button_size", 48)
        config.isSecondaryDisplayMirroring = getBool("secondary_display_mirroring", "secondary_display_mirroring", true)
        config.opacityPercent = getInt("opacity_percent", "opacity_percent", 85)

        config.buttonColor = getString("button_color", "button_color", "#7C4DFF")
        config.shape = getString("button_shape", "button_shape", "CIRCLE")

        config.quickLaunchColorHex = getString("ql_color_hex", "ql_color_hex", "#6750A4")
        config.quickLaunchShape = getString("ql_shape", "ql_shape", "CIRCLE")

        config.fullscreenColorHex = getString("fs_color_hex", "fs_color_hex", "#1976D2")
        config.fullscreenShape = getString("fs_shape", "fs_shape", "CIRCLE")

        config.homeColorHex = getString("home_color_hex", "home_color_hex", "#388E3C")
        config.homeShape = getString("home_shape", "home_shape", "CIRCLE")

        config.backColorHex = getString("back_color_hex", "back_color_hex", "#D32F2F")
        config.backShape = getString("back_shape", "back_shape", "CIRCLE")

        config.refreshColorHex = getString("refresh_color_hex", "refresh_color_hex", "#FFA000")
        config.refreshShape = getString("refresh_shape", "refresh_shape", "CIRCLE")

        config.freeformColorHex = getString("freeform_color_hex", "freeform_color_hex", "#00897B")
        config.freeformShape = getString("freeform_shape", "freeform_shape", "CIRCLE")

        config.syncActionsWithApps()

        return OverlayProfile(profileId, name, defaultEnabled, isSeparateButtons, config)
    }

    fun getLatestFloatingConfig(): FloatingButtonConfig {
        return getLatestProfile(_state.value.floatingButtonsState.selectedProfileId).config
    }

    fun loadProfilesList(): List<OverlayProfile> {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("overlay_profiles_json", null)
        val profileMetaList = mutableListOf<Triple<Int, String, Boolean>>()

        if (!jsonStr.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getInt("id")
                    val name = obj.optString("name", "Оверлей $id")
                    val isEnabled = obj.optBoolean("isEnabled", false)
                    profileMetaList.add(Triple(id, name, isEnabled))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (profileMetaList.isEmpty()) {
            val hasLegacy1 = prefs.contains("overlay_1_enabled") || prefs.contains("overlay_1_name") || prefs.contains("quick_launch_enabled")
            val hasLegacy2 = prefs.contains("overlay_2_enabled") || prefs.contains("overlay_2_name")
            val hasLegacy3 = prefs.contains("overlay_3_enabled") || prefs.contains("overlay_3_name")

            if (hasLegacy1 || hasLegacy2 || hasLegacy3) {
                profileMetaList.add(Triple(1, prefs.getString("overlay_1_name", "Оверлей 1") ?: "Оверлей 1", false))
                if (hasLegacy2) profileMetaList.add(Triple(2, prefs.getString("overlay_2_name", "Оверлей 2") ?: "Оверлей 2", false))
                if (hasLegacy3) profileMetaList.add(Triple(3, prefs.getString("overlay_3_name", "Оверлей 3") ?: "Оверлей 3", false))
            } else {
                profileMetaList.add(Triple(1, "Оверлей 1", false))
            }
        }

        val profiles = profileMetaList.map { (id, _, _) ->
            getLatestProfile(id)
        }
        saveProfilesListToPrefs(prefs, profiles)
        return profiles
    }

    private fun saveProfilesListToPrefs(prefs: android.content.SharedPreferences, profiles: List<OverlayProfile>) {
        val jsonArray = JSONArray()
        for (p in profiles) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("isEnabled", p.isEnabled)
            obj.put("isSeparateButtons", p.isSeparateButtons)
            jsonArray.put(obj)
        }
        prefs.edit().putString("overlay_profiles_json", jsonArray.toString()).apply()
    }

    fun loadFloatingConfig() {
        val profiles = loadProfilesList()
        val currentSelId = _state.value.floatingButtonsState.selectedProfileId
        val selId = if (profiles.any { it.id == currentSelId }) currentSelId else (profiles.firstOrNull()?.id ?: 1)
        val activeProf = profiles.find { it.id == selId } ?: profiles.firstOrNull() ?: OverlayProfile()

        _state.update {
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(
                    selectedProfileId = selId,
                    profiles = profiles,
                    config = activeProf.config.copy(),
                    availableApps = it.installerState.installedApps
                )
            )
        }
    }

    private fun saveAndApplyProfileConfig(profileId: Int, profile: OverlayProfile) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)

        val pPrefix = "overlay_${profileId}_"
        val config = profile.config

        val editor = prefs.edit()
            .putBoolean("${pPrefix}enabled", profile.isEnabled)
            .putString("${pPrefix}name", profile.name)
            .putBoolean("${pPrefix}separate_buttons", profile.isSeparateButtons)

            .putBoolean("${pPrefix}quick_launch_enabled", config.isQuickLaunchEnabled)
            .putStringSet("${pPrefix}quick_launch_apps", config.quickLaunchApps.toSet())
            .putInt("${pPrefix}ql_button_size", config.quickLaunchButtonSize)
            .putInt("${pPrefix}ql_opacity_percent", config.quickLaunchOpacityPercent)

            .putBoolean("${pPrefix}fullscreen_overlay_enabled", config.isFullscreenOverlayEnabled)
            .putStringSet("${pPrefix}fullscreen_apps", config.fullscreenApps.toSet())
            .putInt("${pPrefix}fs_button_size", config.fullscreenButtonSize)
            .putInt("${pPrefix}fs_opacity_percent", config.fullscreenOpacityPercent)

            .putBoolean("${pPrefix}home_navigator_enabled", config.isHomeNavigatorEnabled)
            .putInt("${pPrefix}home_button_size", config.homeButtonSize)
            .putInt("${pPrefix}home_opacity_percent", config.homeOpacityPercent)

            .putBoolean("${pPrefix}back_navigator_enabled", config.isBackNavigatorEnabled)
            .putInt("${pPrefix}back_button_size", config.backButtonSize)
            .putInt("${pPrefix}back_opacity_percent", config.backOpacityPercent)

            .putBoolean("${pPrefix}refresh_navigator_enabled", config.isRefreshNavigatorEnabled)
            .putInt("${pPrefix}refresh_button_size", config.refreshButtonSize)
            .putInt("${pPrefix}refresh_opacity_percent", config.refreshOpacityPercent)

            .putBoolean("${pPrefix}freeform_window_enabled", config.isFreeformWindowEnabled)
            .putInt("${pPrefix}freeform_button_size", config.freeformButtonSize)
            .putInt("${pPrefix}freeform_opacity_percent", config.freeformOpacityPercent)

            .putBoolean("${pPrefix}separate_buttons_enabled", config.isSeparateButtonsEnabled)
            .putInt("${pPrefix}combined_button_size", config.combinedButtonSize)
            .putBoolean("${pPrefix}secondary_display_mirroring", config.isSecondaryDisplayMirroring)
            .putInt("${pPrefix}opacity_percent", config.opacityPercent)
            .putString("${pPrefix}button_color", config.buttonColor)
            .putString("${pPrefix}button_shape", config.shape)

            .putString("${pPrefix}ql_color_hex", config.quickLaunchColorHex)
            .putString("${pPrefix}ql_shape", config.quickLaunchShape)
            .putString("${pPrefix}fs_color_hex", config.fullscreenColorHex)
            .putString("${pPrefix}fs_shape", config.fullscreenShape)
            .putString("${pPrefix}home_color_hex", config.homeColorHex)
            .putString("${pPrefix}home_shape", config.homeShape)
            .putString("${pPrefix}back_color_hex", config.backColorHex)
            .putString("${pPrefix}back_shape", config.backShape)
            .putString("${pPrefix}refresh_color_hex", config.refreshColorHex)
            .putString("${pPrefix}refresh_shape", config.refreshShape)
            .putString("${pPrefix}freeform_color_hex", config.freeformColorHex)
            .putString("${pPrefix}freeform_shape", config.freeformShape)

        if (profileId == 1) {
            editor.putBoolean("quick_launch_enabled", config.isQuickLaunchEnabled)
                .putStringSet("quick_launch_apps", config.quickLaunchApps.toSet())
                .putInt("ql_button_size", config.quickLaunchButtonSize)
                .putInt("ql_opacity_percent", config.quickLaunchOpacityPercent)
                .putBoolean("fullscreen_overlay_enabled", config.isFullscreenOverlayEnabled)
                .putStringSet("fullscreen_apps", config.fullscreenApps.toSet())
                .putInt("fs_button_size", config.fullscreenButtonSize)
                .putInt("fs_opacity_percent", config.fullscreenOpacityPercent)
                .putBoolean("home_navigator_enabled", config.isHomeNavigatorEnabled)
                .putInt("home_button_size", config.homeButtonSize)
                .putInt("home_opacity_percent", config.homeOpacityPercent)
                .putBoolean("back_navigator_enabled", config.isBackNavigatorEnabled)
                .putInt("back_button_size", config.backButtonSize)
                .putInt("back_opacity_percent", config.backOpacityPercent)
                .putBoolean("refresh_navigator_enabled", config.isRefreshNavigatorEnabled)
                .putInt("refresh_button_size", config.refreshButtonSize)
                .putInt("refresh_opacity_percent", config.refreshOpacityPercent)
                .putBoolean("freeform_window_enabled", config.isFreeformWindowEnabled)
                .putInt("freeform_button_size", config.freeformButtonSize)
                .putInt("freeform_opacity_percent", config.freeformOpacityPercent)
                .putBoolean("separate_buttons_enabled", profile.isSeparateButtons)
                .putInt("combined_button_size", config.combinedButtonSize)
                .putBoolean("secondary_display_mirroring", config.isSecondaryDisplayMirroring)
                .putInt("opacity_percent", config.opacityPercent)
                .putString("button_color", config.buttonColor)
                .putString("button_shape", config.shape)
                .putString("ql_color_hex", config.quickLaunchColorHex)
                .putString("ql_shape", config.quickLaunchShape)
                .putString("fs_color_hex", config.fullscreenColorHex)
                .putString("fs_shape", config.fullscreenShape)
                .putString("home_color_hex", config.homeColorHex)
                .putString("home_shape", config.homeShape)
                .putString("back_color_hex", config.backColorHex)
                .putString("back_shape", config.backShape)
                .putString("refresh_color_hex", config.refreshColorHex)
                .putString("refresh_shape", config.refreshShape)
                .putString("freeform_color_hex", config.freeformColorHex)
                .putString("freeform_shape", config.freeformShape)
        }

        editor.apply()

        _state.update {
            val existing = it.floatingButtonsState.profiles
            val updatedProfiles = if (existing.none { p -> p.id == profileId }) {
                ArrayList(existing + profile)
            } else {
                ArrayList(existing.map { p -> if (p.id == profileId) profile else p })
            }
            saveProfilesListToPrefs(prefs, updatedProfiles)
            val activeProfile = updatedProfiles.find { p -> p.id == it.floatingButtonsState.selectedProfileId } ?: profile
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(
                    profiles = updatedProfiles,
                    config = activeProfile.config.copy()
                )
            )
        }

        val intent = Intent("ru.doGood.Lynk.ACTION_UPDATE_OVERLAY")
        intent.setPackage(app.packageName)
        app.sendBroadcast(intent)

        if (_state.value.floatingButtonsState.profiles.any { it.isEnabled }) {
            startOverlayServiceInternal(app)
        }
    }

    private fun startOverlayServiceInternal(app: Context) {
        if (!Settings.canDrawOverlays(app)) return
        try {
            val serviceClass = Class.forName("ru.doGood.Lynk.service.ForegroundOverlayService")
            val startMethod = serviceClass.getMethod("startService", Context::class.java)
            startMethod.invoke(null, app)
        } catch (_: Exception) {
            try {
                val serviceClass = Class.forName("ru.doGood.Lynk.service.ForegroundOverlayService")
                val intent = Intent(app, serviceClass)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    app.startForegroundService(intent)
                } else {
                    app.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val intent = Intent("ru.doGood.Lynk.ACTION_UPDATE_OVERLAY")
        intent.setPackage(app.packageName)
        app.sendBroadcast(intent)
    }

    fun selectOverlayProfile(profileId: Int) {
        _state.update {
            val activeProf = it.floatingButtonsState.profiles.find { p -> p.id == profileId }
                ?: it.floatingButtonsState.profiles.firstOrNull()
                ?: OverlayProfile(profileId, "Оверлей $profileId", false, false, FloatingButtonConfig())
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(
                    selectedProfileId = activeProf.id,
                    config = activeProf.config.copy()
                )
            )
        }
    }

    fun toggleOverlayProfileEnabled(profileId: Int, enabled: Boolean) {
        val currentProfile = _state.value.floatingButtonsState.profiles.find { it.id == profileId }
            ?: getLatestProfile(profileId)
        currentProfile.isEnabled = enabled
        saveAndApplyProfileConfig(profileId, currentProfile)
        if (enabled || _state.value.floatingButtonsState.profiles.any { it.isEnabled }) {
            startOverlayServiceInternal(getApplication())
        }
    }

    fun addOverlayProfile(name: String) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)
        val currentProfiles = _state.value.floatingButtonsState.profiles.toMutableList()
        val nextId = (currentProfiles.maxOfOrNull { it.id } ?: 0) + 1
        val profileName = name.trim().ifBlank { "Оверлей $nextId" }
        val newProfile = OverlayProfile(nextId, profileName, true, false, FloatingButtonConfig())

        currentProfiles.add(newProfile)
        val newList = ArrayList(currentProfiles)
        saveProfilesListToPrefs(prefs, newList)

        _state.update { state ->
            state.copy(
                floatingButtonsState = state.floatingButtonsState.copy(
                    profiles = newList,
                    selectedProfileId = nextId,
                    config = newProfile.config.copy()
                )
            )
        }

        saveAndApplyProfileConfig(nextId, newProfile)
        selectOverlayProfile(nextId)
    }

    fun deleteOverlayProfile(profileId: String) {
        val idInt = profileId.toIntOrNull() ?: return
        deleteOverlayProfile(idInt)
    }

    fun deleteOverlayProfile(profileId: Int) {
        val currentProfiles = _state.value.floatingButtonsState.profiles
        if (currentProfiles.size <= 1) return

        val profileToDelete = currentProfiles.find { it.id == profileId } ?: return
        val updatedProfiles = currentProfiles.filter { it.id != profileId }

        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)

        val pPrefix = "overlay_${profileId}_"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(pPrefix) }.forEach { key ->
            editor.remove(key)
        }
        editor.apply()

        saveProfilesListToPrefs(prefs, updatedProfiles)

        val newSelectedId = if (_state.value.floatingButtonsState.selectedProfileId == profileId) {
            updatedProfiles.first().id
        } else {
            _state.value.floatingButtonsState.selectedProfileId
        }

        val activeProf = updatedProfiles.find { it.id == newSelectedId } ?: updatedProfiles.first()

        _state.update {
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(
                    selectedProfileId = newSelectedId,
                    profiles = updatedProfiles,
                    config = activeProf.config.copy()
                )
            )
        }

        val intent = Intent("ru.doGood.Lynk.ACTION_UPDATE_OVERLAY")
        intent.setPackage(app.packageName)
        app.sendBroadcast(intent)
    }

    fun renameOverlayProfile(profileId: String, newName: String) {
        val idInt = profileId.toIntOrNull() ?: return
        renameOverlayProfile(idInt, newName)
    }

    fun renameOverlayProfile(profileId: Int, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return

        val currentProfiles = _state.value.floatingButtonsState.profiles
        val targetProfile = currentProfiles.find { it.id == profileId } ?: return
        targetProfile.name = cleanName

        saveAndApplyProfileConfig(profileId, targetProfile)
    }

    private fun updateActiveProfileConfig(action: (FloatingButtonConfig, OverlayProfile) -> Unit) {
        val currentState = _state.value.floatingButtonsState
        val selId = currentState.selectedProfileId
        val profile = currentState.profiles.find { it.id == selId } ?: getLatestProfile(selId)
        action(profile.config, profile)
        saveAndApplyProfileConfig(selId, profile)
    }

    fun refreshPermissions(context: Context? = null) {
        val ctx = context ?: getApplication()
        checkFloatingPermissions(ctx)
    }

    fun checkFloatingPermissions(context: Context) {
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasUsageStats = checkUsageStatsPermission(context)
        val isServiceRunning = isServiceRunning(context, "ru.doGood.Lynk.service.ForegroundOverlayService")

        _state.update {
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(
                    isOverlayPermissionGranted = hasOverlay,
                    isUsageStatsPermissionGranted = hasUsageStats,
                    isServiceRunning = isServiceRunning,
                    availableApps = _state.value.installerState.installedApps
                )
            )
        }
    }

    private fun checkUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun isServiceRunning(context: Context, serviceClass: String): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager ?: return false
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass == service.service.className) {
                return true
            }
        }
        return false
    }

    fun toggleOverlayService(context: Context) {
        val serviceClass = Class.forName("ru.doGood.Lynk.service.ForegroundOverlayService")
        val intent = Intent(context, serviceClass)
        if (_state.value.floatingButtonsState.isServiceRunning) {
            context.stopService(intent)
        } else {
            loadFloatingConfig()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        checkFloatingPermissions(context)
    }

    fun toggleQuickLaunchEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isQuickLaunchEnabled = enabled }
    }

    fun toggleFullscreenOverlayEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isFullscreenOverlayEnabled = enabled }
    }

    fun toggleHomeNavigatorEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isHomeNavigatorEnabled = enabled }
    }

    fun toggleBackNavigatorEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isBackNavigatorEnabled = enabled }
    }

    fun toggleRefreshNavigatorEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isRefreshNavigatorEnabled = enabled }
    }

    fun toggleFreeformWindowEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isFreeformWindowEnabled = enabled }
    }

    fun toggleSeparateButtonsEnabled(enabled: Boolean) {
        updateActiveProfileConfig { config, profile ->
            config.isSeparateButtonsEnabled = enabled
            profile.isSeparateButtons = enabled
        }
    }

    fun setQuickLaunchSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.quickLaunchButtonSize = size }
    }

    fun setQuickLaunchOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.quickLaunchOpacityPercent = percent }
    }

    fun setFullscreenSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.fullscreenButtonSize = size }
    }

    fun setFullscreenOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.fullscreenOpacityPercent = percent }
    }

    fun setHomeSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.homeButtonSize = size }
    }

    fun setHomeOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.homeOpacityPercent = percent }
    }

    fun setBackSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.backButtonSize = size }
    }

    fun setBackOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.backOpacityPercent = percent }
    }

    fun setRefreshSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.refreshButtonSize = size }
    }

    fun setRefreshOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.refreshOpacityPercent = percent }
    }

    fun setFreeformSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.freeformButtonSize = size }
    }

    fun setFreeformOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.freeformOpacityPercent = percent }
    }

    fun setQuickLaunchColorHex(colorHex: String) {
        updateActiveProfileConfig { config, _ -> config.quickLaunchColorHex = colorHex }
    }

    fun setQuickLaunchShape(shape: String) {
        updateActiveProfileConfig { config, _ -> config.quickLaunchShape = shape }
    }

    fun setFullscreenColorHex(colorHex: String) {
        updateActiveProfileConfig { config, _ -> config.fullscreenColorHex = colorHex }
    }

    fun setFullscreenShape(shape: String) {
        updateActiveProfileConfig { config, _ -> config.fullscreenShape = shape }
    }

    fun setHomeColorHex(colorHex: String) {
        updateActiveProfileConfig { config, _ -> config.homeColorHex = colorHex }
    }

    fun setHomeShape(shape: String) {
        updateActiveProfileConfig { config, _ -> config.homeShape = shape }
    }

    fun setBackColorHex(colorHex: String) {
        updateActiveProfileConfig { config, _ -> config.backColorHex = colorHex }
    }

    fun setBackShape(shape: String) {
        updateActiveProfileConfig { config, _ -> config.backShape = shape }
    }

    fun setRefreshColorHex(colorHex: String) {
        updateActiveProfileConfig { config, _ -> config.refreshColorHex = colorHex }
    }

    fun setRefreshShape(shape: String) {
        updateActiveProfileConfig { config, _ -> config.refreshShape = shape }
    }

    fun setFreeformColorHex(colorHex: String) {
        updateActiveProfileConfig { config, _ -> config.freeformColorHex = colorHex }
    }

    fun setFreeformShape(shape: String) {
        updateActiveProfileConfig { config, _ -> config.freeformShape = shape }
    }

    fun setCombinedSize(size: Int) {
        updateActiveProfileConfig { config, _ -> config.combinedButtonSize = size }
    }

    fun setOpacityPercent(percent: Int) {
        updateActiveProfileConfig { config, _ -> config.opacityPercent = percent }
    }

    fun toggleSecondaryMirroring(enabled: Boolean) {
        updateActiveProfileConfig { config, _ -> config.isSecondaryDisplayMirroring = enabled }
    }

    fun addQuickLaunchApp(packageName: String) {
        updateActiveProfileConfig { config, _ ->
            config.addQuickLaunchApp(packageName)
        }
    }

    fun removeQuickLaunchApp(packageName: String) {
        updateActiveProfileConfig { config, _ ->
            config.removeQuickLaunchApp(packageName)
        }
    }

    fun setQuickLaunchApps(apps: List<String>) {
        updateActiveProfileConfig { config, _ ->
            config.quickLaunchApps = ArrayList(apps)
            config.syncActionsWithApps()
        }
    }

    fun addFullscreenApp(packageName: String) {
        updateActiveProfileConfig { config, _ ->
            config.addFullscreenApp(packageName)
        }
    }

    fun removeFullscreenApp(packageName: String) {
        updateActiveProfileConfig { config, _ ->
            config.removeFullscreenApp(packageName)
        }
    }

    fun setFullscreenApps(apps: List<String>) {
        updateActiveProfileConfig { config, _ ->
            config.fullscreenApps = ArrayList(apps)
        }
    }

    fun toggleActionEnabled(actionId: String) {
        updateActiveProfileConfig { config, _ ->
            val action = config.actions.find { it.id == actionId }
            action?.toggleEnabled()
            if (action != null) {
                when (action.actionType) {
                    FloatingButtonAction.ActionType.QUICK_LAUNCH -> config.isQuickLaunchEnabled = action.isEnabled
                    FloatingButtonAction.ActionType.FULLSCREEN_TOGGLE -> config.isFullscreenOverlayEnabled = action.isEnabled
                    FloatingButtonAction.ActionType.HOME -> config.isHomeNavigatorEnabled = action.isEnabled
                    FloatingButtonAction.ActionType.BACK -> config.isBackNavigatorEnabled = action.isEnabled
                    FloatingButtonAction.ActionType.REFRESH -> config.isRefreshNavigatorEnabled = action.isEnabled
                    FloatingButtonAction.ActionType.FREEFORM_WINDOW -> config.isFreeformWindowEnabled = action.isEnabled
                    else -> {}
                }
            }
        }
    }
}
