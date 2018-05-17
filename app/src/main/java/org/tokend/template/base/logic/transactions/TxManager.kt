package org.tokend.template.base.logic.transactions

import io.reactivex.Single
import okhttp3.ResponseBody
import org.tokend.sdk.api.ApiFactory
import org.tokend.sdk.api.responses.SubmitTransactionResponse
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.Transaction
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class TxManager(
        private val apiProvider: ApiProvider
) {
    fun submit(transaction: Transaction): Single<SubmitTransactionResponse> {
        return apiProvider.getApi().pushTransaction(transaction.getEnvelope().toBase64())
                .toSingle()
                // Wrap failed submit response into special exception.
                .onErrorResumeNext {
                    if (it is HttpException
                            && it.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
                        val submitResponse = responseFromErrorBody(it.response().errorBody())
                        if (submitResponse != null) {
                            Single.error(TransactionFailedException(submitResponse))
                        } else {
                            Single.error<SubmitTransactionResponse>(it)
                        }
                    } else {
                        Single.error<SubmitTransactionResponse>(it)
                    }
                }
                // Magic delay is required because
                // API not syncs with Horizon immediately.
                .delay(1, TimeUnit.SECONDS)
    }

    private fun responseFromErrorBody(errorBody: ResponseBody): SubmitTransactionResponse? {
        val buffer = errorBody.source().buffer().clone()
        val string = buffer.readString(Charset.defaultCharset())
        return try {
            ApiFactory.getBaseGson().fromJson(string, SubmitTransactionResponse::class.java)
        } catch (e: Exception) {
            null
        } finally {
            buffer.close()
        }
    }
}