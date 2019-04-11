package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.identity.params.IdentitiesPageParams
import org.tokend.template.data.model.IdentityRecord
import org.tokend.template.di.providers.ApiProvider
import retrofit2.HttpException
import java.net.HttpURLConnection

class AccountDetailsRepository(
        private val apiProvider: ApiProvider
) {
    class NoIdentityAvailableException : Exception()

    private val identities = mutableSetOf<IdentityRecord>()

    /**
     * Loads account ID for given email.
     * Result will be cached.
     */
    fun getAccountIdByEmail(email: String): Single<String> {
        val existing = identities.find { it.email == email }?.accountId
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(email = email))
                .map(IdentityRecord::accountId)
    }

    /**
     * Loads email for given account ID.
     * Result will be cached.
     */
    fun getEmailByAccountId(accountId: String): Single<String> {
        val existing = identities.find { it.accountId == accountId }?.email
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
                .map(IdentityRecord::email)
    }

    private fun getIdentity(params: IdentitiesPageParams): Single<IdentityRecord> {
        return apiProvider
                .getApi()
                .identities
                .get(params)
                .toSingle()
                .map { detailsPage ->
                    detailsPage.items.firstOrNull()
                            ?: throw NoIdentityAvailableException()
                }
                .onErrorResumeNext {
                    if (it is HttpException && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                        Single.error(NoIdentityAvailableException())
                    else
                        Single.error(it)
                }
                .map(::IdentityRecord)
                .doOnSuccess { identities.add(it) }
    }
}