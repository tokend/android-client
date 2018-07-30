package org.tokend.template.util.validators

import ua.com.radiokot.pc.util.text_validators.CharSequenceValidator

open class RegexValidator(pattern: String) : CharSequenceValidator {
    private val regex = Regex(pattern)

    override fun isValid(sequence: CharSequence?): Boolean {
        sequence ?: return false
        return regex.matches(sequence)
    }
}