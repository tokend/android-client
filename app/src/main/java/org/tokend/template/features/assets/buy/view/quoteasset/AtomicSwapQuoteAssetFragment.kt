package org.tokend.template.features.assets.buy.view.quoteasset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_atomic_swap_quote_asset.*
import org.tokend.template.R
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.AtomicSwapAskRecord
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
        val assetCode = arguments?.getString(ASSET_CODE_EXTRA)
                ?: throw IllegalArgumentException("No $ASSET_CODE_EXTRA specified")
        val askId = arguments?.getString(ASK_ID_EXTRA)
        this.ask = repositoryProvider.atomicSwapAsks(assetCode)
                .itemsList
                .find { it.id == askId }
                ?: throw IllegalArgumentException("No ask found for ID $askId from $ASK_ID_EXTRA")
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
        private const val ASSET_CODE_EXTRA = "asset_code"
        private const val ASK_ID_EXTRA = "ask_id"
        private const val AMOUNT_EXTRA = "amount"

        fun newInstance(assetCode: String,
                        askId: String,
                        amount: BigDecimal): AtomicSwapQuoteAssetFragment {
            val fragment = AtomicSwapQuoteAssetFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_CODE_EXTRA, assetCode)
                putString(ASK_ID_EXTRA, askId)
                putSerializable(AMOUNT_EXTRA, amount)
            }
            return fragment
        }
    }
}