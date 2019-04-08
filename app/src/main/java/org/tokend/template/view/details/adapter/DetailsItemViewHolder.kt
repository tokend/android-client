package org.tokend.template.view.details.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.jetbrains.anko.find
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.details.DetailsItem

class DetailsItemViewHolder(view: View) : BaseViewHolder<DetailsItem>(view) {
    private val mainTextView: TextView = view.find(android.R.id.title)
    private val hintTextView: TextView = view.find(android.R.id.summary)
    private val headerTextView: TextView = view.find(R.id.header_text_view)
    private val iconImageView: ImageView = view.find(android.R.id.icon)
    private val dividerView: View = view.find(R.id.divider_view)

    var dividerIsVisible: Boolean
        get() = dividerView.visibility == View.VISIBLE
        set(value) {
            dividerView.visibility =
                    if (value)
                        View.VISIBLE
                    else
                        View.GONE
        }

    override fun bind(item: DetailsItem) {
        if (item.header != null) {
            headerTextView.visibility = View.VISIBLE
            headerTextView.text = item.header
        } else {
            headerTextView.visibility = View.GONE
        }

        mainTextView.text = item.text

        if (item.hint != null) {
            hintTextView.visibility = View.VISIBLE
            hintTextView.text = item.hint
        } else {
            hintTextView.visibility = View.GONE
        }

        iconImageView.setImageDrawable(item.icon)
    }
}