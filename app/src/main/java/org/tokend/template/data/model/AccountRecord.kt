package org.tokend.template.data.model

import org.tokend.sdk.api.accounts.model.Account
import org.tokend.wallet.xdr.AccountType
import java.util.*

class AccountRecord(
        val id: String,
        val type: AccountType,
        val depositAccounts: List<DepositAccount>
) {
    constructor(source: Account) : this(
            id = source.accountId,
            type = AccountType.values().find { it.value == source.typeI }!!,
            depositAccounts =
            source.externalAccounts.map {
                AccountRecord.DepositAccount(
                        type = it.type.value,
                        address = it.data,
                        expirationDate = it.expirationDate
                )
            }
    )

    class DepositAccount(
            val type: Int,
            val address: String,
            val expirationDate: Date?
    )
}