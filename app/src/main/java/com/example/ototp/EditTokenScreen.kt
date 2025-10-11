package com.example.ototp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTokenScreen(
    initialToken: TOTPToken,
    onSave: (TOTPToken) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var label by remember { mutableStateOf(initialToken.label) }
    var secret by remember { mutableStateOf(initialToken.secret) }
    var issuer by remember { mutableStateOf(initialToken.issuer ?: "") }
    var algorithm by remember { mutableStateOf(initialToken.algorithm ?: "") }
    var digits by remember { mutableStateOf(initialToken.digits?.toString() ?: "") }
    var period by remember { mutableStateOf(initialToken.period?.toString() ?: "") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Token") },
                navigationIcon = { IconButton(onClick = { onNavigateUp() }) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = {
                        if (label.isNotEmpty() && secret.isNotEmpty()) {
                            onSave(
                                TOTPToken(
                                    label = label,
                                    secret = secret,
                                    issuer = issuer.ifBlank { null },
                                    algorithm = algorithm.ifBlank { null },
                                    digits = digits.toIntOrNull(),
                                    period = period.toLongOrNull()
                                )
                            )
                        }
                    }) { Icon(Icons.Default.Check, "Accept") }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize().padding(12.dp)) {

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
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                OutlinedTextField(
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
                )
            }

        }
    }

}