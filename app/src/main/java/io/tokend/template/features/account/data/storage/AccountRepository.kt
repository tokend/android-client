package io.tokend.template.features.account.data.storage

import io.reactivex.Single
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.account.data.model.NoSuchAccountRoleException
import io.tokend.template.features.account.data.model.ResolvedAccountRole
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3

/**
 * @see NoSuchAccountRoleException
 */
class AccountRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val keyValueEntriesRepository: KeyValueEntriesRepository,
    itemPersistence: ObjectPersistence<AccountRecord>?
) : SingleItemRepository<AccountRecord>(itemPersistence) {

    override fun getItem(): Single<AccountRecord> {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val signedApi = apiProvider.getSignedApi()

        return Single.zip(
            signedApi.v3.accounts
                .getById(
                    accountId,
                    AccountParamsV3(
                        include = listOf(
                            AccountParamsV3.Includes.EXTERNAL_SYSTEM_IDS,
                            AccountParamsV3.Includes.KYC_DATA
                        )
                    )
                )
                .toSingle(),
            keyValueEntriesRepository
                .ensureEntries(AccountRole.keyValues()),
            { accountResource, keyValueEntriesMap ->
                AccountRecord(accountResource, keyValueEntriesMap.values)
            }
        )
    }

    fun updateKycRecoveryStatus(newStatus: AccountRecord.KycRecoveryStatus) {
        item?.also { account ->
            account.kycRecoveryStatus = newStatus
            storeItem(account)
            broadcast()
        }
    }

    fun updateRole(newRole: ResolvedAccountRole) {
        item?.also { account ->
            account.role = newRole
            storeItem(account)
            broadcast()
        }
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