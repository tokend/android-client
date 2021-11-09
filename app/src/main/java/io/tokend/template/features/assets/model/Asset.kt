package io.tokend.template.features.assets.model

import java.io.Serializable

interface Asset : Serializable {
    val code: String
    val trailingDigits: Int
    val name: String?

    fun contentEquals(other: Asset): Boolean {
        return code == other.code
                && trailingDigits == other.trailingDigits
                && name == other.name
    }
}