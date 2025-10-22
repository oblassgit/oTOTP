package com.example.ototp.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ototp.activity.MyViewModel
import com.example.ototp.TOTPUtil
import com.example.ototp.db.TOTPTokenEntity
import kotlinx.coroutines.delay

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
) {
    val tokens by viewModel.tokens.collectAsState()

    // Timer state

    // Tick every second, update both secondsLeft and tick (for TOTP refresh)


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("oTOTP") },
                actions = {
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
                    var secondsLeft by remember { mutableStateOf(0L) }
                    var tick by remember { mutableStateOf(0L) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            val now = System.currentTimeMillis() / 1000
                            secondsLeft = token.period - (now % token.period)
                            tick = now / token.period
                            delay(1000)
                        }
                    }
                    val secret = viewModel.getSecret(token.id) ?: ""
                    val totp = remember(secret, tick) {
                        TOTPUtil.generateTOTPBase32(
                            secret = secret,
                            digits = token.digits,
                            period = token.period
                        )
                    }
                    TOTPItem(
                        totp,
                        token.label,
                        token.issuer,
                        token.period,
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
    totalSeconds: Int,
    secondsLeft: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmationDialogShown by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
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
                    dropdownExpanded = true
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
            ) {

                Text(
                    text = issuer,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = account ?: ""
                )
                Text(
                    text = token,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (secondsLeft <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                )
            }

            ExpressiveCircularCountdown(totalSeconds, secondsLeft)
        }

        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onEdit()
                    dropdownExpanded = false
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    confirmationDialogShown = true
                    dropdownExpanded = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    clipboardManager.setText(AnnotatedString(token))
                    dropdownExpanded = false
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            )
        }
    }
    if (confirmationDialogShown) {
        ConfirmDeletionDialog(
            onConfirm = {
                onDelete()
                confirmationDialogShown = false
            },
            onDismiss = {
                confirmationDialogShown = false
            },
            issuer = issuer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeletionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    issuer: String,
) {
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        },
        icon = {
            Icon(Icons.Default.Delete, "")
        },
        title = {
            Text(text = "Delete $issuer?")
        },
        text = {
            Text("Without this token you might not be able to log in to your $issuer account. Do you really want to proceed?")
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveCircularCountdown(
    totalSeconds: Int = 30,
    secondsLeft: Long,
) {
    var smoothProgress by remember { mutableStateOf(secondsLeft / totalSeconds.toFloat()) }

    LaunchedEffect(secondsLeft) {
        val start = secondsLeft / totalSeconds.toFloat()
        val end = (secondsLeft - 1) / totalSeconds.toFloat()
        val duration = 1000L // ms in one second

        val frameRate = 60 // 60 frames per second
        val frameDelay = 1000L / frameRate

        val startTime = System.currentTimeMillis()
        var elapsed: Long

        do {
            elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
            smoothProgress = start + (end - start) * fraction
            delay(frameDelay)
        } while (elapsed < duration)
        smoothProgress = end
    }

    Box(contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator(
            progress = { smoothProgress.coerceIn(0f, 1f) }
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