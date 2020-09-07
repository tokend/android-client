package org.tokend.template.features.assets.buy

import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.MenuItem
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_user_flow.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.features.balances.storage.BalancesRepository
import org.tokend.template.features.amountscreen.model.AmountInputResult
import org.tokend.template.features.assets.buy.logic.CreateAtomicSwapBidUseCase
import org.tokend.template.features.assets.buy.model.AtomicSwapInvoice
import org.tokend.template.features.assets.buy.view.AtomicSwapAmountFragment
import org.tokend.template.features.assets.buy.view.quoteasset.AtomicSwapQuoteAssetFragment
import org.tokend.template.logic.TxManager
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.ProgressDialogFactory
import org.tokend.template.view.util.UserFlowFragmentDisplayer
import java.math.BigDecimal

class BuyWithAtomicSwapActivity : BaseActivity() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val fragmentDisplayer =
            UserFlowFragmentDisplayer(this, R.id.fragment_container_layout)

    private lateinit var ask: AtomicSwapAskRecord
    private var amount: BigDecimal = BigDecimal.ZERO
    private var asset: Asset? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.fragment_user_flow)

        val assetCode = intent.getStringExtra(ASSET_CODE_EXTRA)
        if (assetCode == null) {
            finishWithMissingArgError(ASSET_CODE_EXTRA)
            return
        }
        val askId = intent.getStringExtra(ASK_ID_EXTRA)
        val ask = repositoryProvider.atomicSwapAsks(assetCode)
                .itemsList
                .find { it.id == askId }
        if (ask == null) {
            finishWithError(IllegalArgumentException("No ask found for ID $askId from $ASK_ID_EXTRA"))
            return
        }
        this.ask = ask

        initToolbar()
        initSwipeRefresh()
        subscribeToBalances()
        toAmountScreen()
    }

    // region Init
    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.template_buy_asset_code, ask.asset.code)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener { balancesRepository.update() }
    }
    // endregion

    private fun toAmountScreen() {
        val fragment = AtomicSwapAmountFragment.newInstance(
                AtomicSwapAmountFragment.getBundle(ask)
        )

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .map(AmountInputResult::amount)
                .subscribeBy(
                        onNext = this::onAmountEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
        fragmentDisplayer.display(fragment, "amount", null)
    }

    private fun onAmountEntered(amount: BigDecimal) {
        this.amount = amount
        toQuoteAssetScreen()
    }

    private fun toQuoteAssetScreen() {
        val fragment = AtomicSwapQuoteAssetFragment.newInstance(
                AtomicSwapQuoteAssetFragment.getBundle(ask, amount)
        )

        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onQuoteAssetSelected,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
        fragmentDisplayer.display(fragment, "quote-asset", true)
    }

    private fun onQuoteAssetSelected(asset: Asset) {
        this.asset = asset
        submitBid()
    }

    private fun submitBid() {
        val assetCode = asset?.code
                ?: return

        var disposable: Disposable? = null

        val progress = ProgressDialogFactory.getDialog(
                this,
                cancelListener = { disposable?.dispose() }
        )

        disposable = CreateAtomicSwapBidUseCase(
                amount = amount,
                quoteAssetCode = assetCode,
                ask = ask,
                repositoryProvider = repositoryProvider,
                walletInfoProvider = walletInfoProvider,
                accountProvider = accountProvider,
                apiProvider = apiProvider,
                txManager = TxManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe { progress.show() }
                .doOnEvent { _, _ -> progress.dismiss() }
                .subscribeBy(
                        onSuccess = this::onBidSubmitted,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
    }

    private fun onBidSubmitted(invoice: AtomicSwapInvoice) {
        val asset = this.asset ?: return
        val sendAmountString = amountFormatter.formatAssetAmount(invoice.amount, asset)
        val receiveAmountString = amountFormatter.formatAssetAmount(amount, ask.asset)

        Navigator.from(this).openQrShare(
                title = this.title.toString(),
                shareLabel = getString(R.string.share_address_label),
                data = invoice.address,
                shareText = getString(
                        R.string.template_atomic_swap_invoice_share_text,
                        sendAmountString,
                        receiveAmountString,
                        invoice.address
                ),
                topText = getString(R.string.template_send_to_address, sendAmountString)
        )

        finish()
    }

    private fun subscribeToBalances() {
        balancesRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it, "balances") }
                .addTo(compositeDisposable)

        balancesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    errorHandlerFactory.getDefault().handle(it)
                }
                .addTo(compositeDisposable)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!fragmentDisplayer.tryPopBackStack()) {
            finish()
        }
    }

    companion object {
        private const val ASSET_CODE_EXTRA = "asset_code"
        private const val ASK_ID_EXTRA = "ask_id"

        fun getBundle(assetCode: String,
                      askId: String) = Bundle().apply {
            putString(ASSET_CODE_EXTRA, assetCode)
            putString(ASK_ID_EXTRA, askId)
        }
    }
}