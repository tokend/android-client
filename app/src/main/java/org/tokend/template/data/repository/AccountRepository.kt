package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.storage.persistence.ObjectPersistence
import org.tokend.template.data.storage.repository.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider

class AccountRepository(private val apiProvider: ApiProvider,
                        private val walletInfoProvider: WalletInfoProvider,
                        itemPersistence: ObjectPersistence<AccountRecord>?)
    : SingleItemRepository<AccountRecord>(itemPersistence) {

    override fun getItem(): Single<AccountRecord> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

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