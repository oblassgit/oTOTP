package com.example.ototp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun AddSecretScreen(navController: NavController, myViewModel: MyViewModel) {
    AddSecretScreen(
        onNavigateUp = { navController.navigateUp() },
        onAccept = { service: String, secret: String ->
            myViewModel.saveSecret(service, secret)
            navController.navigateUp()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSecretScreen(
    onNavigateUp: () -> Unit,
    onAccept: (service: String, secret: String) -> Unit,
) {
    var serviceName by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var secretVisibility by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Add Token") },
                navigationIcon = { IconButton(onClick = { onNavigateUp() }) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = {
                        if (serviceName.isNotEmpty() || secret.isNotEmpty()) onAccept(serviceName, secret)
                    }) { Icon(Icons.Default.Check, "Accept") }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize().padding(12.dp)) {
            Column {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

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

            }

        }
    }
}

@Preview
@Composable
fun AddSecretScreenPreview() {
    AddSecretScreen({}, {service, secret -> })
}