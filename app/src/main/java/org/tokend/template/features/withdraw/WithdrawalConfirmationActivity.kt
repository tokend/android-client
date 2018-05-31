package org.tokend.template.features.withdraw

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details.*
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.singleTop
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.activities.MainActivity
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

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
        val progress = ProgressDialog(this)
        progress.isIndeterminate = true
        progress.setMessage(getString(R.string.processing_progress))
        progress.setCancelable(false)

        WithdrawalManager(repositoryProvider, walletInfoProvider, accountProvider,
                TxManager(apiProvider))
                .submit(request)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .doOnSubscribe {
                    progress.show()
                }
                .doOnEvent { _, _ ->
                    progress.dismiss()
                }
                .subscribeBy(
                        onSuccess = {
                            progress.dismiss()
                            ToastManager.long(R.string.withdrawal_request_created)
                            finishWithSuccess()
                        },
                        onError = {
                            ErrorHandlerFactory.getDefault().handle(it)
                        }
                )
    }


    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK,
                Intent().putExtra(ASSET_RESULT_EXTRA, request.asset))
        finish()
    }

    companion object {
        const val WITHDRAWAL_REQUEST_EXTRA = "withdrawal_request"
        const val ASSET_RESULT_EXTRA = "asset"
    }
}
