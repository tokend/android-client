package org.tokend.template.base.tfa

import android.content.Context
import org.tokend.sdk.api.tfa.TfaBackend
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.sdk.federation.NeedTfaException

class TfaDialogFactory(private val context: Context) {
    @JvmOverloads
    fun getForException(tfaException: NeedTfaException,
                        verifierInterface: TfaVerifier.Interface,
                        email: String? = null): TfaDialog? {
        return when (tfaException.backendType) {
            TfaBackend.Type.PASSWORD -> {
                if (email != null)
                    TfaPasswordDialog(context, verifierInterface,
                            tfaException, email)
                else
                    null
            }
            TfaBackend.Type.TOTP -> TfaTotpDialog(context, verifierInterface)
            else -> null
        }
    }
}