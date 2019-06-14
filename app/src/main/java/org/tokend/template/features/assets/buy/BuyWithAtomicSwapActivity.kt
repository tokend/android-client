package org.tokend.template.features.assets.buy

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.MenuItem
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.features.amountscreen.model.AmountInputResult
import org.tokend.template.features.assets.buy.view.AtomicSwapAmountFragment
import org.tokend.template.features.assets.buy.view.quoteasset.AtomicSwapQuoteAssetFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.ProgressDialogFactory
import java.math.BigDecimal

class BuyWithAtomicSwapActivity : BaseActivity() {
    private lateinit var ask: AtomicSwapAskRecord
    private var amount: BigDecimal = BigDecimal.ZERO
    private var asset: Asset? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.fragment_user_flow)

        val errorHandler = errorHandlerFactory.getDefault()

        val assetCode = intent.getStringExtra(ASSET_CODE_EXTRA)
        if (assetCode == null) {
            errorHandler.handle(
                    IllegalArgumentException("No $ASSET_CODE_EXTRA specified")
            )
            finish()
            return
        }
        val askId = intent.getStringExtra(ASK_ID_EXTRA)
        val ask = repositoryProvider.atomicSwapAsks(assetCode)
                .itemsList
                .find { it.id == askId }
        if (ask == null) {
            errorHandler.handle(
                    IllegalArgumentException("No ask found for ID $askId from $ASK_ID_EXTRA")
            )
            finish()
            return
        }
        this.ask = ask

        initToolbar()

        toAmountScreen()
    }

    // region Init
    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.template_buy_asset_code, ask.asset.code)
    }
    // endregion

    private fun toAmountScreen() {
        val fragment = AtomicSwapAmountFragment.newInstance(ask.asset.code, ask.id)
        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .map { it as AmountInputResult }
                .map(AmountInputResult::amount)
                .subscribeBy(
                        onNext = this::onAmountEntered,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
        displayFragment(fragment, "amount", null)
    }

    private fun onAmountEntered(amount: BigDecimal) {
        this.amount = amount
        toQuoteAssetScreen()
    }

    private fun toQuoteAssetScreen() {
        val fragment = AtomicSwapQuoteAssetFragment.newInstance(ask.asset.code, ask.id, amount)
        fragment
                .resultObservable
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onQuoteAssetSelected,
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
                .addTo(compositeDisposable)
        displayFragment(fragment, "quote-asset", true)
    }

    private fun onQuoteAssetSelected(asset: Asset) {
        this.asset = asset
        toastManager.short("Buy $amount ${ask.asset} for ${asset.code}")
        submitBid()
    }

    private fun submitBid() {
        var disposable: Disposable? = null

        val progress = ProgressDialogFactory.getTunedDialog(this)
        progress.setCancelable(true)
        progress.setOnCancelListener { disposable?.dispose() }

        disposable = Single
                .never<Boolean>()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe { progress.show() }
                .doOnEvent { _, _ -> progress.dismiss() }
                .subscribe()
                .addTo(compositeDisposable)
    }

    private fun displayFragment(
            fragment: Fragment,
            tag: String,
            forward: Boolean?
    ) {
        supportFragmentManager.beginTransaction()
                .setTransition(
                        when (forward) {
                            true -> FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                            false -> FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
                            null -> FragmentTransaction.TRANSIT_NONE
                        }
                )
                .replace(R.id.fragment_container_layout, fragment)
                .addToBackStack(tag)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    companion object {
        const val ASSET_CODE_EXTRA = "asset_code"
        const val ASK_ID_EXTRA = "ask_id"
    }
}