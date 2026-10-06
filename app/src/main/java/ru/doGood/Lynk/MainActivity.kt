package ru.doGood.Lynk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.doGood.Lynk.feature.dashboard.DashboardViewModel
import ru.doGood.Lynk.feature.dashboard.ui.DashboardMainScreen
import ru.doGood.Lynk.feature.dashboard.util.LocaleHelper
import ru.doGood.Lynk.ui.theme.LynkTheme

@Serializable
object MainDashboardRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: DashboardViewModel = viewModel()
            val state by mainViewModel.state.collectAsState()

            val context = LocalContext.current
            val contextWithLocale = remember(state.appLanguage) {
                LocaleHelper.applyLanguage(context, state.appLanguage)
            }
            val configurationWithLocale = remember(state.appLanguage) {
                contextWithLocale.resources.configuration
            }

            CompositionLocalProvider(
                LocalContext provides contextWithLocale,
                LocalConfiguration provides configurationWithLocale
            ) {
                LynkTheme(themeMode = state.themeMode) {
                    val backStack = remember { mutableStateListOf<Any>(MainDashboardRoute) }

                    BackHandler(enabled = backStack.size > 1) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavDisplay(
                            backStack = backStack,
                            entryProvider = entryProvider {
                                entry<MainDashboardRoute> {
                                    DashboardMainScreen(
                                        viewModel = mainViewModel
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
