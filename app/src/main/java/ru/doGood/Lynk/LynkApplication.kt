package ru.doGood.Lynk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.doGood.Lynk.feature.dashboard.installer.PineHookManager

@HiltAndroidApp
class LynkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PineHookManager.init(this)
    }
}
