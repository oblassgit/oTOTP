package org.olidev.ototp.activity

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.material3.Surface
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import org.olidev.ototp.TOTPUtil
import org.olidev.ototp.compose.AddOrEditTokenScreen
import org.olidev.ototp.compose.MainScreen
import org.olidev.ototp.compose.QRScanner
import org.olidev.ototp.db.AppDatabase
import org.olidev.ototp.model.TokenRepository
import org.olidev.ototp.model.TotpSecretStorage
import org.olidev.ototp.settings.SettingsScreen
import org.olidev.ototp.settings.SettingsViewModel
import org.olidev.ototp.settings.ThemePreferences
import org.olidev.ototp.`import`.ImportSourcePickerScreen
import org.olidev.ototp.`import`.ImportTokensReviewScreen
import org.olidev.ototp.`import`.ImportViewModel
import org.olidev.ototp.settings.Settings
import org.olidev.ototp.ui.theme.OTOTPTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).build()

        val totpSecretStorage = TotpSecretStorage(context = this)
        val dao = db.tokenDao()
        val repository = TokenRepository(dao, totpSecretStorage)
        val viewModel = MyViewModel(repository)
        val settingsViewModel = SettingsViewModel(this)
        val importViewModel = ImportViewModel(repository)
        val themePreferences = ThemePreferences(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(org.olidev.ototp.settings.Settings.ThemeMode.SYSTEM)
            val dynamicColors by themePreferences.dynamicColorsEnabled.collectAsState(true)
            val darkTheme = when (themeMode) {
                Settings.ThemeMode.LIGHT -> false
                Settings.ThemeMode.DARK -> true
                Settings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SideEffect {
                enableEdgeToEdge(
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            OTOTPTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColors
            ) {
                Surface {
                    val navController = rememberNavController()
                NavHost(
                    navController, startDestination = "home",
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) {
                    composable("home") { MainScreen(navController, viewModel) }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToImport = { navController.navigate("import") }
                        )
                    }
                    composable("import") {
                        ImportSourcePickerScreen(
                            viewModel = importViewModel,
                            onNavigateBack = { navController.navigateUp() },
                            onTokensParsed = { navController.navigate("import/review") },
                            onScanGoogleAuth = { navController.navigate("import/scan") }
                        )
                    }
                    composable("import/scan") {
                        QRScanner(
                            onNavigateUp = { navController.navigateUp() },
                            onQrCodeScanned = { data ->
                                navController.navigateUp()
                                importViewModel.handleAction(
                                    org.olidev.ototp.`import`.Import.Action.ParseGoogleAuthQR(data)
                                )
                                navController.navigate("import/review")
                            }
                        )
                    }
                    composable("import/review") {
                        ImportTokensReviewScreen(
                            viewModel = importViewModel,
                            onNavigateBack = {
                                importViewModel.handleAction(org.olidev.ototp.`import`.Import.Action.Reset)
                                navController.navigateUp()
                            },
                            onImportSuccess = {
                                viewModel.refreshTokens()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        "add",
                        enterTransition = {
                            slideIntoContainer(
                                animationSpec = tween(300, easing = EaseIn),
                                towards = AnimatedContentTransitionScope.SlideDirection.Start
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                animationSpec = tween(300, easing = EaseOut),
                                towards = AnimatedContentTransitionScope.SlideDirection.End
                            )
                        },
                    ) {
                        AddOrEditTokenScreen(null, viewModel, {
                            viewModel.addToken(it)
                            navController.navigateUp()
                        }, {
                            navController.navigateUp()
                        })
                    }
                    composable(
                        "scan",
                        enterTransition = {
                            slideIntoContainer(
                                animationSpec = tween(300, easing = EaseIn),
                                towards = AnimatedContentTransitionScope.SlideDirection.Start
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                animationSpec = tween(300, easing = EaseOut),
                                towards = AnimatedContentTransitionScope.SlideDirection.End
                            )
                        },
                    ) {
                        QRScanner(onNavigateUp = { navController.navigateUp() }, onQrCodeScanned = {
                            navController.navigateUp()
                            viewModel.tokenDraft = TOTPUtil.parse(it)
                            navController.navigate("add")
                        })
                    }
                    composable(
                        "edit/{tokenId}",
                        arguments = listOf(navArgument("tokenId") {
                            type = NavType.LongType
                        }),
                        enterTransition = {
                            slideIntoContainer(
                                animationSpec = tween(300, easing = EaseIn),
                                towards = AnimatedContentTransitionScope.SlideDirection.Start
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                animationSpec = tween(300, easing = EaseOut),
                                towards = AnimatedContentTransitionScope.SlideDirection.End
                            )
                        },
                    ) { backStackEntry ->
                        val tokenId = backStackEntry.arguments?.getLong("tokenId")
                        if (tokenId != null) {
                            AddOrEditTokenScreen(
                                tokenId = tokenId,
                                viewModel = viewModel,
                                onSave = {
                                    viewModel.updateToken(it)
                                    navController.navigateUp()
                                },
                                onNavigateUp = {
                                    navController.navigateUp()
                                })
                        }
                    }
                }
                } // Surface
            }
        }
    }
}