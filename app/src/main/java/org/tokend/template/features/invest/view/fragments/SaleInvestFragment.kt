package org.tokend.template.features.invest.view.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_sale_invest.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.extensions.hasError
import org.tokend.template.features.invest.view.InvestmentHelpDialog
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.ContentLoadingProgressBar
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.formatter.DateFormatter
import org.tokend.template.view.util.input.AmountEditTextWrapper
import org.tokend.wallet.xdr.SaleType
import java.math.BigDecimal
import java.math.MathContext

class SaleInvestFragment : SaleFragment() {
    private val mainLoading = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private val investLoading = LoadingIndicatorManager(
            showLoading = { (invest_progress as? ContentLoadingProgressBar)?.show() },
            hideLoading = { (invest_progress as? ContentLoadingProgressBar)?.hide() }
    )

    private lateinit var amountWrapper: AmountEditTextWrapper

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val existingOffers: Map<String, OfferRecord>?
        get() = investmentInfoRepository.item?.offersByAsset

    private var investAsset: String = ""
        set(value) {
            field = value
            onInvestAssetChanged()
        }

    private val currentPrice: BigDecimal
        get() = sale.quoteAssets
                .find {
                    it.code == investAsset
                }
                ?.price
                ?: BigDecimal.ONE

    private val maxInvestAmount: BigDecimal
        get() = investmentInfoRepository
                .getMaxInvestmentAmount(investAsset, balancesRepository)

    private val receiveAmount: BigDecimal
        get() {
            val quoteAmount = amountWrapper.scaledAmount
            return BigDecimalUtil.scaleAmount(
                    quoteAmount.divide(currentPrice, MathContext.DECIMAL128),
                    amountFormatter.getDecimalDigitsCount(sale.baseAssetCode)
            )
        }

    private var canInvest: Boolean = false
        set(value) {
            field = value
            invest_button.isEnabled = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sale_invest, container, false)
    }

    override fun onInitAllowed() {
        super.onInitAllowed()

        initFields()
        initButtons()
        initAssetSelection()

        subscribeToInvestmentInfo()
        subscribeToBalances()

        updateInvestAvailability()
    }

    private fun initAssetSelection() {
        val quoteAssets = sale
                .quoteAssets
                .map { it.code }
                .sortedWith(assetComparator)

        val picker = object : BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                assetComparator,
                repositoryProvider.balances(),
                quoteAssets,
                { balance ->
                    quoteAssets.contains(balance.assetCode)
                }
        ) {
            override fun getAvailableAmount(assetCode: String, balance: BalanceRecord?): BigDecimal {
                return getAvailableBalance(assetCode)
            }
        }

        asset_edit_text.setOnClickListener {
            picker.show { result ->
                investAsset = result.assetCode
            }
        }

        val dropDownArrow = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_drop_down)
        asset_edit_text.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
                dropDownArrow, null)

        investAsset = quoteAssets.first()
    }

    private fun initFields() {
        amountWrapper = AmountEditTextWrapper(amount_edit_text)
        amountWrapper.onAmountChanged { _, _ ->
            updateInvestHelperAndError()
            updateInvestAvailability()
        }

        amount_edit_text.isHelperTextAlwaysShown = true
    }

    private fun initButtons() {
        invest_button.setOnClickListener {
            tryToInvest()
        }

        cancel_investment_button.setOnClickListener {
            tryToInvest(cancel = true)
        }

        invest_help_button.setOnClickListener {
            InvestmentHelpDialog(requireContext(), R.style.AlertDialogStyle)
                    .show()
        }
    }

    private fun subscribeToInvestmentInfo() {
        investmentInfoRepository
                .itemSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    onInvestmentInfoUpdated()
                }
                .addTo(compositeDisposable)

        investmentInfoRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    mainLoading.setLoading(it)
                    updateInvestAvailability()
                }
                .addTo(compositeDisposable)
    }

    private fun subscribeToBalances() {
        balancesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    onInvestmentInfoUpdated()
                }
                .addTo(compositeDisposable)

        balancesRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    mainLoading.setLoading(it, "balances")
                    updateInvestAvailability()
                }
                .addTo(compositeDisposable)
    }

    private fun onInvestmentInfoUpdated() {
        displayExistingInvestmentAmount()
        updateInvestHelperAndError()
        updateInvestAvailability()
    }

    private fun onInvestAssetChanged() {
        asset_edit_text.setText(investAsset)
        displayExistingInvestmentAmount()
        updateInvestHelperAndError()
        updateInvestAvailability()
    }

    private fun displayExistingInvestmentAmount() {
        val existingInvestmentAmount =
                investmentInfoRepository.getExistingInvestmentAmount(investAsset)

        amountWrapper.maxPlacesAfterComa = amountFormatter.getDecimalDigitsCount(investAsset)

        if (existingInvestmentAmount.signum() > 0) {
            amount_edit_text.setText(BigDecimalUtil.toPlainString(existingInvestmentAmount))
            amount_edit_text.setSelection(amount_edit_text.text.length)
        } else {
            amount_edit_text.text.clear()
        }
    }

    private fun updateInvestHelperAndError() {
        if (amountWrapper.scaledAmount > maxInvestAmount) {
            amount_edit_text.error = getString(R.string.template_sale_max_investment,
                    amountFormatter.formatAssetAmount(maxInvestAmount, investAsset))
        } else {
            amount_edit_text.error = null
            amount_edit_text.setHelperText(
                    getString(R.string.template_available,
                            amountFormatter.formatAssetAmount(getAvailableBalance(investAsset), investAsset))
            )
        }
    }

    private fun updateInvestAvailability() {
        canInvest = (receiveAmount.signum() > 0
                || existingOffers?.get(investAsset) != null)
                && !amount_edit_text.hasError()
                && !investLoading.isLoading
                && !mainLoading.isLoading

        updateInvestButton()
        updateCancelInvestmentButton()
        updateUnavailableInvestMessageVisibility()
    }

    private fun updateInvestButton() {
        if (existingOffers?.containsKey(investAsset) == true) {
            invest_button.setText(R.string.update_investment_action)
        } else {
            invest_button.setText(R.string.invest_action)
        }
    }

    private fun updateCancelInvestmentButton() {
        if (existingOffers?.containsKey(investAsset) == true) {
            cancel_investment_button.visibility = View.VISIBLE
        } else {
            cancel_investment_button.visibility = View.GONE
        }
    }

    private fun updateUnavailableInvestMessageVisibility() {
        when {
            sale.isAvailable -> error_empty_view.hide()
            sale.isUpcoming -> {
                error_empty_view.setEmptyDrawable(R.drawable.ic_time)
                error_empty_view.showEmpty(getString(
                        R.string.template_invest_unavailable_sale_upcoming_date,
                        DateFormatter(requireContext()).formatLong(sale.startDate)
                ))
            }
            sale.isEnded -> {
                error_empty_view.setEmptyDrawable(R.drawable.ic_time)
                error_empty_view.showEmpty(getString(R.string.invest_unavailable_sale_ended))
            }
            else -> {
                error_empty_view.setEmptyDrawable(R.drawable.ic_error_outline)
                error_empty_view.showEmpty(getString(R.string.invest_unavailable))
            }
        }
    }

    private fun getAvailableBalance(assetCode: String): BigDecimal {
        return investmentInfoRepository.getAvailableBalance(assetCode, balancesRepository)
    }

    // region Proceed invest
    private fun tryToInvest(cancel: Boolean = false) {
        if (canInvest || cancel) {
            invest(cancel)
        }
    }

    private var investDisposable: Disposable? = null
    private fun invest(cancel: Boolean) {
        investDisposable?.dispose()

        val asset = investAsset
        val amount = amountWrapper.scaledAmount
        val receiveAmount = receiveAmount
        val price = currentPrice
        val orderBookId = sale.id
        val orderId = if (cancel) existingOffers?.get(asset)?.id ?: return else 0L

        val getNewOffer =
                if (cancel)
                    Single.just(
                            OfferRecord(
                                    baseAssetCode = sale.baseAssetCode,
                                    quoteAssetCode = asset,
                                    baseAmount = BigDecimal.ZERO,
                                    price = price,
                                    isBuy = true,
                                    orderBookId = orderBookId,
                                    id = orderId
                            )
                    )
                else
                    PrepareOfferUseCase(
                            OfferRecord(
                                    baseAssetCode = sale.baseAssetCode,
                                    baseAmount = receiveAmount,
                                    quoteAssetCode = asset,
                                    quoteAmount = amount,
                                    price = price,
                                    isBuy = true,
                                    orderBookId = sale.id
                            ),
                            walletInfoProvider,
                            FeeManager(apiProvider)
                    )
                            .perform()

        investDisposable =
                getNewOffer
                        .compose(ObservableTransformers.defaultSchedulersSingle())
                        .doOnSubscribe {
                            investLoading.show()
                            updateInvestAvailability()
                        }
                        .doOnEvent { _, _ ->
                            investLoading.hide()
                            updateInvestAvailability()
                        }
                        .subscribeBy(
                                onSuccess = { offer ->
                                    Navigator.openInvestmentConfirmation(this,
                                            INVESTMENT_REQUEST,
                                            offer = offer,
                                            offerToCancel = existingOffers?.get(investAsset),
                                            saleName = sale.name,
                                            displayToReceive =
                                            sale.type.value == SaleType.BASIC_SALE.value
                                    )
                                },
                                onError = {
                                    errorHandlerFactory.getDefault().handle(it)
                                }
                        )
                        .addTo(compositeDisposable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                INVESTMENT_REQUEST -> {
                    investmentInfoRepository.update()
                }
            }
        }
    }
    // endregion

    companion object {
        private val INVESTMENT_REQUEST = "invest".hashCode() and 0xffff
    }
}