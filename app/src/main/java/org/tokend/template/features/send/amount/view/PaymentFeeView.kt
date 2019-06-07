package org.tokend.template.features.send.amount.view

import android.content.Context
import android.support.v7.widget.SwitchCompat
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.data.model.Asset
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

/**
 * View for sender and recipient fees display
 * with ability to pay recipient's fee
 */
class PaymentFeeView
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val feesLayout: ViewGroup
    private val feeTextView: TextView
    private val recipientFeeLayout: ViewGroup
    private val payRecipientFeeSwitch: SwitchCompat
    private val loadingProgress: View

    private var senderFee: BigDecimal = BigDecimal.ZERO
    private var recipientFee: BigDecimal = BigDecimal.ZERO
    private val senderFeeTotal: BigDecimal
        get() = if (!isPayingRecipientFee) senderFee else senderFee + recipientFee
    private var asset: Asset? = null
    private var amountFormatter: AmountFormatter? = null

    init {
        context.layoutInflater.inflate(R.layout.layout_payment_fee, this, true)

        feesLayout = findViewById(R.id.fees_layout)
        feeTextView = findViewById(R.id.sender_fee_text_view)
        recipientFeeLayout = findViewById(R.id.recipient_fee_layout)
        payRecipientFeeSwitch = findViewById(R.id.pay_recipient_fee_switch)
        loadingProgress = findViewById(R.id.fees_loading_progress)

        feesLayout.visibility = View.GONE
        loadingProgress.visibility = View.GONE

        payRecipientFeeSwitch.setOnCheckedChangeListener { _, isChecked ->
            payRecipientFeeChangedListener?.invoke(isChecked)
            displayData()
        }
    }

    var isLoading: Boolean = false
        set(value) {
            field = value

            if (value) {
                feesLayout.visibility = View.INVISIBLE
                loadingProgress.visibility = View.VISIBLE
            } else {
                feesLayout.visibility = View.VISIBLE
                loadingProgress.visibility = View.GONE
            }
        }

    val isPayingRecipientFee: Boolean
        get() = payRecipientFeeSwitch.isChecked

    private var payRecipientFeeChangedListener: ((Boolean) -> Unit)? = null

    fun onPayRecipientFeeChanged(listener: (Boolean) -> Unit) {
        this.payRecipientFeeChangedListener = listener
    }

    fun setFees(sender: BigDecimal,
                recipient: BigDecimal,
                asset: Asset,
                amountFormatter: AmountFormatter) {
        this.senderFee = sender
        this.recipientFee = recipient
        this.asset = asset
        this.amountFormatter = amountFormatter

        displayData()
    }

    private fun displayData() {
        val amountFormatter = this.amountFormatter
                ?: return
        val asset = this.asset
                ?: return

        if (senderFeeTotal.signum() == 0 && recipientFee.signum() == 0) {
            feeTextView.text = context.getString(R.string.no_fee)
            recipientFeeLayout.visibility = View.GONE
            return
        } else if (senderFeeTotal.signum() == 0 && recipientFee.signum() > 0) {
            feeTextView.text = context.getString(
                    R.string.template_recipients_fee,
                    amountFormatter.formatAssetAmount(
                            recipientFee,
                            asset
                    )
            )
            recipientFeeLayout.visibility = View.VISIBLE
        } else if (senderFeeTotal.signum() > 0 && recipientFee.signum() == 0) {
            feeTextView.text = context.getString(
                    R.string.template_fee,
                    amountFormatter.formatAssetAmount(
                            senderFeeTotal,
                            asset
                    )
            )
            recipientFeeLayout.visibility = View.GONE
        } else if (senderFeeTotal.signum() > 0 && recipientFee.signum() > 0) {
            feeTextView.text = context.getString(
                    R.string.template_fee,
                    amountFormatter.formatAssetAmount(
                            senderFeeTotal,
                            asset
                    )
            )
            recipientFeeLayout.visibility = View.VISIBLE
        }
    }
}