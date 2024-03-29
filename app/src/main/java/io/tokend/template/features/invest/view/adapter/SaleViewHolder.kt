package io.tokend.template.features.invest.view.adapter

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import io.tokend.template.R
import io.tokend.template.features.invest.model.SaleRecord
import io.tokend.template.features.invest.view.SaleProgressWrapper
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.util.ImageViewUtil
import io.tokend.template.view.util.formatter.AmountFormatter
import kotlinx.android.synthetic.main.layout_sale_picture.view.*
import kotlinx.android.synthetic.main.list_item_sale.view.*

class SaleViewHolder(
    view: View,
    private val amountFormatter: AmountFormatter
) : BaseViewHolder<SaleRecord>(view) {
    private val pictureImageView = view.sale_picture_image_view
    private val nameTextView = view.sale_name_text_view
    private val shortDescriptionTextView = view.sale_short_description_text_view
    private val upcomingIndicator = view.sale_upcoming_image_view

    private val picturePlaceholder =
        ColorDrawable(ContextCompat.getColor(view.context, R.color.imagePlaceholder))

    override fun bind(item: SaleRecord) {
        nameTextView.text = view.context.getString(
            R.string.template_sale_name_asset,
            item.name, item.baseAsset.code
        )
        shortDescriptionTextView.text = item.description

        ImageViewUtil.loadImage(pictureImageView, item.logoUrl, picturePlaceholder) {
            centerCrop()
        }

        if (item.isUpcoming) {
            upcomingIndicator.visibility = View.VISIBLE
        } else {
            upcomingIndicator.visibility = View.GONE
        }

        SaleProgressWrapper(view, amountFormatter).displayProgress(item)
    }
}