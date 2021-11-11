package io.tokend.template.features.account.data.model

/**
 * @see ResolvedAccountRole
 */
enum class AccountRole {
    // Do not resolve this role to limit access to the app for unknown roles.
    UNKNOWN(""),

    BLOCKED("blocked"),
    UNVERIFIED("unverified"),
    GENERAL("general"),
    CORPORATE("corporate"),
    ;

    val key: String
    private val isUnknown: Boolean

    companion object {
        const val KEY_PREFIX = "account_role"

        /**
         * @see NoSuchAccountRoleException
         */
        fun valueOfKey(key: String) =
            values().find { it.key == key }
                ?: throw NoSuchAccountRoleException(key)

        /**
         * @return values of all the keys except for the unknown ones
         */
        fun keyValues() =
            values()
                .filterNot(AccountRole::isUnknown)
                .map(AccountRole::key)
    }

    constructor(key: String) {
        this.key = "$KEY_PREFIX:$key"
        this.isUnknown = key.isEmpty()
    }
}
