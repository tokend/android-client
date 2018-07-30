package org.tokend.template.features.trade

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
import org.tokend.sdk.api.models.Offer
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.base.view.AmountEditTextWrapper
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.util.inputChanges
import java.math.BigDecimal

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

    private val dialogResultSubject = MaybeSubject.create<Offer>()
    private lateinit var disposable: CompositeDisposable

    private lateinit var currentOffer: Offer

    private lateinit var amountEditTextWrapper: AmountEditTextWrapper
    private lateinit var priceEditTextWrapper: AmountEditTextWrapper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window.setLayout(
                ((context ?: App.context).resources.displayMetrics.widthPixels * 0.8).toInt(),
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

        price_edit_text.setText(BigDecimalUtil.toPlainString(currentOffer.price))

        if (currentOffer.baseAmount.signum() > 0) {
            amount_edit_text.setText(BigDecimalUtil.toPlainString(currentOffer.baseAmount))
        } else {
            amount_edit_text.setText("")
        }

        amount_edit_text.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        disposable.dispose()
    }

    fun showDialog(manager: FragmentManager?, tag: String?): Maybe<Offer> {
        super.show(manager, tag)
        return dialogResultSubject
    }

    private fun btnClicks() = CompositeDisposable(
            cancel_btn.clicks()
                    .subscribe({
                        dialogResultSubject.onComplete()
                        dismiss()
                    }),

            buy_btn.clicks()
                    .subscribe({
                        dialogResultSubject.onSuccess(createOffer(true))
                        dismiss()
                    }),

            sell_btn.clicks()
                    .subscribe({
                        dialogResultSubject.onSuccess(createOffer(false))
                        dismiss()
                    })
    )

    private fun createOffer(isBuy: Boolean): Offer {
        val price = priceEditTextWrapper.scaledAmount
        val amount = amountEditTextWrapper.scaledAmount
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
        val total = BigDecimalUtil.scaleAmount(price * amount,
                AmountFormatter.ASSET_DECIMAL_DIGITS
        )

        total_amount_text_view.text =
                "${AmountFormatter.formatAssetAmount(total)} ${currentOffer.quoteAsset}"

        if (total.signum() == 0) {
            buy_btn.isEnabled = false
            sell_btn.isEnabled = false
        } else {
            buy_btn.isEnabled = true
            sell_btn.isEnabled = true
        }
    }
}