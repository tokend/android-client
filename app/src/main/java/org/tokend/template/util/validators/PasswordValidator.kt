package ua.com.radiokot.pc.util.text_validators

import org.tokend.template.util.validators.RegexValidator

object PasswordValidator :
        RegexValidator("^.{6,}$")