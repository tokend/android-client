package org.tokend.template.base.logic

import org.tokend.sdk.signing.RequestSigner
import org.tokend.wallet.Account

class AccountRequestSigner(
        private val account: Account
) : RequestSigner {
    override val accountId: String = account.accountId

    override fun signToBase64(data: ByteArray): String {
        return account.signDecorated(data).toBase64()
    }
}