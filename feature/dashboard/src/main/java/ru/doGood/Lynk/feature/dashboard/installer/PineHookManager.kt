package ru.doGood.Lynk.feature.dashboard.installer

import android.content.Context
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.canyie.pine.Pine
import top.canyie.pine.PineConfig
import top.canyie.pine.callback.MethodHook

object PineHookManager {
    private const val TAG = "PineHookManager"
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        try {
            // 1. Открываем доступ ко всем скрытым методам Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.addHiddenApiExemptions("L")
            }

            // 2. Настраиваем Pine
            PineConfig.debug = false
            PineConfig.debuggable = false
            Pine.ensureInitialized()

            // 3. Хук для обхода проверки источника установки
            hookPackageInstallerChecks()
            isInitialized = true
            Log.i(TAG, "Pine и HiddenApiBypass успешно инициализированы")
        } catch (t: Throwable) {
            Log.e(TAG, "Ошибка инициализации Pine", t)
        }
    }

    private fun hookPackageInstallerChecks() {
        try {
            val appOpsClass = Class.forName("android.app.AppOpsManager")
            val checkOpMethod = appOpsClass.getDeclaredMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )

            Pine.hook(checkOpMethod, object : MethodHook() {
                override fun afterCall(callFrame: Pine.CallFrame) {
                    val op = callFrame.args[0] as Int
                    if (op == 66) { // OP_REQUEST_INSTALL_PACKAGES = 66 в AOSP
                        callFrame.result = 0 // AppOpsManager.MODE_ALLOWED
                    }
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Не удалось установить хук на AppOpsManager: \${t.message}")
        }
    }
}
