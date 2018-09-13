package org.tokend.template.base.logic.repository

import io.reactivex.Single
import org.tokend.sdk.api.models.AccountsDetailsResponse
import org.tokend.sdk.api.requests.models.AccountsDetailsRequestBody
import org.tokend.sdk.api.responses.AccountResponse
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.extensions.toSingle
import retrofit2.HttpException
import java.net.HttpURLConnection

class AccountDetailsRepository(
        private val apiProvider: ApiProvider
) {
    class NoDetailsFoundException : Exception()

    private val detailsByAccountId = mutableMapOf<String, AccountsDetailsResponse.AccountDetails>()
    private val accountIdByEmail = mutableMapOf<String, String>()

    /**
     * Loads details for given list of AccountIDs.
     * Result will be cached.
     * @param accounts list of AccountIDs
     */
    fun getDetails(accounts: List<String?>):
            Single<Map<String, AccountsDetailsResponse.AccountDetails>> {
        val toReturn = mutableMapOf<String, AccountsDetailsResponse.AccountDetails>()
        val toRequest = mutableListOf<String>()

        accounts
                .filterNotNull()
                .forEach {
                    val cached = detailsByAccountId[it]
                    if (cached != null) {
                        toReturn[it] = cached
                    } else {
                        toRequest.add(it)
                    }
                }

        if (toRequest.isEmpty()) {
            return Single.just(toReturn)
        }

        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.getAccountsDetails(AccountsDetailsRequestBody(toRequest))
                .toSingle()
                .map { it.users }
                .map { detailsMap ->
                    detailsByAccountId.putAll(detailsMap)
                    toReturn.putAll(detailsMap)
                    toReturn
                }
    }

    fun getAccountIdByEmail(email: String): Single<String> {
        val existing = accountIdByEmail[email]
        if (existing != null) {
            return Single.just(existing)
        }

        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.getAccountIdByEmail(email)
                .toSingle()
                .map { it.accountId }
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

    fun getBalancesByAccountId(accountId: String): Single<List<AccountResponse.Balance>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.getAccountBalances(accountId)
                .toSingle()
                .onErrorResumeNext {
                    if (it is HttpException && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                        Single.error(NoDetailsFoundException())
                    else
                        Single.error(it)
                }
    }
}