package io.tokend.template.util.validator

interface CharSequenceValidator {
    fun isValid(sequence: CharSequence?): Boolean
}