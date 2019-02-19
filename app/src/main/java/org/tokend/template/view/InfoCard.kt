package org.tokend.template.view

import android.support.annotation.StringRes
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R

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

        contentLayout = view.find(R.id.card_content_layout)
    }

    /**
     * Adds or changes heading content of the card.
     */
    fun setHeading(title: String?, value: String?): InfoCard {
        if (mainRow == null) {
            mainRow =
                    context.layoutInflater.inflate(R.layout.layout_info_card_heading, contentLayout, false)
            contentLayout.addView(mainRow)
        }

        val titleTextView = mainRow?.find<TextView>(R.id.title)
        titleTextView?.text = title ?: ""
        val valueTextView = mainRow?.find<TextView>(R.id.value)
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

        val titleTextView = rowView.find<TextView>(R.id.title)
        val valueTextView = rowView.find<TextView>(R.id.value)

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
    fun addSwitcherRow(title: String, isChecked: Boolean = false,
                       switchListener: CompoundButton.OnCheckedChangeListener?): InfoCard {
        val rowView =
                context.layoutInflater.inflate(R.layout.layout_info_card_switch_row, contentLayout,
                        false)

        val titleTextView = rowView.find<TextView>(R.id.title)
        val switcherView = rowView.find<SwitchCompat>(R.id.switcher)
        switcherView.isChecked = isChecked

        titleTextView.text = title
        if (switchListener != null) {
            switcherView.setOnCheckedChangeListener(switchListener)
        }

        return addView(rowView)
    }

    /**
     * Adds a row with a switcher instead of the value
     */
    fun addSwitcherRow(@StringRes title: Int, isChecked: Boolean = false,
                       switchListener: CompoundButton.OnCheckedChangeListener?): InfoCard {
        return addSwitcherRow(context.getString(title), isChecked, switchListener)
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