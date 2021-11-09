package io.tokend.template.util.comparator

import io.tokend.template.features.assets.model.Asset

/**
 * Default asset comparator.
 */
class AssetComparator(
    private val assetCodeComparator: Comparator<String>
) : Comparator<Asset> {

    override fun compare(o1: Asset, o2: Asset): Int {
        return assetCodeComparator.compare(o1.code, o2.code)
    }
}