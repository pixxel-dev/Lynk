package ru.doGood.Lynk.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ru.doGood.Lynk.service.ForegroundOverlayService

/**
 * Аналог ru.subver55.hudsettings.MyPackageReplacedReceiver.
 * Перехватывает обновление самого приложения и установку/удаление сторонних APK.
 */
class MyPackageReplacedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageReplacedReceiver"
        const val ACTION_PACKAGE_INSTALL_DONE = "actionPackageInstallDone"
        const val ACTION_UPDATE_PACKAGE_INFO = "actionUpdatePackageInfo"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Lynk успешно обновлён, перезапускаем ForegroundOverlayService")
                runCatching {
                    val serviceIntent = Intent(context, ForegroundOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }

            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REMOVED -> {
                val pkg = intent.data?.schemeSpecificPart.orEmpty()
                Log.i(TAG, "Событие пакета [$action]: $pkg")

                context.sendBroadcast(
                    Intent(ACTION_PACKAGE_INSTALL_DONE).apply {
                        setPackage(context.packageName)
                        putExtra(EXTRA_PACKAGE_NAME, pkg)
                    }
                )
                context.sendBroadcast(
                    Intent(ACTION_UPDATE_PACKAGE_INFO).apply {
                        setPackage(context.packageName)
                        putExtra(EXTRA_PACKAGE_NAME, pkg)
                    }
                )
            }
        }
    }
}