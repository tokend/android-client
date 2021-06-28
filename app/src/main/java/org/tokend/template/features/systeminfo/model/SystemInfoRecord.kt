package org.tokend.template.features.systeminfo.model

import com.google.gson.annotations.SerializedName
import org.tokend.sdk.api.generated.resources.HorizonStateResource
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
        val latestBlock: Long,
) {
    /**
     * @param source fresh [HorizonStateResource], if it's outdated then
     * [timeOffsetSeconds] will be wrong
     */
    constructor(source: HorizonStateResource) : this(
            passphrase = source.networkPassphrase,
            precisionMultiplier = source.precision,
            timeOffsetSeconds = (source.currentTime.time - Date().time) / 1000,
            latestBlock = source.core.latest
    )

    val precision: Int
        get() = log10(precisionMultiplier.toDouble()).toInt()

    fun toNetworkParams(): NetworkParams {
        return NetworkParams(passphrase, precision, timeOffsetSeconds.toInt())
    }
}