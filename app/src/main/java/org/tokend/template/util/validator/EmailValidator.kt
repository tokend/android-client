package org.tokend.template.util.validator

object EmailValidator : RegexValidator(android.util.Patterns.EMAIL_ADDRESS.pattern())