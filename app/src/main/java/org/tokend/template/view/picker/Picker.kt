package org.tokend.template.view.picker

interface Picker {
    var selectedItem: PickerItem?
    var selectedItemIndex: Int

    fun setItems(items: List<PickerItem>, selectedIndex: Int = selectedItemIndex)
    fun setSimpleItems(names: Collection<String>, selectedIndex: Int = selectedItemIndex) {
        setItems(names.map { PickerItem(it) }, selectedIndex)
    }

    fun setSimpleItems(names: Collection<String>, selected: String) {
        setSimpleItems(names, names.indexOf(selected))
    }

    fun onItemSelected(listener: ((PickerItem) -> Unit)?)
}