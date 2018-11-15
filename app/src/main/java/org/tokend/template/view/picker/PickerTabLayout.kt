package org.tokend.template.view.picker


import android.content.Context
import android.support.design.widget.TabLayout
import android.util.AttributeSet

/**
 * Simple value picker based on [TabLayout]
 */
class PickerTabLayout : TabLayout, Picker {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private var items = listOf<PickerItem>()
    private var itemSelectionListener: ((PickerItem) -> Unit)? = null
    private var suspendEvent = false

    override var selectedItemIndex: Int
        get() = selectedTabPosition
        set(value) {
            (getTabAt(value) ?: getTabAt(0))?.select()
        }
    override var selectedItem: PickerItem?
        get() = items.getOrNull(selectedItemIndex)
        set(value) {
            selectedItemIndex = items.indexOf(value)
        }

    init {
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: Tab?) {}

            override fun onTabUnselected(tab: Tab?) {}

            override fun onTabSelected(tab: Tab?) {
                (tab?.tag as? PickerItem)?.takeIf { !suspendEvent }?.let {
                    itemSelectionListener?.invoke(it)
                }
            }
        })
    }

    override fun setItems(items: List<PickerItem>, selectedIndex: Int) {
        val toSelect = items.getOrNull(selectedIndex)
        this.items = items
        initTabs(toSelect)
    }

    override fun onItemSelected(listener: ((PickerItem) -> Unit)?) {
        this.itemSelectionListener = listener
    }

    private fun initTabs(selected: PickerItem? = null) {
        val indexToSelect = items.indexOfFirst { it.text == selected?.text }
                .let { index ->
                    if (index < 0) {
                        suspendEvent = false
                        0
                    } else {
                        suspendEvent = true
                        index
                    }
                }
        removeAllTabs()
        items.forEachIndexed { i, item ->
            addTab(newTab().setText(item.text).setTag(item),
                    i == indexToSelect)
        }
        suspendEvent = false
    }
}