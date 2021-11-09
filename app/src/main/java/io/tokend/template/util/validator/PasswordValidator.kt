package io.tokend.template.util.validator

/**
 * Validator of password strength
 */
object PasswordValidator :
    RegexValidator("^.{6,}$")