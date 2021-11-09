package io.tokend.template.data.repository

import io.reactivex.Single
import io.tokend.template.data.model.IdentityRecord
import io.tokend.template.di.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.identity.params.IdentitiesPageParams
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.util.*

class AccountDetailsRepository(
    private val apiProvider: ApiProvider
) {
    class NoIdentityAvailableException : Exception()

    private val identities = mutableSetOf<IdentityRecord>()

    /**
     * Loads account ID for given email.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getAccountIdByEmail(email: String): Single<String> {
        val formattedEmail = email.toLowerCase(Locale.ENGLISH)
        val existing = identities.find { it.email == formattedEmail }?.accountId
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(email = formattedEmail))
            .map(IdentityRecord::accountId)
    }

    /**
     * Loads email for given account ID.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getEmailByAccountId(accountId: String): Single<String> {
        val existing = identities.find { it.accountId == accountId }?.email
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
            .map(IdentityRecord::email)
    }

    fun getEmailsByAccountIds(accountIds: List<String>): Single<Map<String, String>> {
        val toReturn = mutableMapOf<String, String>()
        val toRequest = mutableListOf<String>()

        val identitiesByAccountId = identities.associateBy(IdentityRecord::accountId)

        accountIds
            .forEach { accountId ->
                val cached = identitiesByAccountId[accountId]
                if (cached != null) {
                    toReturn[accountId] = cached.email
                } else {
                    toRequest.add(accountId)
                }
            }

        if (toRequest.isEmpty()) {
            return Single.just(toReturn)
        }

        val signedApi = apiProvider.getSignedApi()
            ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
            .identities
            .getForAccounts(toRequest)
            .toSingle()
            .map {
                it.map(::IdentityRecord)
            }
            .map { identities ->
                this.identities.addAll(identities)
                toReturn.putAll(
                    identities
                        .associateBy(IdentityRecord::accountId)
                        .mapValues { it.value.email }
                )
                toReturn
            }
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