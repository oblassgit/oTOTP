package com.example.ototp.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ototp.Algorithm
import com.example.ototp.activity.MyViewModel
import com.example.ototp.TOTPToken
import com.example.ototp.TOTPUtil
import com.example.ototp.db.TOTPTokenEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditTokenScreen(
    tokenId: Long?,
    viewModel: MyViewModel,
    onSave: (TOTPToken) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Load entity from DB if editing
    val entity by if (tokenId != null) viewModel.getToken(tokenId).collectAsState(initial = null)
    else remember { mutableStateOf<TOTPTokenEntity?>(null) }

    // 2. Load secret only if editing
    var secretLoaded by remember { mutableStateOf(false) }
    var secret by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(tokenId, entity) {
        if (tokenId != null && !secretLoaded) {
            secret = viewModel.getSecret(tokenId) ?: ""
            secretLoaded = true
        }
    }

    // 3. Use draft if adding via QR, otherwise defaults
    val draft = viewModel.tokenDraft.takeIf { tokenId == null }
    val isEditMode = tokenId != null

    // 4. Initialize state from entity (edit), draft (add w/ QR), or empty (manual add)
    var account by rememberSaveable {
        mutableStateOf(
            draft?.account ?: entity?.label ?: ""
        )
    }
    var issuer by rememberSaveable {
        mutableStateOf(
            draft?.issuer ?: entity?.issuer ?: ""
        )
    }
    var algorithm by rememberSaveable {
        mutableStateOf(
            draft?.algorithm ?: entity?.algorithm ?: Algorithm.SHA1
        )
    }
    var digits by rememberSaveable {
        mutableStateOf(
            draft?.digits ?: entity?.digits ?: 6
        )
    }
    var period by rememberSaveable {
        mutableStateOf(
            draft?.period ?: entity?.period ?: 30
        )
    }
    var secretField by rememberSaveable {
        mutableStateOf(
            draft?.secret ?: secret
        )
    }
    var secretVisibility by rememberSaveable { mutableStateOf(false) }

    // 5. When entity or secret changes (edit mode), update fields if not already set
    LaunchedEffect(entity, secret) {
        if (isEditMode) {
            entity?.let { entity ->
                account = entity.label ?: ""
                issuer = entity.issuer
                algorithm = entity.algorithm
                digits = entity.digits
                period = entity.period
                secretField = secret
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Token" else "Add Token") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Clear draft on exit
                        viewModel.tokenDraft = null
                        onNavigateUp()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        enabled = issuer.isNotBlank() && secretField.isNotBlank(),
                        onClick = {
                            val token = TOTPToken(
                                id = if (isEditMode) entity?.id else null,
                                account = account,
                                secret = secretField,
                                issuer = issuer,
                                algorithm = algorithm,
                                digits = digits,
                                period = period
                            )
                            onSave(token)
                            // Clear draft after save
                            viewModel.tokenDraft = null
                        }
                    ) { Icon(Icons.Default.Check, contentDescription = "Save") }
                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val scrollState = rememberScrollState()
            if (isEditMode && entity == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = issuer,
                        onValueChange = { issuer = it },
                        label = { Text("Issuer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = secretField,
                        onValueChange = { secretField = it },
                        label = { Text("Secret") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (!secretVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { secretVisibility = !secretVisibility }) {
                                if (secretVisibility) Icon(
                                    Icons.Default.VisibilityOff,
                                    "Hide"
                                ) else Icon(Icons.Default.Visibility, "Show")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        label = { Text("Account (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AdvancedOptionsSection {

                        TokenOptionsDropdown(
                            digits = digits,
                            onDigitsChange = { digits = it },
                            period = period,
                            onPeriodChange = { period = it },
                            algorithm = algorithm,
                            onAlgorithmChange = { algorithm = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedOptionsSection(
    title: String = "Advanced Options",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = !expanded }
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenOptionsDropdown(
    digits: Int,
    onDigitsChange: (Int) -> Unit,
    period: Int,
    onPeriodChange: (Int) -> Unit,
    algorithm: Algorithm,
    onAlgorithmChange: (Algorithm) -> Unit,
    modifier: Modifier = Modifier,
) {
    val digitOptions = TOTPUtil.supportedDigitsList
    val periodOptions = listOf(30, 60)

    // State for dropdown expanded status
    var digitsExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }
    var algorithmExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Info,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                "Don't change anything here if it's not required by the issuer. The default settings apply to most use cases.",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        ExposedDropdownMenuBox(
            expanded = algorithmExpanded,
            onExpandedChange = { algorithmExpanded = !algorithmExpanded }
        ) {

            OutlinedTextField(
                value = algorithm.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Algorithm") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = algorithmExpanded,
                onDismissRequest = { algorithmExpanded = false }
            ) {
                Algorithm.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.toString()) },
                        onClick = {
                            onAlgorithmChange(entry)
                            algorithmExpanded = false
                        }
                    )
                }
            }
        }

        // Digits Dropdown
        ExposedDropdownMenuBox(
            expanded = digitsExpanded,
            onExpandedChange = { digitsExpanded = !digitsExpanded }
        ) {
            OutlinedTextField(
                value = digits.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Digits") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = digitsExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = digitsExpanded,
                onDismissRequest = { digitsExpanded = false }
            ) {
                digitOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toString()) },
                        onClick = {
                            onDigitsChange(option)
                            digitsExpanded = false
                        }
                    )
                }
            }
        }

        // Period Dropdown
        ExposedDropdownMenuBox(
            expanded = periodExpanded,
            onExpandedChange = { periodExpanded = !periodExpanded }
        ) {
            OutlinedTextField(
                value = period.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Period (seconds)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = periodExpanded,
                onDismissRequest = { periodExpanded = false }
            ) {
                periodOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toString()) },
                        onClick = {
                            onPeriodChange(option)
                            periodExpanded = false
                        }
                    )
                }
            }
        }
    }
}