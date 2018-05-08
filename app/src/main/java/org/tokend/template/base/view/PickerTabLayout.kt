package org.tokend.template.base.view

import android.content.Context
import android.support.design.widget.TabLayout
import android.util.AttributeSet

/**
 * Simple value picker based on {@link android.support.design.widget.TabLayout}
 */
class PickerTabLayout : TabLayout {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

    constructor(context: Context) : super(context)

    private var items = listOf<String>()
    private var itemSelectionListener: ((String) -> Unit)? = null
    private var suspendEvent = false

    val selectedItem: String?
        get() = items.getOrNull(selectedTabPosition)

    init {
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: Tab?) {}

            override fun onTabUnselected(tab: Tab?) {}

            override fun onTabSelected(tab: Tab?) {
                (tab?.tag as? String)?.takeIf { !suspendEvent }?.let {
                    itemSelectionListener?.invoke(it)
                }
            }
        })
    }

    fun setItems(items: List<String>, keepSelected: Boolean = false) {
        val selected = selectedItem
        this.items = items
        if (keepSelected) {
            initTabs(selected)
        } else {
            initTabs()
        }
    }

    fun setSelectedItem(selected: String) {
        items.indexOf(selected)
                .takeIf { it >= 0 }
                ?.let { setSelectedItemIndex(it) }
    }

    fun setSelectedItemIndex(index: Int) {
        getTabAt(index)?.select()
    }

    fun onItemSelected(listener: ((String) -> Unit)?) {
        this.itemSelectionListener = listener
    }

    private fun initTabs(selected: String? = null) {
        if (selected != null) {
            suspendEvent = true
        }
        removeAllTabs()
        items.forEachIndexed { i, item ->
            addTab(newTab().setText(item).setTag(item),
                    item == selected || selected == null && i == 0)
        }
        suspendEvent = false
    }
}