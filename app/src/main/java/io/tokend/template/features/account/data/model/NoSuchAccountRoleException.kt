package io.tokend.template.features.account.data.model

class NoSuchAccountRoleException(role: String):
    IllegalStateException("There is no such account role ($role)")
