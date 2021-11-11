package io.tokend.template.features.account.data.model

import com.google.gson.annotations.SerializedName
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord

/**
 * A role completed with it's actual ID,
 * which is defined by the environment key-value
 */
data class ResolvedAccountRole(
    @SerializedName("id")
    val id: Long,
    @SerializedName("role")
    val role: AccountRole,
) {
    constructor(
        id: Long,
        keyValueEntries: Collection<KeyValueEntryRecord>
    ) : this(
        id = id,
        role =
        keyValueEntries
            .find {
                it is KeyValueEntryRecord.Number
                        && it.key.startsWith(AccountRole.KEY_PREFIX)
                        && it.value == id
            }
            ?.let { AccountRole.valueOfKey(it.key) }
            ?: AccountRole.UNKNOWN
            // Throw the exception instead of UNKNOWN to limit access to the app for unknown roles.
            //?: throw NoSuchAccountRoleException(id.toString())
    )
}