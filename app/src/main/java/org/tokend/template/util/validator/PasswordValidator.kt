package org.tokend.template.util.validator

object PasswordValidator :
        RegexValidator("^.{6,}$")