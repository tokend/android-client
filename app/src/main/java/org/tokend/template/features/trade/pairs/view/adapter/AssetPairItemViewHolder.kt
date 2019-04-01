package org.tokend.template.features.trade.pairs.view.adapter

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

class AssetPairItemViewHolder(
        view: View,
        private val amountFormatter: AmountFormatter
) : BaseViewHolder<AssetPairListItem>(view) {
    private val priceTextView = view.find<TextView>(R.id.price_text_view)
    private val baseCodeTextView = view.find<TextView>(R.id.base_asset_code_text_view)
    private val restCodeTextView = view.find<TextView>(R.id.rest_pair_code_text_view)
    private val baseLogoImageView = view.find<ImageView>(R.id.base_asset_logo_image_view)

    private val picasso = Picasso.with(view.context)

    private val baseLogoSize: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)
    }

    private val logoFactory = LogoFactory(view.context)

    override fun bind(item: AssetPairListItem) {
        baseCodeTextView.text = item.baseAssetCode
        restCodeTextView.text = view.context.getString(
                R.string.template_asset_pair,
                "",
                item.quoteAssetCode
        )

        priceTextView.text = amountFormatter.formatAssetAmount(
                item.price,
                item.quoteAssetCode,
                withAssetCode = false,
                minDecimalDigits = amountFormatter.getDecimalDigitsCount(item.quoteAssetCode)
        )

        if (item.baseAssetLogoUrl != null) {
            picasso.load(item.baseAssetLogoUrl)
                    .placeholder(R.color.white)
                    .resize(baseLogoSize, baseLogoSize)
                    .centerInside()
                    .transform(CircleTransform())
                    .into(baseLogoImageView)
        } else {
            picasso.cancelRequest(baseLogoImageView)
            baseLogoImageView.setImageBitmap(
                    logoFactory.getWithAutoBackground(
                            item.baseAssetCode,
                            baseLogoSize
                    )
            )
        }
    }
}