package org.tokend.template.features.invest.activities

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import com.google.gson.JsonSyntaxException
import com.squareup.picasso.Picasso
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sale.*
import kotlinx.android.synthetic.main.layout_amount_with_spinner.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.favorites.model.FavoriteEntry
import org.tokend.sdk.api.favorites.model.SaleFavoriteEntry
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.repository.AccountRepository
import org.tokend.template.data.repository.favorites.FavoritesRepository
import org.tokend.template.extensions.Sale
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.extensions.hasError
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.features.invest.logic.InvestmentInfoManager
import org.tokend.template.features.invest.logic.SwitchFavoriteUseCase
import org.tokend.template.features.invest.view.SaleProgressWrapper
import org.tokend.template.features.offers.logic.PrepareOfferUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.util.CircleTransform
import org.tokend.template.util.FileDownloader
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.ContentLoadingProgressBar
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.input.AmountEditTextWrapper
import org.tokend.wallet.xdr.AccountType
import org.tokend.wallet.xdr.AssetPolicy
import org.tokend.wallet.xdr.SaleType
import java.math.BigDecimal
import java.math.MathContext

class SaleActivity : BaseActivity() {
    private val mainLoading = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private val investLoading = LoadingIndicatorManager(
            showLoading = { (sale_invest_progress as? ContentLoadingProgressBar)?.show() },
            hideLoading = { (sale_invest_progress as? ContentLoadingProgressBar)?.hide() }
    )

    private lateinit var fileDownloader: FileDownloader

    private val accountRepository: AccountRepository
        get() = repositoryProvider.account()

    private val favoritesRepository: FavoritesRepository
        get() = repositoryProvider.favorites()

    private lateinit var feeManager: FeeManager

    private lateinit var sale: Sale
    private lateinit var saleAsset: org.tokend.template.extensions.Asset
    private lateinit var investmentInfoManager: InvestmentInfoManager

    private var isFavorited = false
        set(value) {
            field = value
            invalidateOptionsMenu()
        }

    private var existingOffers: Map<String, Offer> = emptyMap()
    private var maxFees: Map<String, BigDecimal> = emptyMap()
    private var maxInvestAmount = BigDecimal.ZERO

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

    private val receiveAmount: BigDecimal
        get() {
            val quoteAmount = investAmountWrapper.scaledAmount
            return BigDecimalUtil.scaleAmount(
                    quoteAmount.divide(currentPrice, MathContext.DECIMAL128),
                    AmountFormatter.getDecimalDigitsCount(sale.baseAsset)
            )
        }

    private val existingInvestmentAmount: BigDecimal
        get() = investmentInfoManager.getExistingInvestmentAmount(investAsset, existingOffers)

    private var canInvest: Boolean = false
        set(value) {
            field = value
            invest_button.isEnabled = value
        }

    private lateinit var investAmountWrapper: AmountEditTextWrapper

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sale)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        feeManager = FeeManager(apiProvider)
        fileDownloader = FileDownloader(this, urlConfigProvider.getConfig().storage)

        supportPostponeEnterTransition()

        initButtons()
        initFields()

        try {
            sale = GsonFactory().getBaseGson().fromJson(
                    intent.getNullableStringExtra(SALE_JSON_EXTRA),
                    Sale::class.java)
            investmentInfoManager = InvestmentInfoManager(sale, repositoryProvider, walletInfoProvider)

            displaySaleInfo()
            update()
        } catch (e: JsonSyntaxException) {
            finish()
            return
        } finally {
            supportStartPostponedEnterTransition()
        }

        subscribeToFavorites()

        canInvest = false
    }

    private fun initButtons() {
        more_info_button.onClick {
            Navigator.openSaleDetails(this, sale)
        }
    }

    private fun initFields() {
        investAmountWrapper = AmountEditTextWrapper(amount_edit_text)
    }

    // region Update
    /**
     * Updates all sale-related data.
     */
    private fun update() {
        repositoryProvider.balances()
                .updateIfNotFreshDeferred()
                .andThen(
                        investmentInfoManager
                                .getInvestmentInfo(
                                        FeeManager(apiProvider)
                                )
                )
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    mainLoading.show()
                    updateInvestAvailability()
                }
                .doOnEvent { _, _ ->
                    mainLoading.hide()
                    updateInvestAvailability()
                }
                .subscribeBy(
                        onSuccess = { result ->
                            this.sale = result.financialInfo.detailedSale
                            this.saleAsset = result.assetDetails
                            this.existingOffers = result.financialInfo.offersByAsset
                            this.maxFees = result.financialInfo.maxFeeByAsset

                            onInvestmentInfoUpdated()
                        },
                        onError = {
                            it.printStackTrace()
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    /**
     * Updates only data required for amount calculations and validation.
     */
    private fun updateFinancial() {
        repositoryProvider.balances()
                .updateIfNotFreshDeferred()
                .andThen(
                        investmentInfoManager
                                .getFinancialInfo(
                                        FeeManager(apiProvider)
                                )
                )
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    mainLoading.show()
                    updateInvestAvailability()
                }
                .doOnEvent { _, _ ->
                    mainLoading.hide()
                    updateInvestAvailability()
                }
                .subscribeBy(
                        onSuccess = { result ->
                            this.sale = result.detailedSale
                            this.existingOffers = result.offersByAsset
                            this.maxFees = result.maxFeeByAsset

                            onInvestmentInfoUpdated()
                        },
                        onError = {
                            it.printStackTrace()
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun onInvestmentInfoUpdated() {
        displayChangeableSaleInfo()
        displayAssetDetails()
        initInvestIfNeeded()
        displayExistingInvestmentAmount()
        updateInvestLimit()
        updateInvestHelperAndError()
        updateInvestAvailability()
    }
    // endregion

    // region Info display
    private fun displaySaleInfo() {
        title = sale.details.name
        sale_name_text_view.text = sale.details.name
        sale_description_text_view.text = sale.details.shortDescription

        if (sale.details.youtubeVideo != null) {
            displayYoutubePreview()
        }

        if (!sale.isUpcoming) {
            scroll_view.post {
                initChart()
                updateChart()
            }
        }

        displayChangeableSaleInfo()
    }

    private fun displayChangeableSaleInfo() {
        SaleProgressWrapper(scroll_view).displayProgress(sale)
    }

    private fun displayAssetDetails() {
        saleAsset.details.logo.getUrl(urlConfigProvider.getConfig().storage)?.let {
            Picasso.with(this)
                    .load(it)
                    .resizeDimen(R.dimen.asset_list_item_logo_size, R.dimen.asset_list_item_logo_size)
                    .centerInside()
                    .transform(CircleTransform())
                    .into(asset_logo_image_view)
        } ?: displayGeneratedLogo()
    }

    private fun displayGeneratedLogo() {
        val logoSize = resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)

        asset_logo_image_view.setImageBitmap(
                LogoFactory(this).getWithAutoBackground(
                        saleAsset.code,
                        logoSize
                )
        )
    }

    private fun displayYoutubePreview() {
        video_preview_layout.visibility = View.VISIBLE

        video_preview_image_view.post {
            val width = video_preview_layout.width
            val height = (width * 720f / 1280f).toInt()

            video_preview_image_view.layoutParams = RelativeLayout.LayoutParams(width, height)

            Picasso.with(this)
                    .load(sale.details.youtubeVideoPreviewImage)
                    .placeholder(ColorDrawable(ContextCompat.getColor(this,
                            R.color.saleImagePlaceholder)))
                    .resize(width, height)
                    .centerCrop()
                    .into(video_preview_image_view)
        }

        video_preview_layout.onClick {
            sale.details.getYoutubeVideoUrl(mobile = true)
                    ?.also { url ->
                        browse(url)
                    }
        }
    }
    // endregion

    // region Invest
    private fun initInvestIfNeeded() {
        if (sale.isAvailable) {
            if (saleAsset.policy and AssetPolicy.REQUIRES_KYC.value == AssetPolicy.REQUIRES_KYC.value &&
                    accountRepository.itemSubject.value.typeI == AccountType.NOT_VERIFIED.value) {
                displayKycRequired()
            } else {
                initInvest()
                sale_invest_card.post {
                    if (sale_invest_card.visibility != View.VISIBLE) {
                        AnimationUtil.fadeInView(sale_invest_card)
                    }
                }
            }
        } else {
            sale_invest_card.visibility = View.GONE
        }
    }

    private fun initInvest() {
        amount_edit_text.isHelperTextAlwaysShown = true

        investAmountWrapper.onAmountChanged { _, _ ->
            updateInvestHelperAndError()
            updateInvestAvailability()
        }

        asset_spinner.setSimpleItems(sale.quoteAssets.map { it.code },
                sale.quoteAssets.indexOfFirst {
                    (existingOffers[it.code]?.quoteAmount?.signum() ?: 0) > 0
                })
        asset_spinner.onItemSelected {
            investAsset = it.text
        }

        invest_button.onClick {
            tryToInvest()
        }

        cancel_investment_button.onClick {
            tryToInvest(cancel = true)
        }
    }

    private fun updateInvestAvailability() {
        canInvest = (receiveAmount.signum() > 0
                || existingOffers[investAsset] != null)
                && !amount_edit_text.hasError()
                && !investLoading.isLoading
                && !mainLoading.isLoading

        updateInvestButton()
        updateCancelInvestmentButton()
    }

    private fun updateInvestButton() {
        if (existingOffers.containsKey(investAsset)) {
            invest_button.setText(R.string.update_investment_action)
        } else {
            invest_button.setText(R.string.invest_action)
        }
    }

    private fun updateCancelInvestmentButton() {
        if (existingOffers.containsKey(investAsset)) {
            cancel_investment_button.visibility = View.VISIBLE
        } else {
            cancel_investment_button.visibility = View.GONE
        }
    }

    private fun onInvestAssetChanged() {
        displayExistingInvestmentAmount()
        updateInvestLimit()
        updateInvestHelperAndError()
        updateInvestAvailability()
    }

    private fun updateInvestLimit() {
        maxInvestAmount = investmentInfoManager.getMaxInvestmentAmount(
                investAsset,
                sale,
                existingOffers,
                maxFees
        )
    }

    private fun updateInvestHelperAndError() {
        if (investAmountWrapper.scaledAmount > maxInvestAmount) {
            amount_edit_text.error = getString(R.string.template_sale_max_investment,
                    AmountFormatter.formatAssetAmount(maxInvestAmount),
                    investAsset)
        } else {
            amount_edit_text.error = null
            amount_edit_text.setHelperText(
                    getString(R.string.template_available,
                            AmountFormatter.formatAssetAmount(getAvailableBalance(investAsset)),
                            investAsset)
            )
        }
    }

    private fun displayExistingInvestmentAmount() {
        existingInvestmentAmount.also {
            if (it.signum() > 0) {
                amount_edit_text.setText(BigDecimalUtil.toPlainString(it))
                amount_edit_text.setSelection(amount_edit_text.text.length)
            } else {
                amount_edit_text.text.clear()
            }
        }
    }

    private fun getAvailableBalance(asset: String): BigDecimal {
        return investmentInfoManager.getAvailableBalance(asset, existingOffers)
    }
    // endregion

    // region Chart
    private fun initChart() {
        asset_chart.apply {
            val quoteAsset = sale.defaultQuoteAsset

            applyTouchHook(scroll_view)
            this.asset = quoteAsset
            valueHint = getString(R.string.deployed_hint)
            total = sale.currentCap

            setLimitLines(listOf(
                    sale.softCap.toFloat() to
                            "${AmountFormatter.formatAssetAmount(sale.softCap,
                                    quoteAsset)} $quoteAsset",
                    sale.hardCap.toFloat() to
                            "${AmountFormatter.formatAssetAmount(sale.hardCap,
                                    quoteAsset)} $quoteAsset"
            ))

            post {
                AnimationUtil.fadeInView(asset_chart_card)
            }
        }
    }

    private var updateChartDisposable: Disposable? = null
    private fun updateChart() {
        updateChartDisposable?.dispose()
        updateChartDisposable = investmentInfoManager
                .getChart(apiProvider.getApi())
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    asset_chart.isLoading = true
                }
                .doOnEvent { _, _ ->
                    asset_chart.isLoading = false
                }
                .subscribeBy(
                        onSuccess = { data ->
                            asset_chart.post {
                                asset_chart.data = data
                            }
                        },
                        onError = { error ->
                            errorHandlerFactory.getDefault().handle(error)
                        }
                )
                .addTo(compositeDisposable)
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
        val amount = investAmountWrapper.scaledAmount
        val receiveAmount = receiveAmount
        val price = currentPrice
        val orderBookId = sale.id
        val orderId = if (cancel) existingOffers[asset]?.id ?: return else 0L

        val getNewOffer =
                if (cancel)
                    Single.just(
                            Offer(
                                    baseAsset = sale.baseAsset,
                                    quoteAsset = asset,
                                    price = price,
                                    isBuy = true,
                                    orderBookId = orderBookId,
                                    id = orderId
                            )
                    )
                else
                    PrepareOfferUseCase(
                            Offer(
                                    baseAsset = sale.baseAsset,
                                    baseAmount = receiveAmount,
                                    quoteAsset = asset,
                                    quoteAmount = amount,
                                    price = price,
                                    isBuy = true,
                                    orderBookId = sale.id
                            ),
                            walletInfoProvider,
                            feeManager
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
                                            offerToCancel = existingOffers[investAsset],
                                            assetName = sale.baseAsset,
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
    // endregion

    // region Favorites
    private fun subscribeToFavorites() {
        favoritesRepository
                .itemsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    isFavorited = getFavoriteEntry() != null
                }
                .addTo(compositeDisposable)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sale, menu)
        menu?.findItem(R.id.follow)
                ?.apply {
                    if (isFavorited) {
                        icon = ContextCompat
                                .getDrawable(this@SaleActivity, R.drawable.ic_star)
                        title = getString(R.string.remove_from_favorites_action)
                    } else {
                        icon = ContextCompat
                                .getDrawable(this@SaleActivity, R.drawable.ic_star_outline)
                        title = getString(R.string.add_to_favorites_action)
                    }
                }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.follow -> switchFavorite()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getFavoriteEntry(): FavoriteEntry? {
        return favoritesRepository.itemsSubject.value.find {
            it.type == SaleFavoriteEntry.TYPE && it.key == sale.baseAsset
        }
    }

    private var switchFavoriteDisposable: Disposable? = null
    private fun switchFavorite() {
        switchFavoriteDisposable?.dispose()
        switchFavoriteDisposable =
                SwitchFavoriteUseCase(
                        SaleFavoriteEntry(sale.baseAsset),
                        favoritesRepository
                )
                        .perform()
                        .compose(ObservableTransformers.defaultSchedulersCompletable())
                        .doOnSubscribe { mainLoading.show("favorite") }
                        .subscribeBy(
                                onError = {
                                    errorHandlerFactory.getDefault().handle(it)
                                    mainLoading.hide("favorite")
                                    updateInvestAvailability()
                                },
                                onComplete = {
                                    mainLoading.hide("favorite")
                                    updateInvestAvailability()
                                }
                        )
                        .addTo(compositeDisposable)
    }
    // endregion

    private fun displayKycRequired() {
        displaySaleUnavailable(R.drawable.ic_security, getString(R.string.verification_required),
                getString(R.string.sale_verifictaion_required_details),
                View.OnClickListener {
                    browse(urlConfigProvider.getConfig().kyc, true)
                },
                getString(R.string.go_to_verification))
    }

    private fun displaySaleUnavailable(@DrawableRes iconResId: Int,
                                       reason: String,
                                       details: String,
                                       buttonClickListener: View.OnClickListener? = null,
                                       buttonText: String = getString(R.string.details)) {
        sale_unavailable_card.post {
            sale_unavailable_icon.setImageDrawable(ContextCompat.getDrawable(this, iconResId))
            sale_unavailable_reason_text_view.text = reason
            sale_unavailable_details_text_view.text = details

            if (buttonClickListener != null) {
                sale_unavailable_details_button.apply {
                    visibility = View.VISIBLE
                    setOnClickListener(buttonClickListener)
                    (this as Button).text = buttonText
                }
            } else {
                sale_unavailable_details_button.visibility = View.GONE
            }

            AnimationUtil.fadeInView(sale_unavailable_card)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                INVESTMENT_REQUEST -> {
                    setResult(Activity.RESULT_OK)
                    updateFinancial()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fileDownloader.handlePermissionResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val INVESTMENT_REQUEST = "invest".hashCode() and 0xffff
        const val SALE_JSON_EXTRA = "sale_json"
    }
}
