package org.tokend.template.base.logic.repository

import io.reactivex.Single
import org.tokend.sdk.api.models.AccountsDetailsResponse
import org.tokend.sdk.api.requests.models.AccountsDetailsRequestBody
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.extensions.toSingle

class AccountDetailsRepository(
        private val apiProvider: ApiProvider
) {
    private val detailsByAccountId = mutableMapOf<String, AccountsDetailsResponse.AccountDetails>()

    public fun getDetails(accounts: List<String>):
            Single<Map<String, AccountsDetailsResponse.AccountDetails>> {
        val toReturn = mutableMapOf<String, AccountsDetailsResponse.AccountDetails>()
        val toRequest = mutableListOf<String>()

        accounts.forEach {
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

        return signedApi.getAccountsDetails(AccountsDetailsRequestBody(accounts))
                .toSingle()
                .map { it.users }
                .map { detailsMap ->
                    detailsByAccountId.putAll(detailsMap)
                    toReturn.putAll(detailsMap)
                    toReturn
                }
    }
}