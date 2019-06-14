package org.tokend.template.features.assets.buy.view

import android.os.Bundle
import kotlinx.android.synthetic.main.fragment_amount_input.*
import org.tokend.template.R
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.features.amountscreen.view.AmountInputFragment

class AtomicSwapAmountFragment : AmountInputFragment() {
    private lateinit var ask: AtomicSwapAskRecord

    override fun onInitAllowed() {
        val askId = arguments?.getString(ASK_ID_EXTRA)
        val ask = repositoryProvider.atomicSwapAsks(requestedAsset!!)
                .itemsList
                .find { it.id == askId }
                ?: throw IllegalArgumentException("No ask found for ID $askId from $ASK_ID_EXTRA")
        this.ask = ask
        displayAvailableAmount()

        super.onInitAllowed()
    }

    override fun initAssetSelection() {
        asset_code_text_view.background = null
    }

    override fun getAssetsToDisplay(): Collection<String> {
        return listOf(ask.asset.code)
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
        private const val ASK_ID_EXTRA = "ask_id"

        fun newInstance(assetCode: String,
                        askId: String): AtomicSwapAmountFragment {
            val fragment = AtomicSwapAmountFragment()
            fragment.arguments = Bundle().apply {
                putString(ASK_ID_EXTRA, askId)
                putString(ASSET_EXTRA, assetCode)
            }
            return fragment
        }
    }
}