package org.tokend.template.features.invest.adapter

import android.view.View
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.list_item_sale.view.*
import org.tokend.template.R
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.view.SaleProgressWrapper
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.formatter.AmountFormatter

class SaleViewHolder(view: View,
                     private val amountFormatter: AmountFormatter) : BaseViewHolder<SaleRecord>(view) {
    private val pictureImageView = view.sale_picture_image_view
    private val nameTextView = view.sale_name_text_view
    private val shortDescriptionTextView = view.sale_short_description_text_view
    private val upcomingIndicator = view.sale_upcoming_image_view

    override fun bind(item: SaleRecord) {
        nameTextView.text = view.context.getString(R.string.template_sale_name_token,
                item.name, item.baseAsset)
        shortDescriptionTextView.text = item.shortDescription

        item.logoUrl.also {
            Picasso.with(view.context)
                    .load(it)
                    .placeholder(R.color.saleImagePlaceholder)
                    .fit()
                    .centerCrop()
                    .into(pictureImageView)
        }

        if (item.isUpcoming) {
            upcomingIndicator.visibility = View.VISIBLE
        } else {
            upcomingIndicator.visibility = View.GONE
        }

        SaleProgressWrapper(view, amountFormatter).displayProgress(item)
    }
}