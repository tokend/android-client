package org.tokend.template.extensions

import org.tokend.wallet.NetworkParams
import org.tokend.wallet.xdr.Uint64
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * @return true if this number is a maximum possible amount ([Uint64.MAX_VALUE])
 * considering specified [amountPrecision]
 *
 * @param amountPrecision network amount precision as a count of decimal places,
 * [NetworkParams.DEFAULT_PRECISION] by default
 */
fun BigDecimal.isMaxPossibleAmount(
        amountPrecision: Int = NetworkParams.DEFAULT_PRECISION
): Boolean = this
        .scaleByPowerOfTen(amountPrecision)
        .setScale(0, RoundingMode.DOWN)
        .longValueExact() == Uint64.MAX_VALUE