package io.tokend.template.view.picker

interface SimplePicker : Picker<PickerItem> {
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
}