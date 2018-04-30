package ua.com.radiokot.pc.util.text_validators

interface CharSequenceValidator {
    fun isValid(sequence: CharSequence?): Boolean
}