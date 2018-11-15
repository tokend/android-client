package org.tokend.template.features.assets.adapter

import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.TooltipCompat
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.SimpleItemClickListener
import org.tokend.template.features.assets.AssetLogoFactory

class AssetListItemViewHolder(view: View) : BaseViewHolder<AssetListItem>(view) {
    private val logoImageView: AppCompatImageView = view.find(R.id.asset_logo_image_view)
    private val codeTextView: TextView = view.find(R.id.asset_code_text_view)
    private val nameTextView: TextView = view.find(R.id.asset_name_text_view)
    private val detailsButton: TextView = view.find(R.id.asset_details_button)
    private val primaryActionButton: Button = view.find(R.id.asset_primary_action_button)
    private val balanceExistsIndicator: View = view.find(R.id.asset_balance_exists_image_view)

    private val logoSize: Int by lazy {
        view.context.resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)
    }

    override fun bind(item: AssetListItem, clickListener: SimpleItemClickListener<AssetListItem>?) {
        super.bind(item, clickListener)
        primaryActionButton.onClick {
            clickListener?.invoke(it, item)
        }
    }

    override fun bind(item: AssetListItem) {
        if (item.logoUrl != null) {
            Picasso.with(view.context)
                    .load(item.logoUrl)
                    .placeholder(R.color.white)
                    .resize(logoSize, logoSize)
                    .centerInside()
                    .into(logoImageView)
        } else {
            logoImageView.setImageBitmap(AssetLogoFactory(view.context)
                    .getForCode(item.code, logoSize))
        }

        codeTextView.text = item.code

        nameTextView.text = item.name
        if (!item.name.isNullOrEmpty()) {
            nameTextView.visibility = View.VISIBLE
        } else {
            nameTextView.visibility = View.GONE
        }

        detailsButton.onClick { view.callOnClick() }

        TooltipCompat.setTooltipText(balanceExistsIndicator,
                view.context.getText(R.string.asset_balance_exists))

        if (item.balanceExists) {
            balanceExistsIndicator.visibility = View.VISIBLE
            primaryActionButton.text = view.context.getString(R.string.view_asset_history_action)
        } else {
            balanceExistsIndicator.visibility = View.GONE
            primaryActionButton.text = view.context.getString(R.string.create_balance_action)
        }
    }
}