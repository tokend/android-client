package io.tokend.template.data.repository

import io.reactivex.Single
import io.tokend.template.data.model.AccountRecord
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3

class AccountRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    itemPersistence: ObjectPersistence<AccountRecord>?
) : SingleItemRepository<AccountRecord>(itemPersistence) {

    override fun getItem(): Single<AccountRecord> {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val signedApi = apiProvider.getSignedApi()

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

    fun updateRole(newRoleId: Long) {
        item?.also { account ->
            account.roleId = newRoleId
            storeItem(account)
            broadcast()
        }
    }
}