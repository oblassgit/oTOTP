package org.olidev.ototp.`import`

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.olidev.ototp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSourcePickerScreen(
    viewModel: ImportViewModel,
    onNavigateBack: () -> Unit,
    onTokensParsed: () -> Unit,
    onScanGoogleAuth: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                viewModel.handleAction(Import.Action.ParseFile(inputStream))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.handleAction(Import.Action.Reset)
    }

    LaunchedEffect(state) {
        if (state is Import.State.TokensReady) {
            onTokensParsed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Tokens") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val currentState = state) {
            is Import.State.Idle -> {
                ImportSourceList(
                    modifier = Modifier.padding(innerPadding),
                    onSelectTwoFas = {
                        filePicker.launch(arrayOf("application/*"))
                    },
                    onSelectGoogleAuth = onScanGoogleAuth,
                    onSelectOtotpBackup = {
                        filePicker.launch(arrayOf("application/*"))
                    }
                )
            }
            is Import.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is Import.State.Error -> {
                ImportErrorScreen(
                    message = currentState.message,
                    modifier = Modifier.padding(innerPadding),
                    onDismiss = { viewModel.handleAction(Import.Action.DismissError) }
                )
            }
            else -> Unit // TokensReady and Success handled by review screen
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportTokensReviewScreen(
    viewModel: ImportViewModel,
    onNavigateBack: () -> Unit,
    onImportSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Tokens") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val currentState = state) {
            is Import.State.TokensReady -> {
                ImportTokensContent(
                    tokens = currentState.tokens,
                    modifier = Modifier.padding(innerPadding),
                    onToggle = { index -> viewModel.handleAction(Import.Action.ToggleToken(index)) },
                    onSelectAll = { viewModel.handleAction(Import.Action.SelectAll) },
                    onDeselectAll = { viewModel.handleAction(Import.Action.DeselectAll) },
                    onConfirm = { viewModel.handleAction(Import.Action.ConfirmImport) }
                )
            }
            is Import.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is Import.State.Success -> {
                ImportSuccessScreen(
                    modifier = Modifier.padding(innerPadding),
                    onDone = onImportSuccess
                )
            }
            else -> Unit // Idle and Error handled by source picker screen
        }
    }
}

@Composable
private fun ImportSourceList(
    modifier: Modifier = Modifier,
    onSelectTwoFas: () -> Unit,
    onSelectGoogleAuth: () -> Unit,
    onSelectOtotpBackup: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select an app to import from",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        OutlinedCard(
            onClick = onSelectOtotpBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SaveAlt,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "oTOTP Backup",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Restore from an oTOTP backup file (.ototp)\nSettings > Export Tokens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        OutlinedCard(
            onClick = onSelectTwoFas,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.import_service_2fas),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "2FAS",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Import from a 2FAS backup file (.2fas) \nSettings > 2FAS Backup > Export",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        OutlinedCard(
            onClick = onSelectGoogleAuth,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.import_service_google),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Google Authenticator",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Scan the export QR code\nTransfer accounts > Export accounts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportTokensContent(
    tokens: List<Import.SelectableToken>,
    modifier: Modifier = Modifier,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onConfirm: () -> Unit
) {
    val selectedCount = tokens.count { it.isSelected }
    val allSelected = tokens.all { it.isSelected }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount of ${tokens.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = if (allSelected) onDeselectAll else onSelectAll) {
                Text(if (allSelected) "Deselect All" else "Select All")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 4.dp
            )
        ) {
            itemsIndexed(tokens) { index, selectable ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = selectable.isSelected,
                            onCheckedChange = { onToggle(index) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectable.token.issuer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val account = selectable.token.account
                            if (!account.isNullOrBlank()) {
                                Text(
                                    text = account,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onConfirm,
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Import $selectedCount Token${if (selectedCount != 1) "s" else ""}")
        }
    }
}

@Composable
private fun ImportErrorScreen(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Import Failed",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDismiss) {
            Text("Try Again")
        }
    }
}

@Composable
private fun ImportSuccessScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tokens Imported!",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your tokens have been imported successfully.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) {
            Text("Done")
        }
    }
}
