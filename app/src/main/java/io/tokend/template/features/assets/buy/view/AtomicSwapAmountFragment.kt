package io.tokend.template.features.assets.buy.view

import android.os.Bundle
import io.tokend.template.R
import io.tokend.template.data.model.AtomicSwapAskRecord
import io.tokend.template.extensions.withArguments
import io.tokend.template.features.amountscreen.view.AmountInputFragment
import kotlinx.android.synthetic.main.fragment_amount_input.*

class AtomicSwapAmountFragment : AmountInputFragment() {
    private lateinit var ask: AtomicSwapAskRecord

    override val requiredAssetCode: String?
        get() = ask.asset.code

    override fun onInitAllowed() {
        this.ask = arguments?.getSerializable(ASK_EXTRA) as? AtomicSwapAskRecord
            ?: throw IllegalArgumentException("No $ASK_EXTRA specified")

        displayAvailableAmount()
        super.onInitAllowed()
        onPickedItemChanged()
    }

    override fun initSelection() {
        asset_code_text_view.background = null
    }

    override fun getTitleText(): String? = null

    override fun displayBalance() {}

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