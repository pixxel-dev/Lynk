package ru.doGood.Lynk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.doGood.Lynk.feature.dashboard.installer.PineHookManager

@HiltAndroidApp
class LynkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PineHookManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("LynkApp", "Pine Hook Crash", e)
        }
    }
}
