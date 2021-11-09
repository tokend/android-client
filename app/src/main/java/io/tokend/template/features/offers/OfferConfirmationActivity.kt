package io.tokend.template.features.offers

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.offers.logic.ConfirmOfferRequestUseCase
import io.tokend.template.features.offers.model.OfferRecord
import io.tokend.template.features.offers.model.OfferRequest
import io.tokend.template.logic.TxManager
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.balancechange.BalanceChangeMainDataView
import io.tokend.template.view.details.DetailsItem
import io.tokend.template.view.details.ExtraViewProvider
import io.tokend.template.view.details.adapter.DetailsItemsAdapter
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.ProgressDialogFactory
import kotlinx.android.synthetic.main.activity_balance_change_confirmation.*
import kotlinx.android.synthetic.main.appbar_with_balance_change_main_data.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.wallet.xdr.FeeType
import java.math.BigDecimal

open class OfferConfirmationActivity : BaseActivity() {
    protected lateinit var request: OfferRequest
    protected val offerToCancel: OfferRecord?
        get() = request.offerToCancel

    protected val adapter = DetailsItemsAdapter()
    protected lateinit var mainDataView: BalanceChangeMainDataView

    protected val payAsset: Asset
        get() =
            if (request.isBuy)
                request.quoteAsset
            else
                request.baseAsset
    protected val toPayTotal: BigDecimal
        get() =
            if (request.isBuy)
                request.quoteAmount + request.fee.total
            else
                request.baseAmount

    protected val receiveAsset: Asset
        get() =
            if (!request.isBuy)
                request.quoteAsset
            else
                request.baseAsset
    protected val toReceiveTotal: BigDecimal
        get() =
            (if (!request.isBuy)
                request.quoteAmount - request.fee.total
            else
                request.baseAmount).takeIf { it.signum() > 0 } ?: BigDecimal.ZERO

    protected val cancellationOnly: Boolean
        get() = request.baseAmount.signum() == 0 && offerToCancel != null

    protected open val feeType: Int = FeeType.OFFER_FEE.value

    protected val feeExtraView: AppCompatImageView by lazy {
        ExtraViewProvider.getFeeView(this) {
            Navigator.from(this).openFees(request.quoteAsset.code, feeType)
        }
    }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_balance_change_confirmation)

        initToolbar()

        mainDataView = BalanceChangeMainDataView(appbar, amountFormatter)

        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter

        request =
            (intent.getSerializableExtra(OFFER_REQUEST_EXTRA) as? OfferRequest)
                ?: return

        displayDetails()
        initConfirmButton()
        ElevationUtil.initScrollElevation(details_list, appbar_elevation_view)
    }

    private fun initToolbar() {
        toolbar.background = ColorDrawable(ContextCompat.getColor(this, R.color.background))
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    // region Display
    protected open fun displayDetails() {
        (top_info_text_view.layoutParams as? LinearLayout.LayoutParams)?.also {
            it.topMargin = 0
            top_info_text_view.layoutParams = it
        }

        mainDataView.displayOperationName(getString(R.string.offer_confirmation_title))
        displayToPay()
        displayToReceive()
        displayPrice()
    }

    protected open fun displayPrice() {
        adapter.addData(
            DetailsItem(
                text = getString(
                    R.string.template_price_one_equals, request.baseAsset.code,
                    amountFormatter.formatAssetAmount(request.price, request.quoteAsset)
                ),
                hint = getString(R.string.price),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_price)
            )
        )
    }

    protected open fun displayToPay() {
        val fee =
            if (request.isBuy)
                request.fee.total
            else
                BigDecimal.ZERO

        mainDataView.displayAmount(toPayTotal, payAsset, false)
        mainDataView.displayNonZeroFee(fee, payAsset)
    }

    protected open fun displayToReceive() {
        adapter.addData(
            DetailsItem(
                text = amountFormatter.formatAssetAmount(toReceiveTotal, receiveAsset),
                hint = getString(R.string.to_receive),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_coins)
            )
        )

        if (!request.isBuy && request.fee.total.signum() > 0) {
            adapter.addData(
                DetailsItem(
                    text = amountFormatter.formatAssetAmount(request.fee.total, receiveAsset),
                    hint = getString(R.string.tx_fee),
                    extraView = feeExtraView
                )
            )
        }
    }
    // endregion

    private fun initConfirmButton() {
        confirm_button.setOnClickListener { confirm() }
    }

    private fun confirm() {
        val progress = ProgressDialogFactory.getDialog(this)

        ConfirmOfferRequestUseCase(
            request,
            accountProvider,
            repositoryProvider,
            TxManager(apiProvider)
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe { progress.show() }
            .doOnTerminate { progress.dismiss() }
            .subscribeBy(
                onComplete = {
                    progress.dismiss()
                    toastManager.short(getSuccessMessage())
                    finishWithSuccess()
                },
                onError = {
                    errorHandlerFactory.getDefault().handle(it)
                }
            )
    }

    protected open fun getSuccessMessage(): String {
        return getString(R.string.offer_created)
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        private const val OFFER_REQUEST_EXTRA = "offer_request"

        fun getBundle(offerRequest: OfferRequest) = Bundle().apply {
            putSerializable(OFFER_REQUEST_EXTRA, offerRequest)
        }
    }
}
