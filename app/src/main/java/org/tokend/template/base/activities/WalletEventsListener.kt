package org.tokend.template.base.activities

import org.tokend.template.base.logic.payment.PaymentRequest
import org.tokend.template.features.withdraw.model.WithdrawalRequest

interface WalletEventsListener {
    fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest)
    fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest)
}