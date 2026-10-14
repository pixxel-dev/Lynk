package ru.doGood.Lynk.di

import android.content.Context
import android.content.SharedPreferences
import com.example.lynk.core.domain.system.AdbWirelessManager
import com.example.lynk.core.domain.update.AppUpdateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.doGood.Lynk.R
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("lynk_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager {
        val currentVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        val repoPath = try {
            context.getString(R.string.github_repo_path)
        } catch (e: Exception) {
            "pixxel-dev/Lynk"
        }
        val token = try {
            context.getString(R.string.github_access_token)
        } catch (e: Exception) {
            ""
        }
        return AppUpdateManager(currentVersion, repoPath, token)
    }

    @Provides
    @Singleton
    fun provideAdbWirelessManager(): AdbWirelessManager {
        return AdbWirelessManager()
    }
}
