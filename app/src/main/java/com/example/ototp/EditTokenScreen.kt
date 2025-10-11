package com.example.ototp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ototp.ui.theme.OTOTPTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditTokenScreen(
    initialToken: TOTPToken? = null,
    onSave: (TOTPToken) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    // If initialToken is null, use empty/default values
    var label by remember { mutableStateOf(initialToken?.label ?: "") }
    var secret by remember { mutableStateOf(initialToken?.secret ?: "") }
    var issuer by remember { mutableStateOf(initialToken?.issuer ?: "") }
    var algorithm by remember { mutableStateOf(initialToken?.algorithm ?: "") }
    var digits by remember { mutableStateOf(initialToken?.digits ?: 6) }
    var period by remember { mutableStateOf(initialToken?.period ?: 30) }

    var secretVisibility by remember { mutableStateOf(false) }

    val isEditMode = initialToken != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Token" else "Add Token") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateUp() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (label.isNotEmpty() && secret.isNotEmpty()) {
                                onSave(
                                    TOTPToken(
                                        id = initialToken?.id,
                                        label = label,
                                        secret = secret,
                                        issuer = issuer.ifBlank { null },
                                        algorithm = algorithm.ifBlank { null },
                                        digits = digits,
                                        period = period
                                    )
                                )
                            }
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
                .padding(12.dp)
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                /*OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )*/
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (!secretVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon =  {
                        IconButton(onClick = { secretVisibility = !secretVisibility }) {
                            if (secretVisibility) Icon(Icons.Default.VisibilityOff, "toggle visibility off") else Icon(Icons.Default.Visibility, "toggle visibility on")
                        }
                    }
                )
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Issuer (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = algorithm,
                    onValueChange = { algorithm = it },
                    label = { Text("Algorithm (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                /*OutlinedTextField(
                    value = digits,
                    onValueChange = { digits = it.filter { c -> c.isDigit() } },
                    label = { Text("Digits (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it.filter { c -> c.isDigit() } },
                    label = { Text("Period (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )*/
                TokenOptionsDropdown(
                    digits = digits,
                    onDigitsChange = { digits = it },
                    period = period,
                    onPeriodChange = { period = it }
                )
            }
        }
    }
}
@Composable
fun DigitSelectDialog(
    selection: Int
) {
    Dialog(
        onDismissRequest = {}
    ) {
        val list = TOTPUtil.supportedDigitsList
        Card {

            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                items(list) { digits ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selection == digits,
                            onClick = {

                            }
                        )

                        Text(digits.toString())
                    }
                }
            }
        }

    }
}

@Preview
@Composable
fun DigitSelectDialogPreview() {
    OTOTPTheme {
        Box(
            Modifier.fillMaxSize()
                .background(Color.White)
        ) {

            DigitSelectDialog(6)
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
    modifier: Modifier = Modifier
) {
    val digitOptions = TOTPUtil.supportedDigitsList
    val periodOptions = listOf(30, 60)

    // State for dropdown expanded status
    var digitsExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
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
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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

        Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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