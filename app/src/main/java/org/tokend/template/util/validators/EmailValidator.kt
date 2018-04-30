package ua.com.radiokot.pc.util.text_validators

object EmailValidator : RegexValidator(android.util.Patterns.EMAIL_ADDRESS.pattern())