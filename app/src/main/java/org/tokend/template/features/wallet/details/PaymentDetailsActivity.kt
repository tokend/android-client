package org.tokend.template.features.wallet.details

import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.util.CopyDataDialogFactory
import java.math.BigDecimal


class PaymentDetailsActivity : BalanceChangeDetailsActivity() {
    private val adapter = DetailsItemsAdapter()

    private var counterpartyLoadingFinished: Boolean = false

    override fun displayDetails(item: BalanceChange) {
        setContentView(R.layout.activity_details_list)

        val details = item.cause as? BalanceChangeCause.Payment

        if (details == null) {
            finish()
            return
        }

        val accountId = walletInfoProvider.getWalletInfo()?.accountId

        if (accountId == null) {
            finish()
            return
        }

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter
        (details_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        initCounterpartyClick(details, accountId)

        displayEffect(item, adapter)
        displayCounterparty(details, accountId)
        displayPaidOrReceived(item, details, accountId)
        displaySubject(details)
        displayDate(item, adapter)

        loadAndDisplayCounterpartyEmail(details, accountId)
    }

    private fun initCounterpartyClick(cause: BalanceChangeCause.Payment,
                                      accountId: String) {
        adapter.onItemClick { _, item ->
            if (item.id == COUNTERPARTY_ITEM_ID) {
                val counterpartyAccount = cause.getCounterpartyAccountId(accountId)
                val counterpartyEmail = cause.getCounterpartyName(accountId)

                val content =
                        if (counterpartyLoadingFinished && counterpartyEmail != null)
                            counterpartyEmail + "\n\n" + counterpartyAccount
                        else
                            counterpartyAccount

                CopyDataDialogFactory.getDialog(
                        this,
                        content,
                        item.hint,
                        toastManager,
                        getString(R.string.data_has_been_copied)
                )
            }
        }
    }

    private fun displayCounterparty(cause: BalanceChangeCause.Payment,
                                    accountId: String) {
        val counterpartyAccount = cause.getCounterpartyAccountId(accountId)
        val counterpartyEmail = cause.getCounterpartyName(accountId)

        val isReceived = cause.isReceived(accountId)

        val accountIdHintStringRes =
                if (isReceived)
                    R.string.tx_sender
                else
                    R.string.tx_recipient

        adapter.addOrUpdateItem(
                DetailsItem(
                        id = COUNTERPARTY_ITEM_ID,
                        text =
                        if (!counterpartyLoadingFinished)
                            getString(R.string.loading_data)
                        else
                            counterpartyEmail ?: counterpartyAccount,
                        hint = getString(accountIdHintStringRes),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account),
                        singleLineText = true
                )
        )
    }

    private fun displayPaidOrReceived(item: BalanceChange,
                                      cause: BalanceChangeCause.Payment,
                                      accountId: String) {
        val isReceived = cause.isReceived(accountId)

        if (isReceived) {
            displayReceived(item, cause)
        } else {
            displayPaid(item, cause)
        }
    }

    private fun displayReceived(item: BalanceChange,
                                cause: BalanceChangeCause.Payment) {
        val feePaidBySender = cause.isDestFeePaidBySource

        val paidFeeTotal =
                if (feePaidBySender)
                    BigDecimal.ZERO
                else
                    cause.destFee.total

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(item.amount, item.assetCode),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (paidFeeTotal.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(paidFeeTotal, item.assetCode),
                            hint = getString(R.string.tx_fee)
                    ),
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    item.amount - paidFeeTotal,
                                    item.assetCode
                            ),
                            hint = getString(R.string.total_label)
                    )
            )
        }

        if (feePaidBySender && cause.destFee.total.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = getString(R.string.fee_paid_by_sender_explanation)
                    )
            )
        }
    }

    private fun displayPaid(item: BalanceChange,
                            cause: BalanceChangeCause.Payment) {
        val feePaidBySender = cause.isDestFeePaidBySource

        val paidFeeTotal =
                if (feePaidBySender)
                    cause.sourceFee.total + cause.destFee.total
                else
                    cause.sourceFee.total

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(item.amount, item.assetCode),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (paidFeeTotal.signum() > 0) {
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(paidFeeTotal, item.assetCode),
                            hint = getString(R.string.tx_fee)
                    ),
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    item.amount + paidFeeTotal,
                                    item.assetCode
                            ),
                            hint = getString(R.string.total_label)
                    )
            )

            if (feePaidBySender) {
                adapter.addData(
                        DetailsItem(
                                text = getString(R.string.fee_paid_by_sender_explanation)
                        )
                )
            }
        }
    }

    private fun displaySubject(cause: BalanceChangeCause.Payment) {
        val subject = cause.subject?.takeIf { it.isNotBlank() }
                ?: return

        adapter.addData(
                DetailsItem(
                        text = subject,
                        hint = getString(R.string.payment_description),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_label_outline)
                )
        )
    }

    private fun loadAndDisplayCounterpartyEmail(cause: BalanceChangeCause.Payment,
                                                accountId: String) {
        val counterpartyAccountId = cause.getCounterpartyAccountId(accountId)

        repositoryProvider
                .accountDetails()
                .getEmailByAccountId(counterpartyAccountId)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnEvent { _, _ ->
                    counterpartyLoadingFinished = true
                    displayCounterparty(cause, accountId)
                }
                .subscribeBy(
                        onSuccess = { email ->
                            cause.setCounterpartyName(accountId, email)
                        },
                        onError = {
                            // Not critical.
                        }
                )
                .addTo(compositeDisposable)
    }

    companion object {
        private const val COUNTERPARTY_ITEM_ID = 1L
    }
}