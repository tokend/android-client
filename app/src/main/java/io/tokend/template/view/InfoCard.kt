package io.tokend.template.view

import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater

/**
 * Information card with heading and lot of rows.
 *
 * @param parent [ViewGroup] to add the card into
 */
class InfoCard(parent: ViewGroup) {
    private val view: View
    private var mainRow: View? = null
    private val contentLayout: LinearLayout
    private val context = parent.context!!

    init {
        view = context.layoutInflater.inflate(R.layout.layout_info_card, parent, false)
        parent.addView(view)

        contentLayout = view.findViewById(R.id.card_content_layout)
    }

    /**
     * Adds or changes heading content of the card.
     */
    fun setHeading(title: String?, value: String?): InfoCard {
        if (mainRow == null) {
            mainRow =
                context.layoutInflater.inflate(
                    R.layout.layout_info_card_heading,
                    contentLayout,
                    false
                )
            contentLayout.addView(mainRow)
        }

        val titleTextView = mainRow?.findViewById<TextView>(R.id.title)
        titleTextView?.text = title ?: ""
        val valueTextView = mainRow?.findViewById<TextView>(R.id.value)
        valueTextView?.text = value ?: ""

        return this
    }

    /**
     * Adds or changes heading content of the card.
     */
    fun setHeading(@StringRes title: Int, value: String?): InfoCard {
        return setHeading(context.getString(title), value)
    }

    /**
     * Adds a single row to the card.
     */
    fun addRow(title: String?, value: String?): InfoCard {
        val rowView =
            context.layoutInflater.inflate(R.layout.layout_info_card_row, contentLayout, false)

        val titleTextView = rowView.findViewById<TextView>(R.id.title)
        val valueTextView = rowView.findViewById<TextView>(R.id.value)

        if (title.isNullOrBlank()) {
            titleTextView.visibility = View.GONE
        } else {
            titleTextView.text = title
        }

        if (value.isNullOrBlank()) {
            valueTextView.visibility = View.GONE
        } else {
            valueTextView.text = value
        }

        return addView(rowView)
    }

    /**
     * Adds a single row to the card.
     */
    fun addRow(@StringRes title: Int, value: String?): InfoCard {
        return addRow(context.getString(title), value)
    }

    /**
     * Adds multiple rows to the card
     */
    fun addRows(vararg rows: Pair<String, String>): InfoCard {
        rows.forEach {
            addRow(it.first, it.second)
        }

        return this
    }

    /**
     * Adds a row with a switcher instead of the value
     */
    fun addSwitcherRow(
        title: String,
        switchListener: CompoundButton.OnCheckedChangeListener?,
        isChecked: Boolean = false,
        animate: Boolean = false
    ): InfoCard {
        val rowView =
            context.layoutInflater.inflate(
                R.layout.layout_info_card_switch_row, contentLayout,
                false
            )

        val titleTextView = rowView.findViewById<TextView>(R.id.title)
        val switcherView = rowView.findViewById<SwitchCompat>(R.id.switcher)

        switcherView.isChecked = animate != isChecked

        titleTextView.text = title

        val result = addView(rowView)

        switcherView.post {
            switcherView.isChecked = isChecked
            if (switchListener != null) {
                switcherView.setOnCheckedChangeListener(switchListener)
            }
        }

        return result
    }

    /**
     * Adds a row with a switcher instead of the value
     */
    fun addSwitcherRow(
        @StringRes title: Int,
        switchListener: CompoundButton.OnCheckedChangeListener?,
        isChecked: Boolean = false,
        animate: Boolean = false
    ): InfoCard {
        return addSwitcherRow(context.getString(title), switchListener, isChecked, animate)
    }

    /**
     * Adds given view to the card
     */
    fun addView(view: View?): InfoCard {
        if (view != null) {
            contentLayout.addView(view)
        }
        return this
    }
}