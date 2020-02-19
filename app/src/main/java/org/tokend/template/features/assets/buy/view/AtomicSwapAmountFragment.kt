package org.tokend.template.features.assets.buy.view

import android.os.Bundle
import kotlinx.android.synthetic.main.fragment_amount_input.*
import org.tokend.template.R
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.amountscreen.view.AmountInputFragment

class AtomicSwapAmountFragment : AmountInputFragment() {
    private lateinit var ask: AtomicSwapAskRecord

    override val requestedAsset: String?
        get() = ask.asset.code

    override fun onInitAllowed() {
        this.ask = arguments?.getSerializable(ASK_EXTRA) as? AtomicSwapAskRecord
                ?: throw IllegalArgumentException("No $ASK_EXTRA specified")

        displayAvailableAmount()
        super.onInitAllowed()
        onAssetChanged()
    }

    override fun initAssetSelection() {
        asset_code_text_view.background = null
    }

    override fun getTitleText(): String? = null

    override fun displayBalance() { }

    private fun displayAvailableAmount() {
        balance_text_view.text = getString(
                R.string.template_amount_available_for_buy,
                amountFormatter.formatAssetAmount(ask.amount, ask.asset, withAssetCode = false)
        )
    }

    override fun checkAmount() {
        val availableExceeded = amountWrapper.scaledAmount > ask.amount

        when {
            availableExceeded ->
                setError(getString(R.string.error_too_big_amount))
            else ->
                setError(null)
        }
    }

    companion object {
        private const val ASK_EXTRA = "ask"

        fun getBundle(ask: AtomicSwapAskRecord) = Bundle().apply {
            putSerializable(ASK_EXTRA, ask)
        }

        fun newInstance(bundle: Bundle): AtomicSwapAmountFragment =
                AtomicSwapAmountFragment().withArguments(bundle)
    }
}