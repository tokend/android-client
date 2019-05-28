package org.tokend.template.features.send.amount.view

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_amount_input.*
import kotlinx.android.synthetic.main.layout_payment_description.*
import kotlinx.android.synthetic.main.layout_payment_description.view.*
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.features.amountscreen.view.AmountInputFragment
import org.tokend.template.features.send.amount.model.PaymentAmountAndDescription
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog
import org.tokend.wallet.Base32Check

class PaymentAmountAndDescriptionFragment : AmountInputFragment() {
    private val recipient: String?
        get() = arguments?.getString(RECIPIENT_EXTRA)

    private var canContinue: Boolean = false
        set(value) {
            field = value
            action_button.isEnabled = value
        }

    override fun getTitleText(): String? {
        val recipient = recipient ?: return null

        val displayRecipient =
                if (Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                                recipient.toCharArray()))
                    recipient.substring(0..3) + "..." +
                            recipient.substring(recipient.length - 5, recipient.length - 1)
                else
                    recipient

        return getString(R.string.template_tx_to, displayRecipient)
    }

    override fun getActionButtonText(): String {
        return getString(R.string.go_to_confirmation_btn_label)
    }

    override fun getAssetsToDisplay(): Collection<String> {
        return balancesRepository
                .itemsList
                .sortedWith(balanceComparator)
                .map(BalanceRecord::asset)
                .filter(AssetRecord::isTransferable)
                .map(AssetRecord::code)
    }

    override fun getBalancePicker(): BalancePickerBottomDialog {
        return BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                balanceComparator,
                balancesRepository
        ) { balance ->
            balance.asset.isTransferable
        }
    }

    override fun getExtraView(parent: ViewGroup): View? {
        val view = requireContext().layoutInflater
                .inflate(R.layout.layout_payment_description, parent, false)

        view.payment_description_edit_text.onEditorAction {
            postResult()
        }

        return view
    }

    override fun postResult() {
        if (!canContinue) {
            return
        }

        val amount = amountWrapper.scaledAmount
        val description = payment_description_edit_text.text
                .toString()
                .trim()
                .takeIf { it.isNotEmpty() }

        resultSubject.onNext(
                PaymentAmountAndDescription(
                        amount = amount,
                        assetCode = asset,
                        description = description
                )
        )
    }

    override fun updateActionButtonAvailability() {
        checkAmount()

        canContinue = !hasError
                && amountWrapper.scaledAmount.signum() > 0
    }

    companion object {
        private const val RECIPIENT_EXTRA = "recipient"

        fun newInstance(recipient: String,
                        requiredAsset: String? = null): PaymentAmountAndDescriptionFragment {
            val fragment = PaymentAmountAndDescriptionFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, requiredAsset)
                putString(RECIPIENT_EXTRA, recipient)
            }
            return fragment
        }
    }
}