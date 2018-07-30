package ua.com.radiokot.pc.util.text_validators

import org.tokend.template.util.validators.RegexValidator

object EmailValidator : RegexValidator(android.util.Patterns.EMAIL_ADDRESS.pattern())