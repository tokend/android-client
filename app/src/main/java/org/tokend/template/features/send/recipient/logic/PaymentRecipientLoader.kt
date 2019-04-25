package org.tokend.template.features.send.recipient.logic

import io.reactivex.Single
import org.tokend.template.data.repository.AccountDetailsRepository
import org.tokend.template.features.send.recipient.model.PaymentRecipient
import org.tokend.wallet.Base32Check

/**
 * Loads payment recipient info
 */
class PaymentRecipientLoader(
        private val accountDetailsRepository: AccountDetailsRepository
) {
    class NoRecipientFoundException(recipient: String)
        : Exception("No recipient account ID found for $recipient")

    /**
     * Loads payment recipient info if [recipient] is an email or just
     * returns it immediately if [recipient] is an account ID
     *
     * @see NoRecipientFoundException
     */
    fun load(recipient: String): Single<PaymentRecipient> {
        return if (Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                        recipient.toCharArray()))
            Single.just(PaymentRecipient(recipient))
        else
            accountDetailsRepository
                    .getAccountIdByEmail(recipient)
                    .map { accountId ->
                        PaymentRecipient(
                                accountId = accountId,
                                nickname = recipient
                        )
                    }
                    .onErrorResumeNext { error ->
                        if (error is AccountDetailsRepository.NoIdentityAvailableException)
                            Single.error(NoRecipientFoundException(recipient))
                        else
                            Single.error(error)
                    }
    }
}