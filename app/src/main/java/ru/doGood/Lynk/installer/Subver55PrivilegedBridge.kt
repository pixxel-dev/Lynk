package ru.doGood.Lynk.installer

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.view.Display
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import com.example.lynk.core.domain.installer.WaterfallInstallStrategy
import top.canyie.pine.Pine
import top.canyie.pine.PineConfig
import top.canyie.pine.callback.MethodHook
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Subver55PrivilegedBridge @Inject constructor(
    @ApplicationContext private val context: Context
) : WaterfallInstallStrategy.PrivilegedBridge {

    companion object {
        private const val TAG = "Subver55Bridge"
        private const val FLYME_DRIVER_USER_ID = 10
        private const val OP_REQUEST_INSTALL_PACKAGES = 66
    }

    @Volatile
    private var pineInitialized = false

    private val _shizukuReadyState = MutableStateFlow(false)
    val shizukuReadyState: StateFlow<Boolean> = _shizukuReadyState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _shizukuReadyState.value = isShizukuReady()
        Log.i(TAG, "Shizuku Binder connected (Head Unit awake)")
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _shizukuReadyState.value = false
        Log.w(TAG, "Shizuku Binder lost (Head Unit entered STR sleep)")
    }

    init {
        try {
            ensurePineAndHiddenApiInitialized()
            runCatching {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Subver55PrivilegedBridge init crash", e)
        }
    }

    override fun ensurePineAndHiddenApiInitialized() {
        if (pineInitialized) return
        synchronized(this) {
            if (pineInitialized) return
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")

                PineConfig.debug = false
                PineConfig.debuggable = false
                Pine.ensureInitialized()

                val appOpsClass = Class.forName("android.app.AppOpsManager")
                val checkOpMethod = appOpsClass.getDeclaredMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                Pine.hook(checkOpMethod, object : MethodHook() {
                    override fun afterCall(callFrame: Pine.CallFrame) {
                        val op = callFrame.args[0] as? Int ?: return
                        if (op == OP_REQUEST_INSTALL_PACKAGES) {
                            callFrame.result = 0 // MODE_ALLOWED
                        }
                    }
                })

                pineInitialized = true
                Log.i(TAG, "Pine & HiddenApiBypass successfully initialized")
            } catch (e: Throwable) {
                Log.w(TAG, "Pine init warning: ${e.message}")
            }
        }
    }

    override fun isShizukuReady(): Boolean {
        val ready = runCatching {
            Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        _shizukuReadyState.value = ready
        return ready
    }

    override fun startShizukuProcess(command: String): java.lang.Process {
        val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
        return newProcessMethod.invoke(
            null,
            arrayOf("sh", "-c", command),
            null,
            null
        ) as java.lang.Process
    }

    override fun startLocalAdbOrShellProcess(command: String): java.lang.Process {
        return Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
    }

    override fun resolveActiveDriverUserId(): Int {
        runCatching {
            val getCurrentUser = ActivityManager::class.java
                .getDeclaredMethod("getCurrentUser")
                .apply { isAccessible = true }
            val current = getCurrentUser.invoke(null) as Int
            if (current > 0) return current
        }
        val processUser = Process.myUid() / 100000
        return if (processUser > 0) processUser else FLYME_DRIVER_USER_ID
    }

    override fun launchSystemInstallerAsUser(apkFile: File, userId: Int, displayId: Int): Boolean {
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

            val targetDisplay = if (displayId > 0) displayId else Display.DEFAULT_DISPLAY
            val optionsBundle: Bundle = ActivityOptions.makeBasic()
                .setLaunchDisplayId(targetDisplay)
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
        }.getOrElse { err ->
            Log.e(TAG, "Fallback startActivityAsUser failed", err)
            runCatching {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(fallbackIntent)
                true
            }.getOrDefault(false)
        }
    }
}