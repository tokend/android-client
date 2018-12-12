package org.tokend.template.features.fees.adapter

import org.tokend.sdk.api.fees.model.Fee
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.wallet.xdr.FeeType

class FeeItem(
        val type: FeeType,
        val subtype: Subtype,
        val fixed: String,
        val percent: String,
        val lowerBound: String,
        val upperBound: String
) {
    enum class Subtype(val value: Int) {
        INCOMING_OUTGOING(0),
        OUTGOING(1),
        INCOMING(2)
    }

    companion object {
        fun fromFee(fee: Fee): FeeItem {
            val type = FeeType.values().find { fee.feeType == it.value }!!

            val subtype = Subtype.values().find { fee.subtype == it.value }!!

            val fixed = "${AmountFormatter.formatAssetAmount(fee.fixed, fee.asset, 1)} " +
                    fee.asset

            val percent = "${AmountFormatter.formatAmount(fee.percent, 2, 1)}%"

            val lowerBound =
                    "${AmountFormatter.formatAssetAmount(fee.lowerBound, fee.requestAsset, 1)} " +
                    fee.requestAsset

            val upperBound =
                    "${AmountFormatter.formatAssetAmount(fee.upperBound, fee.requestAsset, 1)} " +
                    fee.requestAsset

            return FeeItem(type, subtype, fixed, percent, lowerBound, upperBound)
        }
    }
}