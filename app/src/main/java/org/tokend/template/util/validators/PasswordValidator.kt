package ua.com.radiokot.pc.util.text_validators

object PasswordValidator :
        RegexValidator("^.{6,}$")