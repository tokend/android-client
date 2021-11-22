package io.tokend.template.data.repository

import io.reactivex.Single
import io.tokend.template.data.model.IdentityRecord
import io.tokend.template.logic.providers.ApiProvider
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
     * Loads account ID for given login.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getAccountIdByLogin(login: String): Single<String> {
        val formattedLogin = login.toLowerCase(Locale.ENGLISH)
        val existing = identities.find { it.login == formattedLogin }?.accountId
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(email = formattedLogin))
            .map(IdentityRecord::accountId)
    }

    /**
     * Loads login for given account ID.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getLoginByAccountId(accountId: String): Single<String> {
        val existing = identities.find { it.accountId == accountId }?.login
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
            .map(IdentityRecord::login)
    }

    fun getLoginsByAccountIds(accountIds: List<String>): Single<Map<String, String>> {
        val toReturn = mutableMapOf<String, String>()
        val toRequest = mutableListOf<String>()

        val identitiesByAccountId = identities.associateBy(IdentityRecord::accountId)

        accountIds
            .forEach { accountId ->
                val cached = identitiesByAccountId[accountId]
                if (cached != null) {
                    toReturn[accountId] = cached.login
                } else {
                    toRequest.add(accountId)
                }
            }

        if (toRequest.isEmpty()) {
            return Single.just(toReturn)
        }

        val signedApi = apiProvider.getSignedApi()

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
                        .mapValues { it.value.login }
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