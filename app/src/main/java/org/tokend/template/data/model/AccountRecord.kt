package org.tokend.template.data.model

import org.json.JSONException
import org.json.JSONObject
import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIdResource
import java.io.Serializable
import java.util.*

class AccountRecord(
        val id: String,
        val depositAccounts: List<DepositAccount>
) : Serializable {
    constructor(source: AccountResource) : this(
            id = source.id,
            depositAccounts = source.externalSystemIds?.map { DepositAccount.fromResource(it) }
                    ?: emptyList()
    )

    class DepositAccount(
            val type: Int,
            val address: String,
            val payload: String?,
            val expirationDate: Date?
    ) : Serializable {

        companion object {
            private const val TYPE_ADDRESS_WITH_PAYLOAD = "address_with_payload"
            private const val TYPE_ADDRESS = "address"

            private const val FIELD_TYPE = "type"
            private const val FIELD_PAYLOAD = "payload"
            private const val FIELD_ADDRESS = "address"
            private const val FIELD_DATA = "data"

            fun fromResource(source: ExternalSystemIdResource): DepositAccount {
                var payload: String? = null

                val address = try {
                    val data = JSONObject(source.data)
                    val addressData = data.getJSONObject(FIELD_DATA)

                    if (data.getString(FIELD_TYPE) == TYPE_ADDRESS_WITH_PAYLOAD) {
                        payload = addressData.getString(FIELD_PAYLOAD)
                    }

                    addressData.getString(FIELD_ADDRESS)
                } catch (e: JSONException) {
                    source.data
                }

                return DepositAccount(source.externalSystemType, address, payload, source.expiresAt)
            }
        }
    }
}