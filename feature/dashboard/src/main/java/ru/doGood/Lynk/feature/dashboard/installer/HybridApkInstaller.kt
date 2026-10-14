package ru.doGood.Lynk.feature.dashboard.installer

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.view.Display
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors

class HybridApkInstaller(context: Context) {
    private val context = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface InstallCallback {
        fun onProgress(status: String)
        fun onSuccess(isSilent: Boolean)
        fun onError(error: String)
    }

    init {
        PineHookManager.init(this.context)
    }

    fun installApk(apkFile: File?, allowFallbackToIntent: Boolean, callback: InstallCallback?) {
        if (apkFile == null || !apkFile.exists()) {
            notifyError(callback, "APK файл не найден")
            return
        }
        executor.execute {
            val targetUserId = resolveTargetUserId()
            notifyProgress(callback, "Целевой профиль Flyme Auto: User \$targetUserId")
            val silentSuccess = trySilentStreamInstall(apkFile, targetUserId, callback)
            if (silentSuccess) {
                notifySuccess(callback, true)
                return@execute
            }
            if (allowFallbackToIntent) {
                notifyProgress(callback, "Переключение на системный установщик...")
                mainHandler.post { launchSystemInstallerAsUser(apkFile, targetUserId, callback) }
            } else {
                notifyError(callback, "Не удалось выполнить тихую установку (проверьте права Shizuku/ADB)")
            }
        }
    }

    private fun trySilentStreamInstall(apkFile: File, userId: Int, callback: InstallCallback?): Boolean {
        var process: java.lang.Process? = null
        return try {
            val fileSize = apkFile.length()
            notifyProgress(callback, "Тихая установка (\${fileSize / 1024 / 1024} МБ)...")
            val cmd = "pm install -r -g --user \$userId -S \$fileSize"
            process = startPrivilegedProcess(cmd)
            FileInputStream(apkFile).use { `in` ->
                process!!.outputStream.use { out ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (`in`.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\\n")
                }
            }
            val exitCode = process.waitFor()
            val resultStr = output.toString().trim()
            Log.d(TAG, "Результат pm install (code \$exitCode): \$resultStr")
            exitCode == 0 && resultStr.contains("Success")
        } catch (t: Throwable) {
            Log.w(TAG, "Тихая установка недоступна: \${t.message}")
            false
        } finally {
            process?.destroy()
        }
    }

    @Throws(Exception::class)
    private fun startPrivilegedProcess(command: String): java.lang.Process {
        try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val pingMethod = shizukuClass.getDeclaredMethod("pingBinder")
            val binderAlive = pingMethod.invoke(null) as Boolean
            if (binderAlive) {
                val newProcessMethod = shizukuClass.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                return newProcessMethod.invoke(
                    null,
                    arrayOf("sh", "-c", command),
                    null,
                    null
                ) as java.lang.Process
            }
        } catch (ignored: Throwable) {
        }
        return Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
    }

    private fun launchSystemInstallerAsUser(apkFile: File, userId: Int, callback: InstallCallback?) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            var optionsBundle: Bundle? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = Display.DEFAULT_DISPLAY
                optionsBundle = options.toBundle()
            }
            
            val userHandle = createUserHandle(userId)
            if (userHandle != null) {
                val startActivityAsUser = Context::class.java.getMethod(
                    "startActivityAsUser",
                    Intent::class.java,
                    Bundle::class.java,
                    UserHandle::class.java
                )
                startActivityAsUser.invoke(context, intent, optionsBundle, userHandle)
            } else {
                context.startActivity(intent, optionsBundle)
            }
            notifySuccess(callback, false)
        } catch (t: Throwable) {
            Log.e(TAG, "Ошибка запуска системного установщика", t)
            try {
                val apkUri: Uri = FileProvider.getUriForFile(
                    context, context.packageName + ".fileprovider", apkFile
                )
                val fallbackIntent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(fallbackIntent)
                notifySuccess(callback, false)
            } catch (e: Exception) {
                notifyError(callback, "Ошибка запуска установщика: \${e.message}")
            }
        }
    }

    fun resolveTargetUserId(): Int {
        try {
            val getCurrentUser = ActivityManager::class.java.getDeclaredMethod("getCurrentUser")
            getCurrentUser.isAccessible = true
            val currentUser = getCurrentUser.invoke(null) as Int
            if (currentUser > 0) return currentUser
        } catch (ignored: Throwable) {
        }
        val myUserId = Process.myUid() / 100000
        if (myUserId > 0) return myUserId
        return FLYME_DRIVER_USER_ID
    }

    private fun createUserHandle(userId: Int): UserHandle? {
        return try {
            val constructor = UserHandle::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
            constructor.isAccessible = true
            constructor.newInstance(userId)
        } catch (t: Throwable) {
            null
        }
    }

    private fun notifyProgress(cb: InstallCallback?, msg: String) {
        cb?.let { mainHandler.post { it.onProgress(msg) } }
    }

    private fun notifySuccess(cb: InstallCallback?, silent: Boolean) {
        cb?.let { mainHandler.post { it.onSuccess(silent) } }
    }

    private fun notifyError(cb: InstallCallback?, err: String) {
        cb?.let { mainHandler.post { it.onError(err) } }
    }

    companion object {
        private const val TAG = "HybridApkInstaller"
        private const val FLYME_DRIVER_USER_ID = 10
    }
}