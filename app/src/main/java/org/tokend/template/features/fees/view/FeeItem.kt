package org.tokend.template.features.fees.view

import org.tokend.template.extensions.isMaxPossibleAmount
import org.tokend.template.features.fees.model.FeeRecord
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
        fun fromFee(fee: FeeRecord, amountFormatter: AmountFormatter): FeeItem {
            val type = FeeType.values().find { fee.feeType == it.value }!!

            val subtype = Subtype.values().find { fee.subtype == it.value }!!

            val fixed = amountFormatter.formatAssetAmount(fee.fixed, fee.asset, 1)

            val percent = "${amountFormatter.formatAmount(fee.percent, 6, 1)}%"

            val lowerBound =
                    if (fee.lowerBound.isMaxPossibleAmount())
                        ""
                    else
                        amountFormatter.formatAssetAmount(
                                fee.lowerBound, fee.asset, 1,
                                abbreviation = true,
                                withAssetCode = false
                        )

            val upperBound =
                    if (fee.upperBound.isMaxPossibleAmount())
                        ""
                    else
                        amountFormatter.formatAssetAmount(
                                fee.upperBound, fee.asset, 1,
                                abbreviation = true,
                                withAssetCode = false)

            return FeeItem(type, subtype, fixed, percent, lowerBound, upperBound)
        }
    }
}