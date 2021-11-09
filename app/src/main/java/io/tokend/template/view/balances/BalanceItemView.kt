package io.tokend.template.view.balances

import android.view.View
import android.widget.ImageView
import android.widget.TextView

interface BalanceItemView {
    val logoImageView: ImageView
    val nameTextView: TextView
    val amountTextView: TextView
    val dividerView: View

    var dividerIsVisible: Boolean
        get() = dividerView.visibility == View.VISIBLE
        set(value) {
            dividerView.visibility = if (value) View.VISIBLE else View.GONE
        }

    fun displayLogo(logoUrl: String?, assetCode: String)
}