package org.tokend.template.util.comparator

class AssetCodeComparator: Comparator<String> {
    override fun compare(o1: String, o2: String): Int {
        return o1.compareTo(o2, true)
    }
}