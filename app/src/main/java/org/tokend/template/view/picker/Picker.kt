package org.tokend.template.view.picker

/**
 * View with ability to pick one of multiple items
 */
interface Picker {
    var selectedItem: PickerItem?
    var selectedItemIndex: Int

    /**
     * Sets given items to the picker
     *
     * @param items items to set
     * @param selectedIndex index of the item to select after set,
     * by default [selectedItemIndex] will be kept
     * and no 'item selected' event will be invoked
     */
    fun setItems(items: List<PickerItem>, selectedIndex: Int = selectedItemIndex)

    /**
     * Sets items with given names to the picker
     *
     * @see setItems
     */
    fun setSimpleItems(names: Collection<String>, selectedIndex: Int = selectedItemIndex) {
        setItems(names.map { PickerItem(it) }, selectedIndex)
    }

    /**
     * Sets items with given names to the picker
     *
     * @param selected name of the item to select after set
     *
     * @see setSimpleItems
     * @see setItems
     */
    fun setSimpleItems(names: Collection<String>, selected: String) {
        setSimpleItems(names, names.indexOf(selected))
    }

    /**
     * Sets item selection listener
     */
    fun onItemSelected(listener: ((PickerItem) -> Unit)?)
}