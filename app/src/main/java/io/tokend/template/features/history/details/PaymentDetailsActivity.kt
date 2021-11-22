package io.tokend.template.features.history.details

import androidx.core.content.ContextCompat
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.R
import io.tokend.template.features.history.model.BalanceChange
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.view.details.DetailsItem
import io.tokend.template.view.dialog.CopyDataDialogFactory
import kotlinx.android.synthetic.main.layout_balance_change_main_data.*


class PaymentDetailsActivity : BalanceChangeDetailsActivity() {
    private var counterpartyLoadingFinished: Boolean = false

    override fun displayDetails(item: BalanceChange) {
        super.displayDetails(item)

        val details = item.cause as? BalanceChangeCause.Payment

        if (details == null) {
            finishWithError(IllegalStateException("Invalid item cause type"))
            return
        }

        val accountId = walletInfoProvider.getWalletInfo().accountId

        initCounterpartyClick(details, accountId)

        displayCounterparty(details, accountId)
        displaySubject(details)
        displayFeePaidBySenderIfNeeded(item, details)

        loadAndDisplayCounterpartyLogin(details, accountId)
    }

    private fun displayFeePaidBySenderIfNeeded(
        item: BalanceChange,
        details: BalanceChangeCause.Payment
    ) {
        if (item.isReceived == true
            && details.destFee.total.signum() > 0
            && details.isDestFeePaidBySource
        ) {
            bottom_info_text_view.text = getString(R.string.fee_paid_by_sender)
        }
    }

    private fun initCounterpartyClick(
        cause: BalanceChangeCause.Payment,
        accountId: String
    ) {
        adapter.onItemClick { _, item ->
            if (item.id == COUNTERPARTY_ITEM_ID) {
                val counterpartyAccount = cause.getCounterpartyAccountId(accountId)
                val counterpartyLogin = cause.getCounterpartyName(accountId)

                val content =
                    if (counterpartyLoadingFinished && counterpartyLogin != null)
                        counterpartyLogin + "\n\n" + counterpartyAccount
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

    private fun displayCounterparty(
        cause: BalanceChangeCause.Payment,
        accountId: String
    ) {
        val counterpartyAccount = cause.getCounterpartyAccountId(accountId)
        val counterpartyLogin = cause.getCounterpartyName(accountId)
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
                    counterpartyLogin ?: counterpartyAccount,
                hint = getString(accountIdHintStringRes),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_account),
                singleLineText = true
            )
        )
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

    private fun loadAndDisplayCounterpartyLogin(
        cause: BalanceChangeCause.Payment,
        accountId: String
    ) {
        cause.getCounterpartyName(accountId)
            .toMaybe()
            .switchIfEmpty(
                repositoryProvider
                    .accountIdentities
                    .getLoginByAccountId(cause.getCounterpartyAccountId(accountId))
            )
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .doOnEvent { _, _ ->
                counterpartyLoadingFinished = true
            }
            .subscribeBy(
                onSuccess = { login ->
                    cause.setCounterpartyName(accountId, login)
                    displayCounterparty(cause, accountId)
                },
                onError = {
                    // Not critical.
                    displayCounterparty(cause, accountId)
                }
            )
            .addTo(compositeDisposable)
    }

    companion object {
        private const val COUNTERPARTY_ITEM_ID = 1L
    }
}