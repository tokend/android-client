package org.tokend.template.features.tfa.view.confirmation

import android.content.Context
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.R
import org.tokend.template.features.tfa.model.TfaFactorCreationResult
import org.tokend.template.util.confirmation.AbstractConfirmationProvider
import org.tokend.template.view.ToastManager

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

    override fun onConfirmationRequested(payload: TfaFactorCreationResult,
                                         confirmationCallback: (Boolean) -> Unit) {
        getConfirmationDialog(payload)
                ?.show(confirmationCallback)
                ?: confirmationCallback(false)
    }
}