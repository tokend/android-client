package io.tokend.template.features.tfa.view.confirmation

import android.content.Context
import io.tokend.template.R
import io.tokend.template.features.tfa.model.TfaFactorCreationResult
import io.tokend.template.util.confirmation.AbstractConfirmationProvider
import io.tokend.template.view.ToastManager
import org.tokend.sdk.api.tfa.model.TfaFactor

class TfaConfirmationDialogFactory(
    private val context: Context,
    private val toastManager: ToastManager
) : AbstractConfirmationProvider<TfaFactorCreationResult>() {

    fun getConfirmationDialog(creationResult: TfaFactorCreationResult): TfaConfirmationDialog? {
        return when (creationResult.newFactor.type) {
            TfaFactor.Type.TOTP -> TfaTotpConfirmationDialog(
                context,
                creationResult.confirmationAttributes,
                toastManager,
                R.style.AlertDialogStyle
            )
            else -> null
        }
    }

    override fun onConfirmationRequested(
        payload: TfaFactorCreationResult,
        confirmationCallback: (Boolean) -> Unit
    ) {
        getConfirmationDialog(payload)
            ?.show(confirmationCallback)
            ?: confirmationCallback(false)
    }
}