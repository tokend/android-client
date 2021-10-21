package org.tokend.template.features.signup.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.SignerData
import org.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import org.tokend.wallet.Account

object WalletAccountsUtil {
    /**
     * @return accounts required for user to operate in current environment.
     */
    fun getAccountsForNewWallet(): Single<List<Account>> {
        return {
            // By default it's just root account.
            listOf(Account.random())
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    /**
     * @return complete set of signers required for the account to operate.
     *
     */
    fun getSignersForNewWallet(
        orderedAccountIds: List<String>,
        keyValueRepository: KeyValueEntriesRepository
    ): Single<Collection<SignerData>> {
        return keyValueRepository
            .ensureEntries(listOf(KeyServer.DEFAULT_SIGNER_ROLE_KEY_VALUE_KEY))
            .map {
                it[KeyServer.DEFAULT_SIGNER_ROLE_KEY_VALUE_KEY] as KeyValueEntryRecord.Number
            }
            .map(KeyValueEntryRecord.Number::value)
            .map { defaultSignerRole ->
                orderedAccountIds.map { accountId ->
                    SignerData(
                        id = accountId,
                        roleId = defaultSignerRole
                    )
                }
            }
    }
}