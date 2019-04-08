package org.tokend.template.features.offers

import android.graphics.drawable.Drawable
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_create_offer.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import io.reactivex.rxkotlin.addTo
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.input.AmountEditTextWrapper

class CreateOfferActivity : BaseActivity() {

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var amountEditTextWrapper: AmountEditTextWrapper
    private lateinit var priceEditTextWrapper: AmountEditTextWrapper
    private lateinit var totalEditTextWrapper: AmountEditTextWrapper
    private lateinit var currentOffer: OfferRecord
    private var arrow: Drawable? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_create_offer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        currentOffer = intent.getSerializableExtra(EXTRA_OFFER) as? OfferRecord ?: return

        initViews()
        subscribeToBalances()
        update()
    }


    private fun initViews() {
        initArrowDrawable()
        initTextFields()
        updateActionHints()
    }

    private fun initTextFields() {
        priceEditTextWrapper = AmountEditTextWrapper(price_edit_text)
        priceEditTextWrapper.onAmountChanged { _, _ ->

        }
        price_edit_text.setText(
                amountFormatter.formatAssetAmount(
                        currentOffer.price,
                        currentOffer.quoteAssetCode,
                        withAssetCode = false)
        )
        getString(R.string.template_offer_creation_price,
                currentOffer.quoteAssetCode, currentOffer.baseAssetCode)
                .also {
                    price_edit_text.floatingLabelText = it
                    price_edit_text.hint = it
                }

        amountEditTextWrapper = AmountEditTextWrapper(amount_edit_text)
        amountEditTextWrapper.onAmountChanged { _, _ ->

        }
        amount_edit_text.setText(
                amountFormatter.formatAssetAmount(
                        currentOffer.baseAmount,
                        currentOffer.baseAssetCode,
                        withAssetCode = false
                )
        )
        getString(R.string.template_amount_hint, currentOffer.baseAssetCode).also {
            amount_edit_text.floatingLabelText = it
            amount_edit_text.hint = it
        }

        totalEditTextWrapper = AmountEditTextWrapper(total_edit_text)
        totalEditTextWrapper.onAmountChanged { _, _ ->

        }
        total_edit_text.setText(
                amountFormatter.formatAssetAmount(
                        currentOffer.quoteAmount,
                        currentOffer.quoteAssetCode,
                        withAssetCode = false
                )
        )
        getString(R.string.template_total_hint, currentOffer.quoteAssetCode).also {
            total_edit_text.floatingLabelText = it
            total_edit_text.hint = it
        }
    }

    private fun updateActionHints() {
        val amount = amountFormatter.formatAssetAmount(
                amountEditTextWrapper.rawAmount,
                currentOffer.baseAssetCode
        )
        val total = amountFormatter.formatAssetAmount(
                totalEditTextWrapper.rawAmount,
                currentOffer.quoteAssetCode
        )

        sell_hint.text = getActionHintString(amount, total)
        buy_hint.text = getActionHintString(total, amount)
    }

    private fun getActionHintString(from: String, to: String): SpannableString {
        val template = SpannableString("$from * $to")
        template.setSpan(
                ImageSpan(arrow, DynamicDrawableSpan.ALIGN_BASELINE),
                from.length + 1,
                to.length + 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return template
    }

    private fun updateAvailable(balances: List<BalanceRecord>) {
        val baseBalance = balances.find { it.assetCode == currentOffer.baseAssetCode }?.available
        val quoteBalance = balances.find { it.assetCode == currentOffer.quoteAssetCode }?.available

        amount_edit_text.setHelperText(
                getString(R.string.template_available,
                        amountFormatter.formatAssetAmount(baseBalance, currentOffer.baseAssetCode))
        )

        amount_edit_text.setHelperText(
                getString(R.string.template_available,
                        amountFormatter.formatAssetAmount(quoteBalance, currentOffer.quoteAssetCode))
        )
    }

    private fun subscribeToBalances() {
        balancesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    updateAvailable(it)
                }
                .addTo(compositeDisposable)

        balancesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    errorHandlerFactory.getDefault().handle(it)
                }
                .addTo(compositeDisposable)
    }

    private fun update(force: Boolean = false) {
        if (force) {
            balancesRepository.update()
        } else {
            balancesRepository.updateIfNotFresh()
        }
    }

    private fun initArrowDrawable() {
        arrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_right)
        val ascent = sell_hint.paint.fontMetrics.ascent
        val h = (-ascent).toInt()
        arrow!!.setBounds(0, 0, h, h)
    }

    companion object {
        const val EXTRA_OFFER = "extra_offer"
    }
}
