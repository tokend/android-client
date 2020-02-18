package org.tokend.template.features.assets.buy.view.quoteasset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_atomic_swap_quote_asset.*
import org.tokend.template.R
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.assets.buy.view.quoteasset.picker.AtomicSwapQuoteAssetSpinnerItem
import org.tokend.template.features.assets.buy.view.quoteasset.picker.AtomicSwapQuoteAssetsSpinnerAdapter
import org.tokend.template.fragments.BaseFragment
import java.math.BigDecimal

class AtomicSwapQuoteAssetFragment : BaseFragment() {
    private lateinit var ask: AtomicSwapAskRecord
    private lateinit var amount: BigDecimal

    private val resultSubject = PublishSubject.create<Asset>()

    val resultObservable: Observable<Asset>
        get() = resultSubject

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_atomic_swap_quote_asset, container, false)
    }

    override fun onInitAllowed() {
        this.ask = arguments?.getSerializable(ASK_EXTRA) as? AtomicSwapAskRecord
                ?: throw IllegalArgumentException("No $ASK_EXTRA specified")
        this.amount = (arguments?.getSerializable(AMOUNT_EXTRA) as? BigDecimal)
                ?: throw IllegalArgumentException("No $AMOUNT_EXTRA specified")

        initTitle()
        initQuoteAssets()
        initButton()
    }

    private fun initTitle() {
        amount_text_view.text = getString(
                R.string.template_buy_asset_code,
                amountFormatter.formatAssetAmount(
                        amount,
                        ask.asset
                )
        )
    }

    private fun initQuoteAssets() {
        quote_assets_spinner.itemsAdapter =
                AtomicSwapQuoteAssetsSpinnerAdapter(requireContext(), amountFormatter)
        quote_assets_spinner.setItems(
                ask.quoteAssets.map { AtomicSwapQuoteAssetSpinnerItem(it, amount) }
        )
    }

    private fun initButton() {
        continue_button.setOnClickListener {
            postResult()
        }
    }

    private fun postResult() {
        quote_assets_spinner.selectedItem?.asset?.also(resultSubject::onNext)
    }

    companion object {
        private const val ASK_EXTRA = "ask"
        private const val AMOUNT_EXTRA = "amount"

        fun getBundle(ask: AtomicSwapAskRecord,
                      amount: BigDecimal) = Bundle().apply {
            putSerializable(ASK_EXTRA, ask)
            putSerializable(AMOUNT_EXTRA, amount)
        }

        fun newInstance(bundle: Bundle): AtomicSwapQuoteAssetFragment =
                AtomicSwapQuoteAssetFragment().withArguments(bundle)
    }
}