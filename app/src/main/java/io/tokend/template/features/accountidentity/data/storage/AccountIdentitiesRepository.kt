package io.tokend.template.features.accountidentity.data.storage

import io.reactivex.Single
import io.tokend.template.features.accountidentity.data.model.AccountIdentityRecord
import io.tokend.template.features.accountidentity.data.model.NoIdentityAvailableException
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.identity.params.IdentitiesPageParams
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.util.*

class AccountIdentitiesRepository(
    private val apiProvider: ApiProvider
) {
    private val identities = mutableSetOf<AccountIdentityRecord>()
    private val notExistingIdentifiers = mutableSetOf<String>()

    /**
     * Loads account ID for given login.
     * Result will be cached.
     *
     * @param login - email or phone number
     *
     * @see NoIdentityAvailableException
     */
    fun getAccountIdByLogin(login: String): Single<String> {
        val formattedIdentifier = login.toLowerCase(Locale.ENGLISH)
        val existing = identities.find { it.login == formattedIdentifier }?.accountId
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(identifier = formattedIdentifier))
            .map(AccountIdentityRecord::accountId)
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
            .map(AccountIdentityRecord::login)
    }

    fun getLoginsByAccountIds(accountIds: Collection<String>): Single<Map<String, String>> {
        val toReturn = mutableMapOf<String, String>()
        val toRequest = mutableListOf<String>()

        val identitiesByAccountId = identities.associateBy(AccountIdentityRecord::accountId)

        accountIds
            .forEach { accountId ->
                val cached = identitiesByAccountId[accountId]
                if (cached != null) {
                    toReturn[accountId] = cached.login
                } else if (!notExistingIdentifiers.contains(accountId)) {
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
                it.map(::AccountIdentityRecord)
            }
            .map { identities ->
                this.identities.addAll(identities)
                toReturn.putAll(
                    identities
                        .associateBy(AccountIdentityRecord::accountId)
                        .mapValues { it.value.login }
                )
                toReturn
            }
    }

    private fun getIdentity(params: IdentitiesPageParams): Single<AccountIdentityRecord> {
        val identifier = params.identifier ?: params.address

        if (identifier != null && notExistingIdentifiers.contains(identifier)) {
            return Single.error(NoIdentityAvailableException())
        }

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
            .map(::AccountIdentityRecord)
            .doOnSuccess { identities.add(it) }
            .doOnError {
                if (it is NoIdentityAvailableException && identifier != null) {
                    notExistingIdentifiers.add(identifier)
                }
            }
    }

    fun getCachedIdentity(accountId: String): AccountIdentityRecord? {
        return identities.find { it.accountId == accountId }
    }

    fun invalidateCachedIdentity(accountId: String) {
        identities.remove(getCachedIdentity(accountId))
    }
}