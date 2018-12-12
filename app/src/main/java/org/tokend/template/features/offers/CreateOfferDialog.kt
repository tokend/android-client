package org.tokend.template.features.offers

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.*
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.MaybeSubject
import kotlinx.android.synthetic.main.fragment_dialog_crate_order.*
import kotlinx.android.synthetic.main.layout_amount_with_spinner.*
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.extensions.inputChanges
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.input.AmountEditTextWrapper
import org.tokend.template.view.util.input.SoftInputUtil
import java.math.BigDecimal
import java.math.MathContext

class CreateOfferDialog : DialogFragment() {

    companion object {
        private const val EXTRA_ORDER = "extra_order"

        fun withArgs(order: Offer): CreateOfferDialog {

            val dialog = CreateOfferDialog()

            val args = Bundle()
            args.putSerializable(EXTRA_ORDER, order)
            dialog.arguments = args

            return dialog
        }
    }

    private var asset: String = ""
        set(value) {
            field = value
            isSwitched = value == currentOffer.quoteAsset
            updateTotal()
        }

    private var isSwitched = false
    private val dialogResultSubject = MaybeSubject.create<Offer>()
    private lateinit var disposable: CompositeDisposable
    private lateinit var currentOffer: Offer
    private lateinit var amountEditTextWrapper: AmountEditTextWrapper
    private lateinit var priceEditTextWrapper: AmountEditTextWrapper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window.setLayout(
                (requireContext().resources.displayMetrics.widthPixels * 0.8).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return inflater.inflate(R.layout.fragment_dialog_crate_order, container)
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        amountEditTextWrapper = AmountEditTextWrapper(amount_edit_text)
        priceEditTextWrapper = AmountEditTextWrapper(price_edit_text)

        disposable = CompositeDisposable(
                btnClicks(),
                textFields()
        )

        arguments?.let { args ->
            currentOffer = args.getSerializable(EXTRA_ORDER) as Offer
        } ?: return

        asset = currentOffer.baseAsset
        initTextFields()
        initAssetSpinner()
    }

    private fun initTextFields() {
        getString(R.string.amount).also {
            amount_edit_text.floatingLabelText = it
            amount_edit_text.hint = it
        }

        price_edit_text.setText(BigDecimalUtil.toPlainString(currentOffer.price))
        getString(R.string.template_offer_creation_price,
                currentOffer.quoteAsset, currentOffer.baseAsset)
                .also {
                    price_edit_text.floatingLabelText = it
                    price_edit_text.hint = it
                }

        if (currentOffer.baseAmount.signum() > 0) {
            amount_edit_text.setText(BigDecimalUtil.toPlainString(currentOffer.baseAmount))
        } else {
            amount_edit_text.setText("")
        }
        amount_edit_text.requestFocus()
    }

    private fun initAssetSpinner() {
        asset_spinner
                .setSimpleItems(
                        listOf(currentOffer.baseAsset,
                                currentOffer.quoteAsset),
                        currentOffer.baseAsset)

        asset_spinner.onItemSelected {
            asset = it.text
        }
    }

    override fun onStop() {
        super.onStop()
        disposable.dispose()
    }

    fun showDialog(manager: FragmentManager?, tag: String?): Maybe<Offer> {
        super.show(manager, tag)
        return dialogResultSubject
    }

    private fun createOffer(isBuy: Boolean): Offer {
        val price = priceEditTextWrapper.rawAmount

        val amount = when (isSwitched) {
            false -> amountEditTextWrapper.rawAmount
            else -> amountEditTextWrapper.rawAmount.divide(price, MathContext.DECIMAL128)
        }

        val total = BigDecimalUtil.scaleAmount(price * amount,
                AmountFormatter.ASSET_DECIMAL_DIGITS
        )

        return Offer(
                baseAsset = currentOffer.baseAsset,
                quoteAsset = currentOffer.quoteAsset,
                baseAmount = amount,
                price = price,
                quoteAmount = total,
                isBuy = isBuy
        )
    }

    private fun btnClicks() = CompositeDisposable(
            cancel_btn.clicks()
                    .subscribe {
                        dialogResultSubject.onComplete()
                        dismiss()
                    },

            buy_btn.clicks()
                    .subscribe {
                        SoftInputUtil.hideSoftInput(amount_edit_text)
                        dialogResultSubject.onSuccess(createOffer(!isSwitched))
                        dismiss()
                    },

            sell_btn.clicks()
                    .subscribe {
                        SoftInputUtil.hideSoftInput(amount_edit_text)
                        dialogResultSubject.onSuccess(createOffer(isSwitched))
                        dismiss()
                    }
    )

    private fun textFields() =
            Observable.combineLatest(
                    price_edit_text.inputChanges(),
                    amount_edit_text.inputChanges(),
                    BiFunction<String, String, Pair<BigDecimal, BigDecimal>> { n, a ->
                        Pair(BigDecimalUtil.valueOf(n), BigDecimalUtil.valueOf(a))
                    })
                    .doOnNext {
                        onValuesUpdated(it.first, it.second)
                    }
                    .subscribe()

    private fun onValuesUpdated(price: BigDecimal, amount: BigDecimal) {

        val total = when (isSwitched) {
            false -> price * amount
            else -> amount.divide(price, MathContext.DECIMAL128)
        }

        val finalAsset = when (isSwitched) {
            false -> currentOffer.quoteAsset
            else -> currentOffer.baseAsset
        }

        total_amount_text_view.text =
                "${AmountFormatter.formatAssetAmount(total)} $finalAsset"

        if (total.signum() == 0) {
            buy_btn.isEnabled = false
            sell_btn.isEnabled = false
        } else {
            buy_btn.isEnabled = true
            sell_btn.isEnabled = true
        }
    }

    private fun updateTotal() {
        onValuesUpdated(
                priceEditTextWrapper.rawAmount,
                amountEditTextWrapper.rawAmount
        )
    }
}