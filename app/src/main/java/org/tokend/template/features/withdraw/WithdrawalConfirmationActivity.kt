package org.tokend.template.features.withdraw

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_details_list.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.withdraw.logic.ConfirmWithdrawalRequestUseCase
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.logic.transactions.TxManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.details.DetailsItem
import org.tokend.template.view.details.adapter.DetailsItemsAdapter
import org.tokend.template.view.details.ExtraViewProvider
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.wallet.xdr.FeeType

class WithdrawalConfirmationActivity : BaseActivity() {
    private lateinit var request: WithdrawalRequest
    private val adapter = DetailsItemsAdapter()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_details_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        request =
                (intent.getSerializableExtra(WITHDRAWAL_REQUEST_EXTRA) as? WithdrawalRequest)
                        ?: return

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        displayDetails()
    }

    private fun displayDetails() {
        displayAmount()
        displayDestinationAddress()
    }

    private fun displayAmount() {
        val asset = request.asset

        adapter.addData(
                DetailsItem(
                        text = amountFormatter.formatAssetAmount(request.amount, asset),
                        hint = getString(R.string.amount),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
                )
        )

        if (request.fee.total.signum() > 0) {
            val feeExtraView =
                    ExtraViewProvider.getFeeView(this) {
                        Navigator.from(this).openFees(request.asset, FeeType.WITHDRAWAL_FEE.value)
                    }
            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(request.fee.total, asset),
                            hint = getString(R.string.tx_fee),
                            extraView = feeExtraView
                    )
            )

            adapter.addData(
                    DetailsItem(
                            text = amountFormatter.formatAssetAmount(
                                    request.amount + request.fee.total,
                                    asset
                            ),
                            hint = getString(R.string.total_label)
                    )
            )
        }

        adapter.addData(
                DetailsItem(
                        text = getString(R.string.withdrawal_fee_warning)
                )
        )
    }

    private fun displayDestinationAddress() {
        adapter.addData(
                DetailsItem(
                        text = request.destinationAddress,
                        hint = getString(R.string.tx_withdrawal_destination),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_forward)
                )
        )
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
                            toastManager.long(R.string.withdrawal_request_created)
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
