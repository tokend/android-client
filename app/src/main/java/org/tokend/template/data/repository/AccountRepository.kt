package org.tokend.template.data.repository

import io.reactivex.Maybe
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider

class AccountRepository(private val apiProvider: ApiProvider,
                        private val walletInfoProvider: WalletInfoProvider,
                        itemPersistence: ObjectPersistence<AccountRecord>?)
    : SingleItemRepository<AccountRecord>(itemPersistence) {

    override fun getItem(): Maybe<AccountRecord> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Maybe.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Maybe.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .v3
                .accounts
                .getById(
                        accountId,
                        AccountParamsV3(
                                include = listOf(
                                        AccountParamsV3.Includes.EXTERNAL_SYSTEM_IDS,
                                        AccountParamsV3.Includes.KYC_DATA
                                )
                        )
                )
                .toSingle()
                .map(::AccountRecord)
                .toMaybe()
    }

    /**
     * Adds new [depositAccount] locally
     */
    fun addNewDepositAccount(depositAccount: AccountRecord.DepositAccount) {
        val account = item
                ?: return

        account.depositAccounts.remove(depositAccount)
        account.depositAccounts.add(depositAccount)

        storeItem(account)
        onNewItem(account)
    }
}