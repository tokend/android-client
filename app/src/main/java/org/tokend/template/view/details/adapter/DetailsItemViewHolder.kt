package org.tokend.template.view.details.adapter

import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.details.DetailsItem

class DetailsItemViewHolder(view: View) : BaseViewHolder<DetailsItem>(view) {
    private val mainTextView: TextView = view.findViewById(android.R.id.title)
    private val hintTextView: TextView = view.findViewById(android.R.id.summary)
    private val headerTextView: TextView = view.findViewById(R.id.header_text_view)
    private val iconImageView: ImageView = view.findViewById(android.R.id.icon)
    private val dividerView: View = view.findViewById(R.id.divider_view)
    private val preferenceRoot: ViewGroup = view.findViewById(R.id.preference_root_layout)
    private val viewFrame: ViewGroup = view.findViewById(android.R.id.widget_frame)

    private val defaultTextColor = ContextCompat.getColor(view.context, R.color.primary_text)
    private val disabledTextColor = ContextCompat.getColor(view.context, R.color.secondary_text)

    init {
        mainTextView.ellipsize = TextUtils.TruncateAt.MIDDLE

        mainTextView.setOnTouchListener { _, event ->
            if (event.action == 1 && !mainTextView.hasSelection()) {
                view.callOnClick()
            }
            false
        }

        hintTextView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                view.context.resources.getDimensionPixelSize(R.dimen.text_size_hint).toFloat()
        )

        preferenceRoot.background = null
    }

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

        mainTextView.setTextIsSelectable(!item.singleLineText)
        mainTextView.setSingleLine(item.singleLineText)
        mainTextView.text = item.text

        mainTextView.setTextColor(
            if (item.isEnabled)
                item.textColor ?: defaultTextColor
            else
                disabledTextColor
        )
        if (item.hint != null) {
            hintTextView.visibility = View.VISIBLE
            hintTextView.text = item.hint
        } else {
            hintTextView.visibility = View.GONE
        }

        iconImageView.setImageDrawable(item.icon)

        viewFrame.removeAllViews()
        if (item.extraView != null) {
            viewFrame.addView(item.extraView)
        }
    }
}