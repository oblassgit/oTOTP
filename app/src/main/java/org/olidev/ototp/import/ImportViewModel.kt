package org.olidev.ototp.`import`

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.olidev.ototp.model.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ImportViewModel(private val repository: TokenRepository) : ViewModel() {

    private val _state = MutableStateFlow<Import.State>(Import.State.Idle)
    val state: StateFlow<Import.State> = _state

    fun handleAction(action: Import.Action) {
        when (action) {
            is Import.Action.ParseFile -> parseTwoFasFile(action.inputStream)
            is Import.Action.ParseGoogleAuthQR -> parseGoogleAuthQR(action.data)
            is Import.Action.ToggleToken -> toggleToken(action.index)
            is Import.Action.SelectAll -> setAllSelected(true)
            is Import.Action.DeselectAll -> setAllSelected(false)
            is Import.Action.ConfirmImport -> importSelected()
            is Import.Action.DismissError -> _state.value = Import.State.Idle
            is Import.Action.Reset -> _state.value = Import.State.Idle
        }
    }

    private fun parseTwoFasFile(inputStream: java.io.InputStream) {
        _state.value = Import.State.Loading
        viewModelScope.launch {
            try {
                val tokens = withContext(Dispatchers.IO) {
                    val bytes = inputStream.readBytes()
                    val content = bytes.toString(Charsets.UTF_8)
                    val json = org.json.JSONObject(content)
                    when {
                        json.optString("app") == "oTOTP" -> OtotpImporter.parse(bytes.inputStream())
                        else -> TwoFasImporter.parse(bytes.inputStream())
                    }
                }
                _state.value = if (tokens.isEmpty()) {
                    Import.State.Error("No TOTP tokens found in the selected file.")
                } else {
                    Import.State.TokensReady(tokens.map { Import.SelectableToken(it) })
                }
            } catch (e: Exception) {
                _state.value = Import.State.Error(e.message ?: "Failed to parse the backup file.")
            }
        }
    }

    private fun parseGoogleAuthQR(data: String) {
        _state.value = Import.State.Loading
        viewModelScope.launch {
            try {
                val tokens = withContext(Dispatchers.IO) {
                    GoogleAuthImporter.parse(data)
                }
                _state.value = if (tokens.isEmpty()) {
                    Import.State.Error("No TOTP tokens found in the scanned QR code.")
                } else {
                    Import.State.TokensReady(tokens.map { Import.SelectableToken(it) })
                }
            } catch (e: Exception) {
                _state.value = Import.State.Error(e.message ?: "Failed to parse the Google Authenticator QR code.")
            }
        }
    }

    private fun toggleToken(index: Int) {
        val current = _state.value as? Import.State.TokensReady ?: return
        val updated = current.tokens.toMutableList()
        updated[index] = updated[index].copy(isSelected = !updated[index].isSelected)
        _state.value = current.copy(tokens = updated)
    }

    private fun setAllSelected(selected: Boolean) {
        val current = _state.value as? Import.State.TokensReady ?: return
        _state.value = current.copy(tokens = current.tokens.map { it.copy(isSelected = selected) })
    }

    private fun importSelected() {
        val current = _state.value as? Import.State.TokensReady ?: return
        val selected = current.tokens.filter { it.isSelected }.map { it.token }
        if (selected.isEmpty()) return

        _state.value = Import.State.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    selected.forEach { repository.addToken(it) }
                }
                _state.value = Import.State.Success
            } catch (e: Exception) {
                _state.value = Import.State.Error(e.message ?: "Failed to import tokens.")
            }
        }
    }
}
