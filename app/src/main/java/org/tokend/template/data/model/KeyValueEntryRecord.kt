package org.tokend.template.data.model

import org.tokend.sdk.api.generated.resources.KeyValueEntryResource
import org.tokend.wallet.xdr.KeyValueEntryType

sealed class KeyValueEntryRecord(val key: kotlin.String) {
    class Number(key: kotlin.String, val value: Long) : KeyValueEntryRecord(key)
    class String(key: kotlin.String, val value: kotlin.String) : KeyValueEntryRecord(key)

    companion object {
        fun fromResource(source: KeyValueEntryResource): KeyValueEntryRecord {
            val key = source.id
            val value = source.value

            return when (value.type.value) {
                KeyValueEntryType.STRING.value -> String(key, value.str!!)
                KeyValueEntryType.UINT32.value -> Number(key, value.u32!!)
                KeyValueEntryType.UINT64.value -> Number(key, value.u64!!)
                else -> throw IllegalArgumentException("Unknown KeyValue type " +
                        "${value.type.name} ${value.type.value}")
            }
        }
    }
}