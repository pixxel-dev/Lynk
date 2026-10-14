package ru.doGood.Lynk.installer

import android.app.Application
import android.content.Intent
import androidx.core.app.AppComponentFactory
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.canyie.pine.Pine
import top.canyie.pine.PineConfig
import top.canyie.pine.callback.MethodHook

/**
 * Полный аналог ru.subver55.SubverAppComponentFactory.
 * Снимает ограничения Hidden API и инициализирует Pine до старта Application.onCreate().
 */
class SubverAppComponentFactory : AppComponentFactory() {

    companion object {
        private const val OP_REQUEST_INSTALL_PACKAGES = 66

        @Volatile
        var isInitialized = false
            private set

        fun ensureInitialized() {
            if (isInitialized) return
            synchronized(this) {
                if (isInitialized) return
                runCatching {
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
                                callFrame.result = 0 // AppOpsManager.MODE_ALLOWED
                            }
                        }
                    })
                    isInitialized = true
                }
            }
        }
    }

    override fun instantiateApplicationCompat(cl: ClassLoader, className: String): Application {
        ensureInitialized()
        return super.instantiateApplicationCompat(cl, className)
    }

    override fun instantiateActivityCompat(
        cl: ClassLoader,
        className: String,
        intent: Intent?
    ) = super.instantiateActivityCompat(cl, className, intent).also {
        ensureInitialized()
    }
}