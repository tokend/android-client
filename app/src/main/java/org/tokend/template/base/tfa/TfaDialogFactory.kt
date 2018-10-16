package org.tokend.template.base.tfa

import android.content.Context
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.base.logic.persistance.CredentialsPersistor
import org.tokend.template.util.error_handlers.ErrorHandler

class TfaDialogFactory(private val context: Context,
                       private val errorHandler: ErrorHandler,
                       private val credentialsPersistor: CredentialsPersistor?) {
    @JvmOverloads
    fun getForException(tfaException: NeedTfaException,
                        verifierInterface: TfaVerifier.Interface,
                        email: String? = null): TfaDialog? {
        return when (tfaException.factorType) {
            TfaFactor.Type.PASSWORD -> {
                if (email != null)
                    TfaPasswordDialog(context, errorHandler, verifierInterface,
                            credentialsPersistor, tfaException, email)
                else
                    null
            }
            TfaFactor.Type.TOTP -> TfaTotpDialog(context, errorHandler, verifierInterface)
            TfaFactor.Type.EMAIL -> TfaEmailOtpDialog(context, errorHandler, verifierInterface)
            else -> TfaDefaultDialog(context, errorHandler, verifierInterface)
        }
    }
}