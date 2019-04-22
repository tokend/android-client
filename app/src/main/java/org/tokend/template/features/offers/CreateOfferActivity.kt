package org.tokend.template.features.offers

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.WindowManager
import com.rengwuxian.materialedittext.MaterialEditText
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_create_offer.*
import org.jetbrains.anko.onClick
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.isMaxPossibleAmount
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.template.view.util.input.AmountEditTextWrapper
import java.math.BigDecimal
import java.math.MathContext

class CreateOfferActivity : BaseActivity() {

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private lateinit var amountEditTextWrapper: AmountEditTextWrapper
    private lateinit var priceEditTextWrapper: AmountEditTextWrapper
    private lateinit var totalEditTextWrapper: AmountEditTextWrapper
    private lateinit var currentOffer: OfferRecord
    private var arrow: Drawable? = null

    private var triggerOthers: Boolean = false

    private var baseScale: Int = 0
    private var quoteScale: Int = 0
    private var baseBalance: BigDecimal = BigDecimal.ZERO
    private var quoteBalance: BigDecimal = BigDecimal.ZERO

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_create_offer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        currentOffer = intent.getSerializableExtra(EXTRA_OFFER) as? OfferRecord ?: return

        baseScale = amountFormatter.getDecimalDigitsCount(currentOffer.baseAssetCode)
        quoteScale = amountFormatter.getDecimalDigitsCount(currentOffer.quoteAssetCode)

        initViews()
        subscribeToBalances()
        updateActionsAvailability()
        updateActionHints()
        update()
    }


    private fun initViews() {
        initTextFields()
        initArrowDrawable()
        initButtons()
    }

    private fun initTextFields() {
        initAmountWrappers()

        price_edit_text.setAmount(currentOffer.price, quoteScale)
        price_edit_text.floatingLabelText =
                getString(R.string.template_offer_creation_price,
                        currentOffer.quoteAssetCode, currentOffer.baseAssetCode)

        amount_edit_text.floatingLabelText =
                getString(R.string.template_amount_hint, currentOffer.baseAssetCode)

        total_edit_text.floatingLabelText =
                getString(R.string.template_total_hint, currentOffer.quoteAssetCode)

        amount_edit_text.requestFocus()
        triggerOthers = true
    }

    private fun initAmountWrappers() {
        priceEditTextWrapper = AmountEditTextWrapper(price_edit_text).apply {
            maxPlacesAfterComa = quoteScale
            onAmountChanged { _, rawAmount ->
                onInputUpdated {
                    val unscaledTotal = rawAmount * amountEditTextWrapper.rawAmount
                    total_edit_text.setAmount(unscaledTotal, quoteScale)
                    updateActionHints()
                }
            }
        }

        amountEditTextWrapper = AmountEditTextWrapper(amount_edit_text, true).apply {
            maxPlacesAfterComa = baseScale
            onAmountChanged { _, rawAmount ->
                amount_edit_text.error = getError(rawAmount)
                onInputUpdated {
                    val unscaledTotal = rawAmount * priceEditTextWrapper.rawAmount
                    total_edit_text.setAmount(unscaledTotal, quoteScale)
                }
            }
        }

        totalEditTextWrapper = AmountEditTextWrapper(total_edit_text, true).apply {
            maxPlacesAfterComa = quoteScale
            onAmountChanged { _, rawAmount ->
                total_edit_text.error = getError(rawAmount)
                onInputUpdated {
                    val price = priceEditTextWrapper.rawAmount
                    val unscaledAmount =
                            if (price.signum() > 0) {
                                rawAmount.divide(price, MathContext.DECIMAL128)
                            } else BigDecimal.ZERO

                    amount_edit_text.setAmount(unscaledAmount, baseScale)
                }
            }
        }
    }

    private fun getError(amount: BigDecimal): String? {
        return try {
            if (amount.isMaxPossibleAmount()) {
                return getString(R.string.error_amount_to_big)
            } else null
        } catch (e: ArithmeticException) {
            getString(R.string.error_amount_to_big)
        }
    }

    private fun onInputUpdated(updateFields: () -> Unit) {
        if (triggerOthers) {
            triggerOthers = false
            updateFields.invoke()
            updateActionHints()
            updateActionsAvailability()
            triggerOthers = true
        }
    }

    private fun initButtons() {
        sell_btn.onClick {
            goToOfferConfirmation(buildOffer(false))
        }

        buy_btn.onClick {
            goToOfferConfirmation(buildOffer(true))
        }

        max_sell_text_view.onClick {
            amount_edit_text.setAmount(baseBalance, baseScale)
            amount_edit_text.requestFocus()
        }

        max_buy_text_view.onClick {
            total_edit_text.setAmount(quoteBalance, quoteScale)
            total_edit_text.requestFocus()
        }
    }

    private fun MaterialEditText.setAmount(amount: BigDecimal, scale: Int) {
        if (amount.signum() > 0) {
            val value = BigDecimalUtil.scaleAmount(amount, scale)
            setText(BigDecimalUtil.toPlainString(value))
            setSelection(text.length)
        } else {
            setText("")
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

    private fun initArrowDrawable() {
        arrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_right)
        val ascent = sell_hint.paint.fontMetrics.ascent
        val h = (-ascent).toInt()
        arrow?.setBounds(0, 0, h, h)
    }

    private fun getActionHintString(from: String, to: String): SpannableString {
        val template = SpannableString("$from * $to")
        template.setSpan(
                ImageSpan(arrow, DynamicDrawableSpan.ALIGN_BASELINE),
                from.length + 1,
                from.length + 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return template
    }

    private fun updateAvailable(balances: List<BalanceRecord>) {
        baseBalance = balances.find { it.assetCode == currentOffer.baseAssetCode }?.available
                ?: BigDecimal.ZERO
        quoteBalance = balances.find { it.assetCode == currentOffer.quoteAssetCode }?.available
                ?: BigDecimal.ZERO

        amount_edit_text.setHelperText(
                getString(R.string.template_available,
                        amountFormatter.formatAssetAmount(baseBalance,
                                currentOffer.baseAssetCode, withAssetCode = false))
        )

        total_edit_text.setHelperText(
                getString(R.string.template_available,
                        amountFormatter.formatAssetAmount(quoteBalance,
                                currentOffer.quoteAssetCode, withAssetCode = false))
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

    private fun buildOffer(isBuy: Boolean): OfferRecord {
        val price = priceEditTextWrapper.rawAmount
        val amount = amountEditTextWrapper.rawAmount
        val total = BigDecimalUtil.scaleAmount(price * amount,
                amountFormatter.getDecimalDigitsCount(currentOffer.quoteAssetCode))

        return OfferRecord(
                baseAssetCode = currentOffer.baseAssetCode,
                quoteAssetCode = currentOffer.quoteAssetCode,
                baseAmount = amount,
                price = price,
                quoteAmount = total,
                isBuy = isBuy
        )
    }

    private fun updateActionsAvailability() {
        val isAvailable = !price_edit_text.text.isNullOrBlank()
                && !amount_edit_text.text.isNullOrBlank()
                && !total_edit_text.text.isNullOrBlank()
                && !amount_edit_text.hasError()
                && !total_edit_text.hasError()

        sell_btn.isEnabled = isAvailable
        buy_btn.isEnabled = isAvailable
    }

    private var offerPreparationDisposable: Disposable? = null
    private fun goToOfferConfirmation(offer: OfferRecord) {
        offerPreparationDisposable?.dispose()

        val progress = ProgressDialogFactory.getTunedDialog(this).apply {
            setCanceledOnTouchOutside(true)
            setMessage(getString(R.string.loading_data))
            setOnCancelListener {
                offerPreparationDisposable?.dispose()
            }
        }

        offerPreparationDisposable = PrepareOfferUseCase(
                offer,
                walletInfoProvider,
                FeeManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe { progress.show() }
                .doOnEvent { _, _ -> progress.hide() }
                .subscribeBy(
                        onSuccess = { completedOffer ->
                            Navigator(this).openOfferConfirmation(
                                    CREATE_OFFER_REQUEST, completedOffer
                            )
                        },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_OFFER_REQUEST && resultCode == Activity.RESULT_OK) {
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private val CREATE_OFFER_REQUEST = "create_offer".hashCode() and 0xffff
        const val EXTRA_OFFER = "extra_offer"
    }
}
