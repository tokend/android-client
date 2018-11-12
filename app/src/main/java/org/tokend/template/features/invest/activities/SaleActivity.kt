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
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import kotlinx.android.synthetic.main.activity_sale.*
import kotlinx.android.synthetic.main.layout_amount_with_spinner.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.accounts.params.OffersParams
import org.tokend.sdk.api.favorites.model.FavoriteEntry
import org.tokend.sdk.api.favorites.model.SaleFavoriteEntry
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.logic.FeeManager
import org.tokend.template.base.logic.repository.AccountRepository
import org.tokend.template.base.logic.repository.assets.AssetsRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.repository.favorites.FavoritesRepository
import org.tokend.template.base.view.AmountEditTextWrapper
import org.tokend.template.base.view.ContentLoadingProgressBar
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.AnimationUtil
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.extensions.Sale
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.toSingle
import org.tokend.template.features.explore.AssetLogoFactory
import org.tokend.template.features.invest.SwitchFavoriteUseCase
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.invest.view.SaleProgressWrapper
import org.tokend.template.features.trade.PrepareOfferUseCase
import org.tokend.template.features.trade.repository.offers.OffersRepository
import org.tokend.template.util.FileDownloader
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
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

    private val salesRepository: SalesRepository
        get() = repositoryProvider.sales()

    private val offersRepository: OffersRepository
        get() = repositoryProvider.offers()

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val favoritesRepository: FavoritesRepository
        get() = repositoryProvider.favorites()

    private val assetsRepository: AssetsRepository
        get() = repositoryProvider.assets()

    private lateinit var feeManager: FeeManager

    private lateinit var sale: Sale
    private lateinit var saleAsset: org.tokend.template.extensions.Asset

    private var isFavorited = false
        set(value) {
            field = value
            invalidateOptionsMenu()
        }

    private var existingOffers: Map<String, Offer>? = null
    private var maxFees: MutableMap<String, BigDecimal> = mutableMapOf()
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
        get() = existingOffers?.get(investAsset)?.quoteAmount ?: BigDecimal.ZERO

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

        try {
            sale = GsonFactory().getBaseGson().fromJson(
                    intent.getNullableStringExtra(SALE_JSON_EXTRA),
                    Sale::class.java)

            displaySaleInfo()
            update()
        } catch (e: JsonSyntaxException) {
            finish()
            return
        } finally {
            supportStartPostponedEnterTransition()
        }

        initButtons()
        initFields()

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

    private var saleDisposable: Disposable? = null
    private fun update() {
        val getAssetDetails =
                assetsRepository
                        .getSingle(sale.baseAsset)
                        .compose(ObservableTransformers.defaultSchedulersSingle())

        val getOffers =
                if (sale.isAvailable)
                    offersRepository.getPage(
                            OffersParams(
                                    orderBookId = sale.id,
                                    onlyPrimary = true,
                                    baseAsset = null,
                                    quoteAsset = null,
                                    isBuy = true
                            )
                    )
                            .map {
                                it.items
                            }
                else
                    Single.just(emptyList())

        val getDetailedSale =
                if (sale.isAvailable)
                    salesRepository.getSingle(sale.id)
                else
                    Single.just(sale)

        // Has to be created when requested.
        val getMaxFees = {
            if (sale.isAvailable) {
                walletInfoProvider.getWalletInfo()?.accountId.toMaybe()
                        .switchIfEmpty(Single.error<String>(IllegalStateException("No wallet info found")))
                        .flatMap { accountId ->
                            Observable.merge(
                                    sale.quoteAssets.mapNotNull {
                                        val availableBalance = getAvailableBalance(it.code)

                                        return@mapNotNull if (availableBalance.signum() == 0)
                                            null
                                        else
                                            feeManager.getOfferFee(accountId, it.code, availableBalance)
                                                    .map {
                                                        val percent = it.percent
                                                        maxFees[it.asset] = percent
                                                        percent
                                                    }
                                                    .onErrorResumeNext(Single.just(BigDecimal.ZERO))
                                                    .toObservable()
                                    }
                            ).last(BigDecimal.ZERO)
                        }
            } else {
                Single.just(BigDecimal.ZERO)
            }
        }

        saleDisposable?.dispose()
        saleDisposable =
                balancesRepository.updateIfNotFreshDeferred()
                        .andThen(
                                Single.zip(
                                        getAssetDetails,
                                        getOffers,
                                        getDetailedSale,
                                        Function3 { asset: org.tokend.template.extensions.Asset,
                                                    offers: List<Offer>, sale: Sale ->
                                            Triple(offers, sale, asset)
                                        }
                                )
                        )
                        .flatMap { offersSalePair ->
                            maxFees.clear()
                            getMaxFees().map { offersSalePair }
                        }
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
                                onSuccess = { (offers, sale, asset) ->
                                    this.sale = sale
                                    this.saleAsset = asset
                                    this.existingOffers = offers.associateBy { it.quoteAsset }

                                    displayChangeableSaleInfo()
                                    displayAssetDetails()
                                    initInvestIfNeeded()
                                    displayExistingInvestmentAmount()
                                    updateInvestLimit()
                                    updateInvestHelperAndError()
                                    updateInvestAvailability()
                                },
                                onError = {
                                    errorHandlerFactory.getDefault().handle(it)
                                }
                        )
                        .addTo(compositeDisposable)
    }

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
                    .into(asset_logo_image_view)
        } ?: displayGeneratedLogo()
    }

    private fun displayGeneratedLogo() {
        val logoSize = resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)

        asset_logo_image_view.setImageBitmap(AssetLogoFactory(this)
                .getForCode(saleAsset.code, logoSize))
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
            sale.details.getYoutubeVideoUrl(mobile = true)?.also { browse(it) }
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
                    (existingOffers?.get(it.code)?.quoteAmount?.signum() ?: 0) > 0
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
        canInvest = existingOffers != null
                && (receiveAmount.signum() > 0
                || existingOffers?.get(investAsset) != null)
                && !amount_edit_text.hasError()
                && !investLoading.isLoading
                && !mainLoading.isLoading

        updateCancelInvestmentButton()
    }

    private fun updateCancelInvestmentButton() {
        if (existingOffers?.containsKey(investAsset) == true) {
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
        val investAssetDetails = sale.quoteAssets.find { it.code == investAsset }
        val maxByHardCap = (investAssetDetails?.hardCap ?: BigDecimal.ZERO)
                .subtract(investAssetDetails?.totalCurrentCap ?: BigDecimal.ZERO)
                .add(existingInvestmentAmount)

        val maxByBalance = getAvailableBalance(investAsset)
                .minus(maxFees[investAsset] ?: BigDecimal.ZERO)

        maxInvestAmount =
                BigDecimalUtil.scaleAmount(maxByBalance.min(maxByHardCap),
                        AmountFormatter.getDecimalDigitsCount(investAsset))
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
        val offer = existingOffers?.get(asset)
        val locked = (offer?.quoteAmount ?: BigDecimal.ZERO).add(offer?.fee ?: BigDecimal.ZERO)

        val assetBalance = balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.balance ?: BigDecimal.ZERO

        return locked + assetBalance
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
        updateChartDisposable = apiProvider.getApi().assets.getChart(sale.baseAsset).toSingle()
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
        val orderId = if (cancel) existingOffers?.get(asset)?.id ?: return else 0L

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
                                    Navigator.openOfferConfirmation(this,
                                            INVESTMENT_REQUEST,
                                            offer = offer,
                                            offerToCancel = existingOffers?.get(investAsset),
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
                        .subscribeBy(
                                onError = { errorHandlerFactory.getDefault().handle(it) }
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
                    update()
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
