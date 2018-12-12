package org.tokend.template.util.validator

/**
 * [CharSequenceValidator] which validates text by regular expression
 */
open class RegexValidator(pattern: String) : CharSequenceValidator {
    private val regex = Regex(pattern)

    override fun isValid(sequence: CharSequence?): Boolean {
        sequence ?: return false
        return regex.matches(sequence)
    }
}