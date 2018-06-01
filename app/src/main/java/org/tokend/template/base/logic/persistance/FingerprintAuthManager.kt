package org.tokend.template.base.logic.persistance

import android.content.Context
import android.os.Build
import io.reactivex.rxkotlin.toSingle
import org.tokend.template.util.ObservableTransformers

class FingerprintAuthManager(
        context: Context,
        private val credentialsPersistor: CredentialsPersistor
) {
    private val fingerprintUtil = FingerprintUtil(context)
    private var isAuthCanceled = false

    fun requestAuthIfAvailable(onAuthStart: () -> Unit,
                               onSuccess: (String, CharArray) -> Unit,
                               onError: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        fingerprintUtil.cancelAuth()

        isAuthCanceled = false

        val savedEmail = credentialsPersistor.getSavedEmail()
                ?: return
        {
            credentialsPersistor.hasSavedPassword()
                    && fingerprintUtil.isFingerprintAvailable
        }
                .toSingle()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .subscribe { fingerprintAuthAvailable ->
                    if (fingerprintAuthAvailable && !isAuthCanceled) {
                        onAuthStart()

                        val handleErrorMessage: (String?) -> Unit = {
                            it?.also(onError)
                        }

                        fingerprintUtil.requestAuth(
                                onSuccess = {
                                    val password = credentialsPersistor.getSavedPassword()
                                            ?: CharArray(0)
                                    onSuccess(savedEmail, password)
                                },
                                onError = handleErrorMessage,
                                onHelp = handleErrorMessage
                        )
                    }
                }
    }

    fun cancelAuth() {
        fingerprintUtil.cancelAuth()
        isAuthCanceled = true
    }
}