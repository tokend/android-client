package org.tokend.template.features.invest.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sale_invest.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.extensions.hasError
import org.tokend.template.features.invest.logic.InvestmentInfoHolder
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.repository.InvestmentInfoRepository
import org.tokend.template.features.offers.logic.CreateOfferRequestUseCase
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.ContentLoadingProgressBar
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.AmountEditTextWrapper
import org.tokend.wallet.xdr.SaleType
import java.math.BigDecimal
import java.math.MathContext

class SaleInvestActivity : BaseActivity(), InvestmentInfoHolder {
    private lateinit var mSale: SaleRecord

    override val sale: SaleRecord
        get() = mSale

    override val investmentInfoRepository: InvestmentInfoRepository
        get() = repositoryProvider.investmentInfo(sale)


    private val mainLoading = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
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

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sale_invest)

        try {
            mSale = intent.getSerializableExtra(SALE_EXTRA) as SaleRecord
        } catch (e: Exception) {
            finish()
            return
        }

        initToolbar()
        initFields()
        initButtons()
        initAssetSelection()
        initSwipeRefresh()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        subscribeToInvestmentInfo()
        subscribeToBalances()

        updateInvestAvailability()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitleAppearance)
        toolbar.subtitle = sale.name
    }

    private fun initAssetSelection() {
        val quoteAssets = sale
                .quoteAssets
                .map { it.code }
                .sortedWith(assetComparator)

        val picker = object : BalancePickerBottomDialog(
                this,
                amountFormatter,
                balanceComparator,
                balancesRepository,
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

        val dropDownArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_drop_down)
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
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener { update() }
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

    private fun update() {
        investmentInfoRepository.update()
        balancesRepository.update()
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
            amount_edit_text.setSelection(amount_edit_text.text?.length ?: 0)
        } else {
            amount_edit_text.text?.clear()
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

    private fun getAvailableBalance(assetCode: String): BigDecimal {
        return investmentInfoRepository.getAvailableBalance(assetCode, balancesRepository)
    }

    // region Help
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.invest, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.help -> openHelpDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openHelpDialog() {
        InvestmentHelpDialog(this, R.style.AlertDialogStyle).show()
    }
    // endregion


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
        val receiveAmount = receiveAmount
        val price = currentPrice
        val orderBookId = sale.id
        val offerToCancel = existingOffers?.get(asset)

        investDisposable =
                CreateOfferRequestUseCase(
                        baseAmount = if (cancel) BigDecimal.ZERO else receiveAmount,
                        baseAssetCode = sale.baseAssetCode,
                        quoteAssetCode = asset,
                        price = price,
                        isBuy = true,
                        orderBookId = orderBookId,
                        offerToCancel = offerToCancel,
                        walletInfoProvider = walletInfoProvider,
                        feeManager = FeeManager(apiProvider)
                )
                        .perform()
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
                                onSuccess = { offerRequest ->
                                    Navigator.from(this).openInvestmentConfirmation(
                                            INVESTMENT_REQUEST,
                                            request = offerRequest,
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
        const val SALE_EXTRA = "sale"
    }
}