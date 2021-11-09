package io.tokend.template.logic

import io.tokend.template.features.send.model.PaymentRequest
import io.tokend.template.features.withdraw.model.WithdrawalRequest

interface WalletEventsListener {
    fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest)
    fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest)
}