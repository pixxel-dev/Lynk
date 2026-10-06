package ru.doGood.Lynk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (Intent.ACTION_BOOT_COMPLETED == action || "android.intent.action.LOCKED_BOOT_COMPLETED" == action) {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val qlEnabled = prefs.getBoolean("quick_launch_enabled", false)
            val fsEnabled = prefs.getBoolean("fullscreen_overlay_enabled", false)
            val homeEnabled = prefs.getBoolean("home_navigator_enabled", false)
            val backEnabled = prefs.getBoolean("back_navigator_enabled", false)
            val refreshEnabled = prefs.getBoolean("refresh_navigator_enabled", false)

            if (qlEnabled || fsEnabled || homeEnabled || backEnabled || refreshEnabled) {
                val serviceIntent = Intent(context, ForegroundOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
