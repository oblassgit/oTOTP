package com.example.ototp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.ototp.ui.theme.OTOTPTheme
import kotlinx.coroutines.delay

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
                            viewModel.tokenDraft = OTPAuthParser.parse(it)
                            navController.navigate("add")
                        })
                    }
                    composable(
                        "edit/{tokenId}",
                        arguments = listOf(navArgument("tokenId") { type = NavType.LongType }),
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
                            AddOrEditTokenScreen(tokenId = tokenId, viewModel = viewModel, onSave = {
                                viewModel.updateToken(it)
                                navController.navigateUp()
                            }, onNavigateUp = {
                                navController.navigateUp()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavController, viewModel: MyViewModel) {
    MainScreen(
        viewModel,
        { navController.navigate("add") },
        { navController.navigate("scan") },
        { viewModel.deleteToken(it) },
        onEdit = {
            navController.navigate("edit/${it.id}")
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MyViewModel,
    onManual: () -> Unit,
    onQRCode: () -> Unit,
    onDelete: (id: Long) -> Unit,
    onEdit: (token: TOTPTokenEntity) -> Unit,
    totpPeriod: Long = 30L // seconds
) {
    val tokens by viewModel.tokens.collectAsState()

    // Timer state
    var secondsLeft by remember { mutableStateOf(0L) }
    var tick by remember { mutableStateOf(0L) }

    // Tick every second, update both secondsLeft and tick (for TOTP refresh)
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis() / 1000
            secondsLeft = totpPeriod - (now % totpPeriod)
            tick = now / totpPeriod
            delay(1000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("oTOTP") },
                actions = {
                    /*Text(
                        text = "Next refresh: $secondsLeft s",
                        modifier = Modifier.padding(end = 16.dp)
                    )*/
                    ExpressiveLinearCountdown(secondsLeft = secondsLeft)
                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                items(tokens) { token ->
                    val secret = viewModel.getSecret(token.id) ?: ""
                    val totp = remember(secret, tick) {
                        TOTPUtil.generateTOTPBase32(
                            secret = secret,
                            digits = token.digits ?: 6,
                            period = token.period ?: 30
                        )
                    }
                    TOTPItem(
                        totp,
                        token.label,
                        token.issuer,
                        secondsLeft,
                        { onEdit(token) },
                        { onDelete(token.id) }
                        )
                }
            }

            SimpleFabMenu(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                onManual = { onManual() },
                onQRCode = { onQRCode() }
            )
        }
    }
}

@Composable
fun TOTPItem(
    token: String,
    account: String?,
    issuer: String,
    secondsLeft: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    clipboardManager.setText(AnnotatedString(token))
                },
                onLongClick = {
                    expanded = true
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {

            Text(
                text = issuer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = account?:""
            )
            Text(
                text = token,
                style = MaterialTheme.typography.headlineMedium,
                color = if (secondsLeft <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onEdit()
                    expanded = false
                          },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    expanded = false
                          },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    clipboardManager.setText(AnnotatedString(token))
                    expanded = false
                          },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLinearCountdown(
    totalSeconds: Long = 30,
    secondsLeft: Long,
) {
    val progress by animateFloatAsState(
        targetValue = (secondsLeft / totalSeconds.toFloat()).coerceIn(0f, 1f),
        label = "progress"
    )
    Box(
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { progress }
        )

        Text(secondsLeft.toString(), style = LocalTextStyle.current.copy())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimpleFabMenu(
    onQRCode: () -> Unit = {},
    onManual: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var fabMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                checked = fabMenuExpanded,
                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onQRCode()
            },
            icon = { Icon(Icons.Filled.QrCode, contentDescription = null) },
            text = { Text("Scan QR Code") }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onManual()
            },
            icon = { Icon(Icons.Filled.Password, contentDescription = null) },
            text = { Text("Enter manually") }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview()
@Composable
fun MainScreenPreview() {
    val secrets = listOf(
        Secret("Test", "OIJSOIFJOI")
    )
    //MainScreen(secrets, {}, {})
}

