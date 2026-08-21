package org.olidev.ototp.`import`

import org.olidev.ototp.TOTPToken

internal interface Import {

    data class SelectableToken(
        val token: TOTPToken,
        val isSelected: Boolean = true
    )

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class TokensReady(val tokens: List<SelectableToken>) : State
        data class Error(val message: String) : State
        data object Success : State
    }

    sealed interface Action {
        data class ParseFile(val inputStream: java.io.InputStream) : Action
        data class ParseGoogleAuthQR(val data: String) : Action
        data class ToggleToken(val index: Int) : Action
        data object SelectAll : Action
        data object DeselectAll : Action
        data object ConfirmImport : Action
        data object DismissError : Action
        data object Reset : Action
    }
}
