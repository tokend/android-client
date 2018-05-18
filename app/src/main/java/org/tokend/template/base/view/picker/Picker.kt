package org.tokend.template.base.view.picker

interface Picker {
    var selectedItem: PickerItem?
    var selectedItemIndex: Int

    fun setItems(items: List<PickerItem>, keepSelection: Boolean = true)
    fun setSimpleItems(names: Collection<String>, keepSelection: Boolean = true) {
        setItems(names.map { PickerItem(it) }, keepSelection)
    }
    fun onItemSelected(listener: ((PickerItem) -> Unit)?)
}