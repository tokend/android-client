package org.tokend.template.base.view.picker

import android.content.Context
import android.support.v7.widget.AppCompatSpinner
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import org.tokend.template.R

/**
 * Simple value picker based on [AppCompatSpinner]
 */
class PickerSpinner : AppCompatSpinner, Picker {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private var items = listOf<PickerItem>()
    private var itemSelectionListener: ((PickerItem) -> Unit)? = null
    private var suspendEvent = false

    override var selectedItemIndex: Int
        get() = selectedItemPosition
        set(value) {
            setSelection(value)
        }
    override var selectedItem: PickerItem?
        get() = items.getOrNull(selectedItemIndex)
        set(value) {
            selectedItemIndex = items.indexOf(value).takeIf { it >= 0 } ?: 0
        }

    init {
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                        position: Int, id: Long) {
                if (!suspendEvent) {
                    itemSelectionListener?.invoke(items[position])
                }
            }
        }
    }

    override fun setItems(items: List<PickerItem>, keepSelection: Boolean) {
        val selected = selectedItem
        this.items = items
        if (keepSelection) {
            initItems(selected)
        } else {
            initItems()
        }
    }

    override fun onItemSelected(listener: ((PickerItem) -> Unit)?) {
        this.itemSelectionListener = listener
    }

    private fun initItems(selected: PickerItem? = null) {
        val indexToSelect = items.indexOfFirst { it.text == selected?.text }
                .let { index ->
                    if (index < 0) {
                        suspendEvent = false
                        0
                    } else {
                        index
                    }
                }

        adapter = ArrayAdapter<String>(context, R.layout.spinner_item,
                items.map { it.text })

        selectedItemIndex = indexToSelect

        suspendEvent = false
    }
}