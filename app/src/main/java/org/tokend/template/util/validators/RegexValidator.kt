package ua.com.radiokot.pc.util.text_validators

/**
 * Created by Oleg Koretsky on 2/13/18.
 */
open class RegexValidator(pattern: String) : CharSequenceValidator {
    private val regex = Regex(pattern)

    override fun isValid(sequence: CharSequence?): Boolean {
        sequence ?: return false
        return regex.matches(sequence)
    }
}