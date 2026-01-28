package com.example.ototp.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.ototp.TOTPUtil
import com.example.ototp.compose.AddOrEditTokenScreen
import com.example.ototp.compose.MainScreen
import com.example.ototp.compose.QRScanner
import com.example.ototp.db.AppDatabase
import com.example.ototp.model.TokenRepository
import com.example.ototp.model.TotpSecretStorage
import com.example.ototp.ui.theme.OTOTPTheme

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

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            OTOTPTheme {
                NavHost(
                    navController, startDestination = "home",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    composable("home") { MainScreen(navController, viewModel) }
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
            }
        }
    }
}