package org.tokend.template.features.assets.adapter

import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.TooltipCompat
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.SimpleItemClickListener
import org.tokend.template.view.util.CircleLogoUtil

class AssetListItemViewHolder(view: View) : BaseViewHolder<AssetListItem>(view) {
    private val logoImageView: AppCompatImageView = view.find(R.id.asset_logo_image_view)
    private val codeTextView: TextView = view.find(R.id.asset_code_text_view)
    private val nameTextView: TextView = view.find(R.id.asset_name_text_view)
    private val detailsButton: TextView = view.find(R.id.asset_details_button)
    private val primaryActionButton: Button = view.find(R.id.asset_primary_action_button)
    private val balanceExistsIndicator: View = view.find(R.id.asset_balance_exists_image_view)

    init {
        TooltipCompat.setTooltipText(balanceExistsIndicator,
                view.context.getText(R.string.asset_balance_exists))
    }

    override fun bind(item: AssetListItem, clickListener: SimpleItemClickListener<AssetListItem>?) {
        super.bind(item, clickListener)
        primaryActionButton.onClick {
            clickListener?.invoke(it, item)
        }
    }

    override fun bind(item: AssetListItem) {
        CircleLogoUtil.setLogo(logoImageView, item.code, item.logoUrl)

        codeTextView.text = item.code

        nameTextView.text = item.name
        if (!item.name.isNullOrEmpty()) {
            nameTextView.visibility = View.VISIBLE
        } else {
            nameTextView.visibility = View.GONE
        }

        detailsButton.onClick { view.callOnClick() }

        if (item.balanceExists) {
            balanceExistsIndicator.visibility = View.VISIBLE
            primaryActionButton.text = view.context.getString(R.string.view_asset_history_action)
        } else {
            balanceExistsIndicator.visibility = View.GONE
            primaryActionButton.text = view.context.getString(R.string.create_balance_action)
        }
    }
}