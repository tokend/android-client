package org.tokend.template.features.signin.logic

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.tokend.sdk.api.authenticator.AuthResultsApi
import org.tokend.sdk.api.authenticator.model.AuthResult
import org.tokend.sdk.api.base.model.DataEntity
import org.tokend.rx.extensions.toSingle
import java.util.concurrent.TimeUnit

class AuthResultPoller(
        val publicKey: String,
        val authResultsApi: AuthResultsApi,
        val pollingDelayMs: Long = DEFAULT_POLLING_DELAY_MS) {

    /**
     * @return Observable which polls auth result for the public key
     * and emits it once it was obtained
     */
    fun getObservable(): Observable<AuthResult> {
        val dumbObject = DataEntity(null)

        val getCheckingObservable =
                {
                    authResultsApi
                            .getAuthResult(publicKey)
                            .toSingle()
                            .map<DataEntity<AuthResult?>> { DataEntity(it) }
                            .onErrorReturnItem(dumbObject)
                            .toObservable()
                }

        return Observable.zip(
                Observable.defer { getCheckingObservable() },
                Observable.timer(pollingDelayMs, TimeUnit.MILLISECONDS),
                BiFunction { pollingResult: DataEntity<AuthResult?>, _: Any -> pollingResult }
        )
                .repeat()
                .filter { it.data != null }
                .map { it.data!! }
    }

    companion object {
        private const val DEFAULT_POLLING_DELAY_MS = 1500L
    }

}