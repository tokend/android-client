package org.tokend.template.util.comparator

/**
 * Default asset comparator.
 */
class AssetComparator : Comparator<String> {

    override fun compare(o1: String, o2: String): Int {
        return o1.compareTo(o2, true)
    }
}