package org.tokend.template.util.validator

/**
 * Validator of emails with Android's email address pattern
 */
object EmailValidator : RegexValidator(android.util.Patterns.EMAIL_ADDRESS.pattern())