package io.tokend.template.util

interface RecordWithPolicy {
    val policy: Int

    fun hasPolicy(value: Int): Boolean {
        return policy and value == value
    }
}