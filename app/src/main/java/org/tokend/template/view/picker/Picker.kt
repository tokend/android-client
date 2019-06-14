package org.tokend.template.view.picker

/**
 * View with ability to pick one of multiple items
 */
interface Picker<T> {
    var selectedItem: T?
    var selectedItemIndex: Int

    /**
     * Sets given items to the picker
     *
     * @param items items to set
     * @param selectedIndex index of the item to select after set,
     * by default [selectedItemIndex] will be kept
     * and no 'item selected' event will be invoked
     */
    fun setItems(items: List<T>, selectedIndex: Int = selectedItemIndex)

    /**
     * Sets item selection listener
     */
    fun onItemSelected(listener: ((T) -> Unit)?)
}