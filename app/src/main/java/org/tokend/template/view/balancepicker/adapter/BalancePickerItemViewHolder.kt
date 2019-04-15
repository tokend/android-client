package org.tokend.template.view.balancepicker.adapter

import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.jetbrains.anko.find
import org.tokend.template.R
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.util.CircleTransform
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class BalancePickerItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter
) : BaseViewHolder<BalancePickerListItem>(view) {
    private val logoImageView = view.find<ImageView>(R.id.asset_logo_image_view)
    private val codeTextView = view.find<TextView>(R.id.asset_code_text_view)
    private val availableTextView = view.find<TextView>(R.id.balance_available_text_view)
    private val dividerView = view.find<View>(R.id.divider_view)

    private val picasso = Picasso.with(view.context)
    private val logoSize =
            view.context.resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)
    private val logoFactory = LogoFactory(view.context)

    private val colorDefaultText = ContextCompat.getColor(view.context, R.color.primary_text)
    private val colorSecondaryText = ContextCompat.getColor(view.context, R.color.secondary_text)
    private val colorError = ContextCompat.getColor(view.context, R.color.error)

    var dividerIsVisible: Boolean
        get() = dividerView.visibility == View.VISIBLE
        set(value) {
            dividerView.visibility =
                    if (value)
                        View.VISIBLE
                    else
                        View.GONE
        }

    override fun bind(item: BalancePickerListItem) {
        if (item.logoUrl != null) {
            picasso.load(item.logoUrl)
                    .placeholder(R.color.white)
                    .resize(logoSize, logoSize)
                    .centerInside()
                    .transform(CircleTransform())
                    .into(logoImageView)
        } else {
            picasso.cancelRequest(logoImageView)
            logoImageView.setImageBitmap(
                    logoFactory.getWithAutoBackground(
                            item.assetCode,
                            logoSize
                    )
            )
        }

        codeTextView.text = item.assetCode

        availableTextView.text = view.context.getString(
                R.string.template_available,
                amountFormatter.formatAssetAmount(
                        item.available,
                        item.assetCode,
                        withAssetCode = false
                )
        )

        if (item.isEnough) {
            availableTextView.setTextColor(colorDefaultText)
            codeTextView.setTextColor(colorDefaultText)
            logoImageView.alpha = 1f
            availableTextView.alpha = 1f
        } else {
            availableTextView.setTextColor(colorError)
            codeTextView.setTextColor(colorSecondaryText)
            logoImageView.alpha = DISABLED_ALPHA
            availableTextView.alpha = DISABLED_ALPHA
        }
    }

    companion object {
        private const val DISABLED_ALPHA = 0.6f
    }
}