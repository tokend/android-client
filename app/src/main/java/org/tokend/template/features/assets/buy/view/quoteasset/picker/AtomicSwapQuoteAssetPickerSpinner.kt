package org.tokend.template.features.assets.buy.view.quoteasset.picker

import android.content.Context
import androidx.appcompat.widget.AppCompatSpinner
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import org.tokend.template.view.picker.Picker

class AtomicSwapQuoteAssetPickerSpinner : AppCompatSpinner, Picker<AtomicSwapQuoteAssetSpinnerItem> {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private var items = listOf<AtomicSwapQuoteAssetSpinnerItem>()
    private var itemSelectionListener: ((AtomicSwapQuoteAssetSpinnerItem) -> Unit)? = null
    private var suspendEvent = false

    var itemsAdapter: AtomicSwapQuoteAssetsSpinnerAdapter?
        get() = this.adapter as? AtomicSwapQuoteAssetsSpinnerAdapter
        set(value) {
            this.adapter = value
        }

    override var selectedItemIndex: Int
        get() = selectedItemPosition
        set(value) {
            setSelection(value)
        }
    override var selectedItem: AtomicSwapQuoteAssetSpinnerItem?
        get() = items.getOrNull(selectedItemIndex)
        set(value) {
            selectedItemIndex = items.indexOf(value).takeIf { it >= 0 } ?: 0
        }

    init {
        adapter = itemsAdapter
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?,
                position: Int, id: Long
            ) {
                if (!suspendEvent) {
                    itemSelectionListener?.invoke(items[position])
                }
            }
        }
    }

    override fun setItems(items: List<AtomicSwapQuoteAssetSpinnerItem>, selectedIndex: Int) {
        val selected = items.getOrNull(selectedIndex)
        this.items = items
        initItems(selected)
    }

    override fun onItemSelected(listener: ((AtomicSwapQuoteAssetSpinnerItem) -> Unit)?) {
        this.itemSelectionListener = listener
    }

    private fun initItems(selected: AtomicSwapQuoteAssetSpinnerItem? = null) {
        val indexToSelect = items.indexOfFirst { it.asset.code == selected?.asset?.code }
            .let { index ->
                if (index < 0) {
                    suspendEvent = false
                    0
                } else {
                    suspendEvent = true
                    index
                }
            }

        post {
            itemsAdapter?.clear()
            itemsAdapter?.addAll(items)

            selectedItemIndex = indexToSelect

            suspendEvent = false
        }
    }
}