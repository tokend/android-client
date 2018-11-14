package org.tokend.template.features.withdraw

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ProgressDialogFactory
import org.tokend.template.util.ToastManager

class WithdrawalConfirmationActivity : BaseActivity() {
    private lateinit var request: WithdrawalRequest

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        request =
                (intent.getSerializableExtra(WITHDRAWAL_REQUEST_EXTRA) as? WithdrawalRequest)
                ?: return

        displayDetails()
    }

    private fun displayDetails() {
        displayDestinationAddress()
        displayToPay()
        displayToReceive()
    }

    private fun displayDestinationAddress() {
        InfoCard(cards_layout)
                .setHeading(R.string.tx_withdrawal_destination, null)
                .addRow(request.destinationAddress, null)
    }

    private fun displayToPay() {
        val toPay = request.amount + request.fee.total

        InfoCard(cards_layout)
                .setHeading(R.string.to_pay,
                        "${AmountFormatter.formatAssetAmount(toPay)} ${request.asset}")
                .addRow(R.string.amount,
                        "+${AmountFormatter.formatAssetAmount(request.amount,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${request.asset}")
                .addRow(R.string.tx_fixed_fee,
                        "+${AmountFormatter.formatAssetAmount(request.fee.fixed,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${request.asset}")
                .addRow(R.string.tx_percent_fee,
                        "+${AmountFormatter.formatAssetAmount(request.fee.percent,
                                minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
                        } ${request.asset}")
    }

    private fun displayToReceive() {
        InfoCard(cards_layout)
                .setHeading(R.string.to_receive,
                        "${AmountFormatter.formatAssetAmount(request.amount)} " +
                                request.asset)
                .addRow(getString(R.string.template_withdrawal_fee_warning, request.asset),
                        null)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.confirmation, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.confirm -> confirm()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirm() {
        val progress = ProgressDialogFactory.getTunedDialog(this)

        ConfirmWithdrawalRequestUseCase(
                request,
                accountProvider,
                repositoryProvider,
                TxManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    progress.show()
                }
                .doOnTerminate {
                    progress.dismiss()
                }
                .subscribeBy(
                        onComplete = {
                            progress.dismiss()
                            ToastManager(this).long(R.string.withdrawal_request_created)
                            finishWithSuccess()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
    }


    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK,
                Intent().putExtra(WITHDRAWAL_REQUEST_EXTRA, request))
        finish()
    }

    companion object {
        const val WITHDRAWAL_REQUEST_EXTRA = "withdrawal_request"
    }
}
