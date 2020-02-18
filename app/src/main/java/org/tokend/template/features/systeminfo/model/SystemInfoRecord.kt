package org.tokend.template.features.systeminfo.model

import com.google.gson.annotations.SerializedName
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.wallet.NetworkParams
import java.util.*
import kotlin.math.log10

class SystemInfoRecord(
        @SerializedName("passphrase")
        val passphrase: String,
        @SerializedName("precision_multiplier")
        val precisionMultiplier: Long,
        @SerializedName("time_offset_seconds")
        val timeOffsetSeconds: Long,
        @SerializedName("latest_block")
        val latestBlock: Long
) {
    /**
     * @param source fresh [SystemInfo], if it's outdated then
     * [timeOffsetSeconds] will be wrong
     */
    constructor(source: SystemInfo) : this(
            passphrase = source.passphrase,
            precisionMultiplier = source.precisionMultiplier,
            timeOffsetSeconds = source.currentTime - Date().time / 1000,
            latestBlock = source.ledgersState[SystemInfo.LEDGER_CORE]?.latest
                    ?: throw IllegalArgumentException("Latest core block number is required")
    )

    val precision: Int
        get() = log10(precisionMultiplier.toDouble()).toInt()

    fun toNetworkParams(): NetworkParams {
        return NetworkParams(passphrase, precision, timeOffsetSeconds.toInt())
    }
}