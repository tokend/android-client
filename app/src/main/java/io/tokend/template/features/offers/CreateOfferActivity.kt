package io.tokend.template.features.offers

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import com.rengwuxian.materialedittext.MaterialEditText
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.extensions.getBigDecimalExtra
import io.tokend.template.extensions.hasError
import io.tokend.template.extensions.isMaxPossibleAmount
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.fees.logic.FeeManager
import io.tokend.template.features.offers.logic.CreateOfferRequestUseCase
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.ProgressDialogFactory
import io.tokend.template.view.util.input.AmountEditTextWrapper
import kotlinx.android.synthetic.main.activity_create_offer.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.math.MathContext

class CreateOfferActivity : BaseActivity() {

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances

    private lateinit var baseAsset: Asset
    private lateinit var quoteAsset: Asset
    private lateinit var requiredPrice: BigDecimal

    private val baseScale: Int
        get() = baseAsset.trailingDigits
    private val quoteScale: Int
        get() = quoteAsset.trailingDigits

    private lateinit var amountEditTextWrapper: AmountEditTextWrapper
    private lateinit var priceEditTextWrapper: AmountEditTextWrapper
    private lateinit var totalEditTextWrapper: AmountEditTextWrapper
    private var arrow: Drawable? = null

    private var triggerOthers: Boolean = false

    private var baseBalance: BigDecimal = BigDecimal.ZERO
    private var quoteBalance: BigDecimal = BigDecimal.ZERO

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_create_offer)
        setSupportActionBar(toolbar)
        setTitle(R.string.create_offer_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        baseAsset = intent.getSerializableExtra(BASE_ASSET_EXTRA) as? Asset
            ?: return
        quoteAsset = intent.getSerializableExtra(QUOTE_ASSET_EXTRA) as? Asset
            ?: return
        requiredPrice = intent.getBigDecimalExtra(PRICE_STRING_EXTRA)

        initViews()
        subscribeToBalances()
        updateActionsAvailability()
        updateActionHints()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)
        update()
    }


    private fun initViews() {
        initTextFields()
        initArrowDrawable()
        initButtons()
    }

    private fun initTextFields() {
        initAmountWrappers()

        price_edit_text.setAmount(requiredPrice, quoteScale)
        price_edit_text.floatingLabelText =
            getString(
                R.string.template_offer_creation_price,
                quoteAsset.code, baseAsset.code
            )

        amount_edit_text.floatingLabelText =
            getString(R.string.template_amount_hint, baseAsset.code)

        total_edit_text.floatingLabelText =
            getString(R.string.template_total_hint, quoteAsset.code)

        if (requiredPrice.signum() == 0) {
            price_edit_text.requestFocus()
        } else {
            amount_edit_text.requestFocus()
        }

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
                return getString(R.string.error_too_big_amount)
            } else null
        } catch (e: ArithmeticException) {
            getString(R.string.error_too_big_amount)
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
        sell_btn.setOnClickListener {
            goToOfferConfirmation(false)
        }

        buy_btn.setOnClickListener {
            goToOfferConfirmation(true)
        }

        max_sell_text_view.setOnClickListener {
            amount_edit_text.setAmount(baseBalance, baseScale)
            amount_edit_text.requestFocus()
        }

        max_buy_text_view.setOnClickListener {
            total_edit_text.setAmount(quoteBalance, quoteScale)
            total_edit_text.requestFocus()
        }
    }

    private fun MaterialEditText.setAmount(amount: BigDecimal, scale: Int) {
        if (amount.signum() > 0) {
            val value = BigDecimalUtil.scaleAmount(amount, scale)
            setText(BigDecimalUtil.toPlainString(value))
            setSelection(text?.length ?: 0)
        } else {
            setText("")
        }
    }

    private fun updateActionHints() {
        val amount = amountFormatter.formatAssetAmount(
            amountEditTextWrapper.rawAmount,
            baseAsset
        )
        val total = amountFormatter.formatAssetAmount(
            totalEditTextWrapper.rawAmount,
            quoteAsset
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
        arrow?.also {
            template.setSpan(
                ImageSpan(it, DynamicDrawableSpan.ALIGN_BASELINE),
                from.length + 1,
                from.length + 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return template
    }

    private fun updateAvailable(balances: List<BalanceRecord>) {
        baseBalance = balances.find { it.assetCode == baseAsset.code }?.available
            ?: BigDecimal.ZERO
        quoteBalance = balances.find { it.assetCode == quoteAsset.code }?.available
            ?: BigDecimal.ZERO

        amount_edit_text.setHelperText(
            getString(
                R.string.template_available,
                amountFormatter.formatAssetAmount(
                    baseBalance,
                    baseAsset, withAssetCode = false
                )
            )
        )

        total_edit_text.setHelperText(
            getString(
                R.string.template_available,
                amountFormatter.formatAssetAmount(
                    quoteBalance,
                    quoteAsset, withAssetCode = false
                )
            )
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

    private fun updateActionsAvailability() {
        val isAvailable = !price_edit_text.text.isNullOrBlank()
                && !amount_edit_text.text.isNullOrBlank()
                && !total_edit_text.text.isNullOrBlank()
                && !amount_edit_text.hasError()
                && !total_edit_text.hasError()

        sell_btn.isEnabled = isAvailable
        buy_btn.isEnabled = isAvailable
    }

    private var offerCreationDisposable: Disposable? = null
    private fun goToOfferConfirmation(isBuy: Boolean) {
        offerCreationDisposable?.dispose()

        val price = priceEditTextWrapper.scaledAmount
        val amount = amountEditTextWrapper.scaledAmount

        val progress = ProgressDialogFactory.getDialog(
            this,
            R.string.loading_data
        ) {
            offerCreationDisposable?.dispose()
        }

        offerCreationDisposable = CreateOfferRequestUseCase(
            baseAmount = amount,
            isBuy = isBuy,
            price = price,
            orderBookId = 0,
            baseAsset = baseAsset,
            quoteAsset = quoteAsset,
            offerToCancel = null,
            walletInfoProvider = walletInfoProvider,
            feeManager = FeeManager(apiProvider)
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .doOnSubscribe { progress.show() }
            .doOnEvent { _, _ -> progress.cancel() }
            .subscribeBy(
                onSuccess = { offerRequest ->
                    Navigator.from(this)
                        .openOfferConfirmation(offerRequest)
                        .addTo(activityRequestsBag)
                        .doOnSuccess { finish() }
                },
                onError = { errorHandlerFactory.getDefault().handle(it) }
            )
            .addTo(compositeDisposable)
    }

    companion object {
        private const val BASE_ASSET_EXTRA = "base_asset"
        private const val QUOTE_ASSET_EXTRA = "quote_asset"
        private const val PRICE_STRING_EXTRA = "price"

        fun getBundle(
            baseAsset: Asset,
            quoteAsset: Asset,
            requiredPrice: BigDecimal?
        ) = Bundle().apply {
            putSerializable(BASE_ASSET_EXTRA, baseAsset)
            putSerializable(QUOTE_ASSET_EXTRA, quoteAsset)
            putString(PRICE_STRING_EXTRA, BigDecimalUtil.toPlainString(requiredPrice))
        }
    }
}
