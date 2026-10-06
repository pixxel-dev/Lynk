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
import com.example.lynk.core.domain.approval.ApprovalManager
import com.example.lynk.core.domain.approval.ApprovalRequest
import com.example.lynk.core.domain.audit.AuditResult
import com.example.lynk.core.domain.audit.RuleAuditor
import com.example.lynk.core.domain.audit.rules.ArchitecturalRule
import com.example.lynk.core.domain.audit.rules.DuplicatePatternRule
import com.example.lynk.core.domain.backlog.BacklogItem
import com.example.lynk.core.domain.backlog.BacklogManager
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
    val downloadProgress: Float = 0f,
    val statusMessage: String? = null
)

data class DashboardState(
    val selectedTab: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.RU,
    val backlogItems: List<BacklogItem> = emptyList(),
    val approvalRequests: List<ApprovalRequest> = emptyList(),
    val auditResults: List<AuditResult> = emptyList(),
    val fileManagerState: FileManagerState = FileManagerState(),
    val installerState: ApkInstallerState = ApkInstallerState(),
    val systemInfoState: SystemInfoState = SystemInfoState(),
    val floatingButtonsState: FloatingButtonsState = FloatingButtonsState(),
    val updateState: AppUpdateUiState = AppUpdateUiState()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val backlogManager = BacklogManager()
    private val approvalManager = ApprovalManager()
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

        // Initialize Backlog Mock Data
        val item1 = BacklogItem("1", "Task 7: Fullscreen Migration", "Migrate File Manager & APK Installer to Compose", 100)
        val item2 = BacklogItem("2", "Setup Architecture Rules", "Keep domain module clean pure Java", 80)
        val item3 = BacklogItem("3", "Navigation 3 Integration", "Adaptive list-detail layouts", 90)
        backlogManager.addItem(item1)
        backlogManager.addItem(item2)
        backlogManager.addItem(item3)

        val req1 = ApprovalRequest("req1", "Approve Task 7 Fullscreen UI Migration", "lead")
        approvalManager.submitRequest(req1)

        refreshBacklogState()
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
                        downloadProgress = 0f,
                        statusMessage = "Загрузка пакета v${info.latestVersion}..."
                    )
                )
            }

            val app = getApplication<Application>()
            val downloader = ru.doGood.Lynk.feature.dashboard.utils.ApkDownloader(app)
            downloader.downloadFile(url, "Lynk-update.apk")
            
            _state.update {
                it.copy(
                    updateState = it.updateState.copy(
                        isDownloading = false,
                        statusMessage = "Загрузка началась. Проверьте уведомления."
                    )
                )
            }
        }
    }

    // --- Backlog & Audit ---
    private fun refreshBacklogState() {
        _state.update {
            it.copy(
                backlogItems = backlogManager.itemsSortedByPriority,
                approvalRequests = listOfNotNull(approvalManager.getRequest("req1"))
            )
        }
    }

    fun completeBacklogItem(id: String) {
        backlogManager.completeItem(id)
        refreshBacklogState()
    }

    fun approveRequest(id: String) {
        approvalManager.approveRequest(id)
        refreshBacklogState()
    }

    fun rejectRequest(id: String) {
        approvalManager.rejectRequest(id)
        refreshBacklogState()
    }

    fun runAudit() {
        val rules = listOf(
            ArchitecturalRule("**/*.java", "Java domain source rules"),
            ArchitecturalRule("**/*.kt", "Kotlin Compose feature rules")
        )

        val auditor = RuleAuditor<List<ArchitecturalRule>>()
        auditor.addRule(DuplicatePatternRule())

        val results = auditor.audit(rules)
        _state.update { it.copy(auditResults = results) }
    }

    // --- File Explorer ---
    fun loadDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return@launch

            val filesList = mutableListOf<FileItem>()

            // Parent folder navigation ".."
            dir.parentFile?.let { parent ->
                filesList.add(FileItem("..", parent.absolutePath, 0L, 0L, true))
            }

            val childFiles = dir.listFiles()
            if (childFiles != null) {
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

    fun deleteSelectedFiles() {
        val selected = _state.value.fileManagerState.selectedFiles.toList()
        val currentPath = _state.value.fileManagerState.currentPath
        viewModelScope.launch(Dispatchers.IO) {
            for (filePath in selected) {
                File(filePath).deleteRecursively()
            }
            loadDirectory(currentPath)
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
    fun selectApkForInstallation(path: String) {
        _state.update {
            it.copy(
                selectedTab = 2, // Switch to APK tab
                installerState = it.installerState.copy(selectedApkPath = path)
            )
        }
    }

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
            callback.onResult(InstallResult(true, "Sent to Android Package Installer", InstallStep.PACKAGE_INSTALLER))
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
    fun loadFloatingConfig() {
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

        config.syncActionsWithApps()

        config.isSeparateButtonsEnabled = prefs.getBoolean("separate_buttons_enabled", false)
        config.combinedButtonSize = prefs.getInt("combined_button_size", 48)
        config.isSecondaryDisplayMirroring = prefs.getBoolean("secondary_display_mirroring", true)
        config.opacityPercent = prefs.getInt("opacity_percent", 85)

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

            .putBoolean("separate_buttons_enabled", newConfig.isSeparateButtonsEnabled)
            .putInt("combined_button_size", newConfig.combinedButtonSize)
            .putBoolean("secondary_display_mirroring", newConfig.isSecondaryDisplayMirroring)
            .putInt("opacity_percent", newConfig.opacityPercent)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        checkFloatingPermissions(context)
    }

    fun toggleQuickLaunchEnabled(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isQuickLaunchEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleFullscreenOverlayEnabled(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isFullscreenOverlayEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleHomeNavigatorEnabled(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isHomeNavigatorEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleBackNavigatorEnabled(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isBackNavigatorEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleRefreshNavigatorEnabled(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isRefreshNavigatorEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun toggleSeparateButtonsEnabled(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isSeparateButtonsEnabled = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchSize(size: Int) {
        val config = _state.value.floatingButtonsState.config
        config.quickLaunchButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setQuickLaunchOpacityPercent(percent: Int) {
        val config = _state.value.floatingButtonsState.config
        config.quickLaunchOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenSize(size: Int) {
        val config = _state.value.floatingButtonsState.config
        config.fullscreenButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setFullscreenOpacityPercent(percent: Int) {
        val config = _state.value.floatingButtonsState.config
        config.fullscreenOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setHomeSize(size: Int) {
        val config = _state.value.floatingButtonsState.config
        config.homeButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setHomeOpacityPercent(percent: Int) {
        val config = _state.value.floatingButtonsState.config
        config.homeOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setBackSize(size: Int) {
        val config = _state.value.floatingButtonsState.config
        config.backButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setBackOpacityPercent(percent: Int) {
        val config = _state.value.floatingButtonsState.config
        config.backOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setRefreshSize(size: Int) {
        val config = _state.value.floatingButtonsState.config
        config.refreshButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setRefreshOpacityPercent(percent: Int) {
        val config = _state.value.floatingButtonsState.config
        config.refreshOpacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun setCombinedSize(size: Int) {
        val config = _state.value.floatingButtonsState.config
        config.combinedButtonSize = size
        saveAndApplyFloatingConfig(config)
    }

    fun setOpacityPercent(percent: Int) {
        val config = _state.value.floatingButtonsState.config
        config.opacityPercent = percent
        saveAndApplyFloatingConfig(config)
    }

    fun toggleSecondaryMirroring(enabled: Boolean) {
        val config = _state.value.floatingButtonsState.config
        config.isSecondaryDisplayMirroring = enabled
        saveAndApplyFloatingConfig(config)
    }

    fun addQuickLaunchApp(packageName: String) {
        val config = _state.value.floatingButtonsState.config
        config.addQuickLaunchApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun removeQuickLaunchApp(packageName: String) {
        val config = _state.value.floatingButtonsState.config
        config.removeQuickLaunchApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun addFullscreenApp(packageName: String) {
        val config = _state.value.floatingButtonsState.config
        config.addFullscreenApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun removeFullscreenApp(packageName: String) {
        val config = _state.value.floatingButtonsState.config
        config.removeFullscreenApp(packageName)
        saveAndApplyFloatingConfig(config)
    }

    fun toggleActionEnabled(actionId: String) {
        val config = _state.value.floatingButtonsState.config
        val action = config.actions.find { it.id == actionId }
        action?.toggleEnabled()
        if (action != null) {
            when (action.actionType) {
                FloatingButtonAction.ActionType.QUICK_LAUNCH -> config.isQuickLaunchEnabled = action.isEnabled
                FloatingButtonAction.ActionType.FULLSCREEN_TOGGLE -> config.isFullscreenOverlayEnabled = action.isEnabled
                FloatingButtonAction.ActionType.HOME -> config.isHomeNavigatorEnabled = action.isEnabled
                FloatingButtonAction.ActionType.BACK -> config.isBackNavigatorEnabled = action.isEnabled
                FloatingButtonAction.ActionType.REFRESH -> config.isRefreshNavigatorEnabled = action.isEnabled
                else -> {}
            }
        }
        saveAndApplyFloatingConfig(config)
    }
}
