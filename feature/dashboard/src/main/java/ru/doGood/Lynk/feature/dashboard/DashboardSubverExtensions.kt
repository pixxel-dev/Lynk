package ru.doGood.Lynk.feature.dashboard

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.view.Display
import androidx.core.content.FileProvider
import com.example.lynk.core.domain.installer.SubverPackageOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.doGood.Lynk.feature.dashboard.installer.PineHookManager
import java.io.File

/**
 * Создаёт экземпляр SubverPackageOperations, автоматически использующий Shizuku
 * (через рефлексию, без жёсткой зависимости в build.gradle) или локальный Shell/ADB.
 */
fun createSubverPackageOperations(): SubverPackageOperations {
    return SubverPackageOperations { command ->
        val shizukuProcess = runCatching {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val pingBinder = shizukuClass.getMethod("pingBinder").invoke(null) as? Boolean ?: false
            val checkSelfPermission = shizukuClass.getMethod("checkSelfPermission").invoke(null) as? Int
                ?: PackageManager.PERMISSION_DENIED

            if (pingBinder && checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
                val newProcess = shizukuClass.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                newProcess.invoke(null, arrayOf("sh", "-c", command), null, null) as? java.lang.Process
            } else {
                null
            }
        }.getOrNull()

        shizukuProcess ?: Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
    }
}

/**
 * Определяет активный профиль водителя в Flyme Auto (по умолчанию User 10).
 */
fun resolveDriverUserId(): Int {
    runCatching {
        val getCurrentUser = ActivityManager::class.java
            .getDeclaredMethod("getCurrentUser")
            .apply { isAccessible = true }
        val user = getCurrentUser.invoke(null) as Int
        if (user > 0) return user
    }
    val uidUser = Process.myUid() / 100000
    return if (uidUser > 0) uidUser else 10
}

fun CoroutineScope.subverInstallApk(
    context: Context,
    apkPath: String,
    targetUserId: Int = resolveDriverUserId(),
    onResult: (String) -> Unit
) {
    launch(Dispatchers.IO) {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            withContext(Dispatchers.Main) { onResult("Файл не найден: $apkPath") }
            return@launch
        }

        // Передаём context в PineHookManager.init(context)
        runCatching { PineHookManager.init(context) }

        val parsedPkgName = runCatching {
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)?.packageName
        }.getOrNull()

        val ops = createSubverPackageOperations()
        val silentSuccess = ops.installApkAndGrantPermissions(
            apkFile,
            parsedPkgName,
            targetUserId,
            null
        )

        if (silentSuccess) {
            withContext(Dispatchers.Main) {
                onResult("Тихая установка завершена + выданы права (User $targetUserId): ${parsedPkgName ?: apkFile.name}")
            }
            return@launch
        }

        val fallbackLaunched = launchSystemInstallerFallback(context, apkFile, targetUserId)
        withContext(Dispatchers.Main) {
            if (fallbackLaunched) {
                onResult("Открыто системное окно установки (User $targetUserId)")
            } else {
                onResult("Ошибка запуска установки APK")
            }
        }
    }
}

fun CoroutineScope.subverGivePermissions(
    packageName: String,
    targetUserId: Int = resolveDriverUserId(),
    onResult: (List<String>) -> Unit
) {
    launch(Dispatchers.IO) {
        val ops = createSubverPackageOperations()
        val logs = ops.giveMyPermissions(packageName, targetUserId)
        withContext(Dispatchers.Main) {
            onResult(logs)
        }
    }
}

fun CoroutineScope.subverForceStopApp(
    packageName: String,
    targetUserId: Int = resolveDriverUserId(),
    onResult: (String) -> Unit
) {
    launch(Dispatchers.IO) {
        val ops = createSubverPackageOperations()
        val ok = ops.forceStopPackage(packageName, targetUserId)
        withContext(Dispatchers.Main) {
            onResult(if (ok) "Остановлено: $packageName (User $targetUserId)" else "Ошибка остановки $packageName")
        }
    }
}

fun CoroutineScope.subverUninstallApp(
    packageName: String,
    targetUserId: Int = resolveDriverUserId(),
    onResult: (String) -> Unit
) {
    launch(Dispatchers.IO) {
        val ops = createSubverPackageOperations()
        val removed = ops.removePackage(packageName, targetUserId)
        withContext(Dispatchers.Main) {
            onResult(if (removed) "Удалено: $packageName (User $targetUserId)" else "Не удалось удалить $packageName")
        }
    }
}

fun openAppSystemSettings(context: Context, packageName: String) {
    runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName.trim(), null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

private fun launchSystemInstallerFallback(context: Context, apkFile: File, userId: Int): Boolean {
    return runCatching {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val optionsBundle: Bundle = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        val userHandle = UserHandle::class.java
            .getDeclaredConstructor(Int::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .newInstance(userId)

        val startActivityAsUser = Context::class.java.getMethod(
            "startActivityAsUser",
            Intent::class.java,
            Bundle::class.java,
            UserHandle::class.java
        )
        startActivityAsUser.invoke(context, intent, optionsBundle, userHandle)
        true
    }.getOrElse {
        runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
            true
        }.getOrDefault(false)
    }
}