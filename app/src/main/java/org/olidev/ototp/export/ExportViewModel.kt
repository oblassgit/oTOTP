package org.olidev.ototp.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.olidev.ototp.TOTPToken
import org.olidev.ototp.model.TokenRepository

internal sealed interface ExportState {
    data object Loading : ExportState
    data class Idle(val tokenCount: Int) : ExportState
    data class Success(val tokenCount: Int) : ExportState
    data class Error(val message: String) : ExportState
}

internal class ExportViewModel(private val repository: TokenRepository) : ViewModel() {

    private val _state = MutableStateFlow<ExportState>(ExportState.Loading)
    val state: StateFlow<ExportState> = _state

    init {
        loadTokenCount()
    }

    private fun loadTokenCount() {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { repository.getAllTokens().size }
            _state.value = ExportState.Idle(count)
        }
    }

    fun exportToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ExportState.Loading
            try {
                val tokens = withContext(Dispatchers.IO) {
                    repository.getAllTokens().mapNotNull { entity ->
                        val secret = repository.getSecret(entity.id) ?: return@mapNotNull null
                        TOTPToken(
                            id = entity.id,
                            account = entity.label,
                            secret = secret,
                            issuer = entity.issuer,
                            algorithm = entity.algorithm,
                            digits = entity.digits,
                            period = entity.period
                        )
                    }
                }
                val json = OtotpExporter.serialize(tokens)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open output stream for the selected file.")
                }
                _state.value = ExportState.Success(tokens.size)
            } catch (e: Exception) {
                _state.value = ExportState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun reset() {
        loadTokenCount()
    }
}
