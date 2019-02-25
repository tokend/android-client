package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.accounts.model.AccountsDetailsResponse
import org.tokend.sdk.api.identity.params.IdentitiesPageParams
import org.tokend.template.di.providers.ApiProvider
import retrofit2.HttpException
import java.net.HttpURLConnection

class AccountDetailsRepository(
        private val apiProvider: ApiProvider
) {
    class NoDetailsFoundException : Exception()

    private val detailsByAccountId = mutableMapOf<String, AccountsDetailsResponse.AccountDetails>()
    private val accountIdByEmail = mutableMapOf<String, String>()

    /**
     * Loads account ID for given email.
     * Result will be cached.
     */
    fun getAccountIdByEmail(email: String): Single<String> {
        val existing = accountIdByEmail[email]
        if (existing != null) {
            return Single.just(existing)
        }

        val api = apiProvider.getApi()

        return api
                .identities
                .get(IdentitiesPageParams(email = email))
                .toSingle()
                .map { detailsPage ->
                    detailsPage.items.firstOrNull()?.address
                            ?: throw NoDetailsFoundException()
                }
                .onErrorResumeNext {
                    if (it is HttpException && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                        Single.error(NoDetailsFoundException())
                    else
                        Single.error(it)
                }
                .doOnSuccess { accountId ->
                    accountIdByEmail[email] = accountId
                }
    }
}