package org.tokend.template.extensions

import org.tokend.sdk.api.models.Asset
import org.tokend.wallet.xdr.AssetPolicy

fun Asset.isTransferable(): Boolean {
    return checkPolicy(policy, AssetPolicy.TRANSFERABLE.value)
}

fun Asset.isBase(): Boolean {
    return checkPolicy(policy, AssetPolicy.BASE_ASSET.value)
}

fun Asset.isWithdrawable(): Boolean {
    return checkPolicy(policy, AssetPolicy.WITHDRAWABLE.value)
}

private fun checkPolicy(policy: Int, mask: Int): Boolean {
    return policy and mask == mask
}