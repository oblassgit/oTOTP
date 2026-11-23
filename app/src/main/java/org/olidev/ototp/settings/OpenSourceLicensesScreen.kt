package org.olidev.ototp.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.olidev.ototp.R

@Composable
internal fun OpenSourceLicensesScreen(onNavigateBack: () -> Unit) {
    val resources = LocalContext.current.resources
    val result by produceState<Result<Libs>?>(null, resources) {
        value = withContext(Dispatchers.IO) {
            try {
                val metadata = resources.openRawResource(R.raw.aboutlibraries)
                    .bufferedReader().use { it.readText() }
                val coordinates = resources.openRawResource(R.raw.runtime_license_dependencies)
                    .bufferedReader().use { it.readLines().filter(String::isNotBlank).toSet() }
                Result.success(Libs.Builder().withJson(metadata).build().runtimeLibraries(coordinates))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Result.failure(exception)
            }
        }
    }

    OpenSourceLicensesScreen(
        libraries = result?.getOrNull(),
        loadFailed = result?.isFailure == true,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpenSourceLicensesScreen(
    libraries: Libs?,
    loadFailed: Boolean = false,
    onNavigateBack: () -> Unit
) {
    var selectedLibraryId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.open_source_licenses)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.licenses_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            loadFailed -> LicenseStatus(R.string.licenses_load_error, Modifier.padding(innerPadding))
            libraries == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            libraries.libraries.isEmpty() -> LicenseStatus(
                R.string.licenses_empty, Modifier.padding(innerPadding)
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                item {
                    Text(
                        stringResource(R.string.licenses_scope),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(libraries.libraries, key = { it.uniqueId }) { library ->
                    ListItem(
                        headlineContent = { Text(library.name) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(library.artifactId)
                                Text(
                                    library.licenses.joinToString { it.name }.ifBlank {
                                        stringResource(R.string.license_information_unavailable)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.clickable { selectedLibraryId = library.uniqueId }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val selectedLibrary = libraries?.libraries?.find { it.uniqueId == selectedLibraryId }
    if (selectedLibrary != null) {
        AlertDialog(
            onDismissRequest = { selectedLibraryId = null },
            title = { Text(selectedLibrary.name) },
            confirmButton = {
                TextButton(onClick = { selectedLibraryId = null }) {
                    Text(stringResource(R.string.licenses_close))
                }
            },
            text = {
                SelectionContainer {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(selectedLibrary.artifactId, style = MaterialTheme.typography.bodySmall)
                        if (selectedLibrary.licenses.isEmpty()) {
                            Text(stringResource(R.string.license_information_unavailable))
                        }
                        selectedLibrary.licenses.forEach { license ->
                            Text(license.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                license.licenseContent?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.license_information_unavailable)
                            )
                            license.url?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun LicenseStatus(message: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(message))
    }
}
