package org.tokend.template.data.model

import java.io.Serializable

interface Asset : Serializable {
    val code: String
    val trailingDigits: Int
    val name: String?
}