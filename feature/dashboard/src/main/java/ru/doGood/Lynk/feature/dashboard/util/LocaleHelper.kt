package ru.doGood.Lynk.feature.dashboard.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import ru.doGood.Lynk.feature.dashboard.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                localeManager?.applicationLocales = LocaleList(locale)
            } catch (_: Exception) {}
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        return context.createConfigurationContext(config)
    }
}
