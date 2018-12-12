package org.tokend.template.logic.wallet

import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.withdraw.model.WithdrawalRequest

interface WalletEventsListener {
    fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest)
    fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest)
}