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
    val inlineInstallState: InlineInstallState? = null
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
    val logsText: String = ""
)

data class FloatingButtonsState(
    val config: FloatingButtonConfig = FloatingButtonConfig(),
    val isOverlayPermissionGranted: Boolean = false,
    val isUsageStatsPermissionGranted: Boolean = false,
    val isServiceRunning: Boolean = false,
    val availableApps: List<AppItem> = emptyList()
)

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
        loadInstalledApps()
        loadFloatingConfig()
        checkForUpdates()
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
            downloader.downloadFile(url, "Lynk-update.apk") { _ ->
                _state.update {
                    it.copy(
                        updateState = it.updateState.copy(
                            isDownloading = false,
                            isDownloaded = true,
                            statusMessage = "Загрузка завершена. Запуск установки..."
                        )
                    )
                }
            }
            
            _state.update {
                it.copy(
                    updateState = it.updateState.copy(
                        statusMessage = "Выполняется скачивание пакета..."
                    )
                )
            }
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
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return@launch

            val filesList = mutableListOf<FileItem>()

            // Parent folder navigation ".." removed because TopAppBar handles it

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

            var freeBytes = 0L
            var totalBytes = 0L
            try {
                val stat = StatFs(dir.absolutePath)
                freeBytes = stat.availableBytes
                totalBytes = stat.totalBytes
            } catch (ignored: Exception) { }

            withContext(Dispatchers.Main) {
                _state.update {
                    val currentFm = it.fileManagerState
                    val newFm = currentFm.copy(
                        currentPath = dir.absolutePath,
                        rawFiles = filesList,
                        freeSpaceBytes = freeBytes,
                        totalSpaceBytes = totalBytes,
                        selectedFiles = emptySet()
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
        
        val parent = File(currentPath).parentFile
        if (parent?.exists() == true) {
            loadDirectory(parent.absolutePath)
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

    // --- System Info & Logger ---
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

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod: Method = clazz.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }

    // --- Floating Buttons & Overlay Tweaks ---
    fun getLatestFloatingConfig(): FloatingButtonConfig {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)

        val config = FloatingButtonConfig()
        config.isQuickLaunchEnabled = prefs.getBoolean("quick_launch_enabled", false)
        config.quickLaunchApps = ArrayList(prefs.getStringSet("quick_launch_apps", emptySet()) ?: emptySet())
        config.quickLaunchButtonSize = prefs.getInt("ql_button_size", 48)
        config.quickLaunchOpacityPercent = prefs.getInt("ql_opacity_percent", 85)

        config.isFullscreenOverlayEnabled = prefs.getBoolean("fullscreen_overlay_enabled", false)
        config.fullscreenApps = ArrayList(prefs.getStringSet("fullscreen_apps", emptySet()) ?: emptySet())
        config.fullscreenButtonSize = prefs.getInt("fs_button_size", 48)
        config.fullscreenOpacityPercent = prefs.getInt("fs_opacity_percent", 85)

        config.isHomeNavigatorEnabled = prefs.getBoolean("home_navigator_enabled", false)
        config.homeButtonSize = prefs.getInt("home_button_size", 48)
        config.homeOpacityPercent = prefs.getInt("home_opacity_percent", 85)

        config.isBackNavigatorEnabled = prefs.getBoolean("back_navigator_enabled", false)
        config.backButtonSize = prefs.getInt("back_button_size", 48)
        config.backOpacityPercent = prefs.getInt("back_opacity_percent", 85)

        config.isRefreshNavigatorEnabled = prefs.getBoolean("refresh_navigator_enabled", false)
        config.refreshButtonSize = prefs.getInt("refresh_button_size", 48)
        config.refreshOpacityPercent = prefs.getInt("refresh_opacity_percent", 85)

        config.isFreeformWindowEnabled = prefs.getBoolean("freeform_window_enabled", false)
        config.freeformButtonSize = prefs.getInt("freeform_button_size", 48)
        config.freeformOpacityPercent = prefs.getInt("freeform_opacity_percent", 85)

        config.isSeparateButtonsEnabled = prefs.getBoolean("separate_buttons_enabled", false)
        config.combinedButtonSize = prefs.getInt("combined_button_size", 48)
        config.isSecondaryDisplayMirroring = prefs.getBoolean("secondary_display_mirroring", true)
        config.opacityPercent = prefs.getInt("opacity_percent", 85)

        config.buttonColor = prefs.getString("button_color", "#7C4DFF") ?: "#7C4DFF"
        config.shape = prefs.getString("button_shape", "CIRCLE") ?: "CIRCLE"

        config.quickLaunchColorHex = prefs.getString("ql_color_hex", "#6750A4") ?: "#6750A4"
        config.quickLaunchShape = prefs.getString("ql_shape", "CIRCLE") ?: "CIRCLE"

        config.fullscreenColorHex = prefs.getString("fs_color_hex", "#1976D2") ?: "#1976D2"
        config.fullscreenShape = prefs.getString("fs_shape", "CIRCLE") ?: "CIRCLE"

        config.homeColorHex = prefs.getString("home_color_hex", "#388E3C") ?: "#388E3C"
        config.homeShape = prefs.getString("home_shape", "CIRCLE") ?: "CIRCLE"

        config.backColorHex = prefs.getString("back_color_hex", "#D32F2F") ?: "#D32F2F"
        config.backShape = prefs.getString("back_shape", "CIRCLE") ?: "CIRCLE"

        config.refreshColorHex = prefs.getString("refresh_color_hex", "#FFA000") ?: "#FFA000"
        config.refreshShape = prefs.getString("refresh_shape", "CIRCLE") ?: "CIRCLE"

        config.freeformColorHex = prefs.getString("freeform_color_hex", "#00897B") ?: "#00897B"
        config.freeformShape = prefs.getString("freeform_shape", "CIRCLE") ?: "CIRCLE"

        config.syncActionsWithApps()

        return config
    }

    fun loadFloatingConfig() {
        val config = getLatestFloatingConfig()
        _state.update {
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(
                    config = config,
                    availableApps = it.installerState.installedApps
                )
            )
        }
    }

    private fun saveAndApplyFloatingConfig(newConfig: FloatingButtonConfig) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("${app.packageName}_preferences", Context.MODE_PRIVATE)

        prefs.edit()
            .putBoolean("quick_launch_enabled", newConfig.isQuickLaunchEnabled)
            .putStringSet("quick_launch_apps", newConfig.quickLaunchApps.toSet())
            .putInt("ql_button_size", newConfig.quickLaunchButtonSize)
            .putInt("ql_opacity_percent", newConfig.quickLaunchOpacityPercent)

            .putBoolean("fullscreen_overlay_enabled", newConfig.isFullscreenOverlayEnabled)
            .putStringSet("fullscreen_apps", newConfig.fullscreenApps.toSet())
            .putInt("fs_button_size", newConfig.fullscreenButtonSize)
            .putInt("fs_opacity_percent", newConfig.fullscreenOpacityPercent)

            .putBoolean("home_navigator_enabled", newConfig.isHomeNavigatorEnabled)
            .putInt("home_button_size", newConfig.homeButtonSize)
            .putInt("home_opacity_percent", newConfig.homeOpacityPercent)

            .putBoolean("back_navigator_enabled", newConfig.isBackNavigatorEnabled)
            .putInt("back_button_size", newConfig.backButtonSize)
            .putInt("back_opacity_percent", newConfig.backOpacityPercent)

            .putBoolean("refresh_navigator_enabled", newConfig.isRefreshNavigatorEnabled)
            .putInt("refresh_button_size", newConfig.refreshButtonSize)
            .putInt("refresh_opacity_percent", newConfig.refreshOpacityPercent)

            .putBoolean("freeform_window_enabled", newConfig.isFreeformWindowEnabled)
            .putInt("freeform_button_size", newConfig.freeformButtonSize)
            .putInt("freeform_opacity_percent", newConfig.freeformOpacityPercent)

            .putBoolean("separate_buttons_enabled", newConfig.isSeparateButtonsEnabled)
            .putInt("combined_button_size", newConfig.combinedButtonSize)
            .putBoolean("secondary_display_mirroring", newConfig.isSecondaryDisplayMirroring)
            .putInt("opacity_percent", newConfig.opacityPercent)
            .putString("button_color", newConfig.buttonColor)
            .putString("button_shape", newConfig.shape)

            .putString("ql_color_hex", newConfig.quickLaunchColorHex)
            .putString("ql_shape", newConfig.quickLaunchShape)
            .putString("fs_color_hex", newConfig.fullscreenColorHex)
            .putString("fs_shape", newConfig.fullscreenShape)
            .putString("home_color_hex", newConfig.homeColorHex)
            .putString("home_shape", newConfig.homeShape)
            .putString("back_color_hex", newConfig.backColorHex)
            .putString("back_shape", newConfig.backShape)
            .putString("refresh_color_hex", newConfig.refreshColorHex)
            .putString("refresh_shape", newConfig.refreshShape)
            .putString("freeform_color_hex", newConfig.freeformColorHex)
            .putString("freeform_shape", newConfig.freeformShape)
            .apply()

        val configCopy = newConfig.copy()

        _state.update {
            it.copy(
                floatingButtonsState = it.floatingButtonsState.copy(config = configCopy)
            )
        }

        val intent = Intent("ru.doGood.Lynk.ACTION_UPDATE_OVERLAY")
        intent.setPackage(app.packageName)
        app.sendBroadcast(intent)
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
        val config = getLatestFloatingConfig()
        config.isQuickLaunchEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleFullscreenOverlayEnabled(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isFullscreenOverlayEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleHomeNavigatorEnabled(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isHomeNavigatorEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleBackNavigatorEnabled(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isBackNavigatorEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleRefreshNavigatorEnabled(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isRefreshNavigatorEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleFreeformWindowEnabled(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isFreeformWindowEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleSeparateButtonsEnabled(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isSeparateButtonsEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.quickLaunchButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.quickLaunchOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.fullscreenButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.fullscreenOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setHomeSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.homeButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setHomeOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.homeOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setBackSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.backButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setBackOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.backOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setRefreshSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.refreshButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setRefreshOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.refreshOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setFreeformSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.freeformButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setFreeformOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.freeformOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchColorHex(colorHex: String) {
        val config = getLatestFloatingConfig()
        config.quickLaunchColorHex = colorHex
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchShape(shape: String) {
        val config = getLatestFloatingConfig()
        config.quickLaunchShape = shape
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenColorHex(colorHex: String) {
        val config = getLatestFloatingConfig()
        config.fullscreenColorHex = colorHex
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenShape(shape: String) {
        val config = getLatestFloatingConfig()
        config.fullscreenShape = shape
        saveAndApplyFloatingConfig(config)
    }

    fun setHomeColorHex(colorHex: String) {
        val config = getLatestFloatingConfig()
        config.homeColorHex = colorHex
        saveAndApplyFloatingConfig(config)
    }

    fun setHomeShape(shape: String) {
        val config = getLatestFloatingConfig()
        config.homeShape = shape
        saveAndApplyFloatingConfig(config)
    }

    fun setBackColorHex(colorHex: String) {
        val config = getLatestFloatingConfig()
        config.backColorHex = colorHex
        saveAndApplyFloatingConfig(config)
    }

    fun setBackShape(shape: String) {
        val config = getLatestFloatingConfig()
        config.backShape = shape
        saveAndApplyFloatingConfig(config)
    }

    fun setRefreshColorHex(colorHex: String) {
        val config = getLatestFloatingConfig()
        config.refreshColorHex = colorHex
        saveAndApplyFloatingConfig(config)
    }

    fun setRefreshShape(shape: String) {
        val config = getLatestFloatingConfig()
        config.refreshShape = shape
        saveAndApplyFloatingConfig(config)
    }

    fun setFreeformColorHex(colorHex: String) {
        val config = getLatestFloatingConfig()
        config.freeformColorHex = colorHex
        saveAndApplyFloatingConfig(config)
    }

    fun setFreeformShape(shape: String) {
        val config = getLatestFloatingConfig()
        config.freeformShape = shape
        saveAndApplyFloatingConfig(config)
    }

    fun setCombinedSize(size: Int) {
        val config = getLatestFloatingConfig()
        config.combinedButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setOpacityPercent(percent: Int) {
        val config = getLatestFloatingConfig()
        config.opacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun toggleSecondaryMirroring(enabled: Boolean) {
        val config = getLatestFloatingConfig()
        config.isSecondaryDisplayMirroring = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun addQuickLaunchApp(packageName: String) {
        val config = getLatestFloatingConfig()
        config.addQuickLaunchApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun removeQuickLaunchApp(packageName: String) {
        val config = getLatestFloatingConfig()
        config.removeQuickLaunchApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchApps(apps: List<String>) {
        val config = getLatestFloatingConfig()
        config.quickLaunchApps = ArrayList(apps)
        saveAndApplyFloatingConfig(config)
    }

    fun addFullscreenApp(packageName: String) {
        val config = getLatestFloatingConfig()
        config.addFullscreenApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun removeFullscreenApp(packageName: String) {
        val config = getLatestFloatingConfig()
        config.removeFullscreenApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenApps(apps: List<String>) {
        val config = getLatestFloatingConfig()
        config.fullscreenApps = ArrayList(apps)
        saveAndApplyFloatingConfig(config)
    }

    fun toggleActionEnabled(actionId: String) {
        val config = getLatestFloatingConfig()
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
        saveAndApplyFloatingConfig(config)
    }
}
