package io.tokend.template.features.account.data.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord

/**
 * A role completed with it's actual ID,
 * which is defined by the environment key-value
 */
data class ResolvedAccountRole(
    @JsonProperty("id")
    val id: Long,
    @JsonProperty("role")
    val role: AccountRole,
) {
    constructor(
        id: Long,
        keyValueEntries: Collection<KeyValueEntryRecord>
    ) : this(
        id = id,
        role =
        (keyValueEntries
            .find {
                it is KeyValueEntryRecord.Number
                        && it.key.startsWith(AccountRole.KEY_PREFIX)
                        && it.value == id
            }
            ?.let { AccountRole.valueOfKeyOrUnknown(it.key) }
            ?: AccountRole.UNKNOWN)
            // Throw the exception instead of UNKNOWN to limit access to the app for unknown roles.
//            .also { role ->
//                if (role == AccountRole.UNKNOWN) {
//                    throw NoSuchAccountRoleException(id.toString())
//                }
//            }
    )
}