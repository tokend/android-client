package io.tokend.template.features.systeminfo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.sdk.api.generated.resources.HorizonStateResource
import org.tokend.wallet.NetworkParams
import java.util.*
import kotlin.math.log10

class SystemInfoRecord(
    @JsonProperty("passphrase")
    val passphrase: String,
    @JsonProperty("precision_multiplier")
    val precisionMultiplier: Long,
    @JsonProperty("time_offset_seconds")
    val timeOffsetSeconds: Long,
    @JsonProperty("latest_block")
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

    @get:JsonIgnore
    val precision: Int
        get() = log10(precisionMultiplier.toDouble()).toInt()

    fun toNetworkParams(): NetworkParams {
        return NetworkParams(passphrase, precision, timeOffsetSeconds.toInt())
    }
}