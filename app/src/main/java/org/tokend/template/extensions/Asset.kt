package org.tokend.template.extensions

import org.tokend.sdk.api.assets.model.AssetPair
import org.tokend.sdk.api.assets.model.SimpleAsset
import org.tokend.wallet.xdr.AssetPairPolicy
import org.tokend.wallet.xdr.AssetPolicy

typealias Asset = SimpleAsset

fun Asset.isTransferable(): Boolean {
    return checkPolicy(policy, AssetPolicy.TRANSFERABLE.value)
}

fun Asset.isBase(): Boolean {
    return checkPolicy(policy, AssetPolicy.BASE_ASSET.value)
}

fun Asset.isWithdrawable(): Boolean {
    return checkPolicy(policy, AssetPolicy.WITHDRAWABLE.value)
}

fun AssetPair.isTradeable(): Boolean {
    return checkPolicy(policy, AssetPairPolicy.TRADEABLE_SECONDARY_MARKET.value)
}

private fun checkPolicy(policy: Int, mask: Int): Boolean {
    return policy and mask == mask
}