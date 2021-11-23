package io.tokend.template.features.kyc.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm

class ActiveKycPersistence(
    preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<ActiveKyc>(ActiveKyc::class.java, preferences, "active_kyc") {
    private class Container(
        @JsonProperty("is_missing")
        val isMissing: Boolean,
        @JsonProperty("role")
        val role: AccountRole?,
        @JsonProperty("form_data")
        val serializedForm: JsonNode?
    )

    override fun serializeItem(item: ActiveKyc): String {
        return mapper.writeValueAsString(
            when (item) {
                is ActiveKyc.Missing -> Container(
                    isMissing = true,
                    role = null,
                    serializedForm = null,
                )
                is ActiveKyc.Form -> Container(
                    isMissing = false,
                    role = item.formData.role,
                    serializedForm = mapper.valueToTree(item.formData),
                )
            }
        )
    }

    override fun deserializeItem(serialized: String): ActiveKyc? {
        return try {
            val container = mapper.readValue(serialized, Container::class.java)
            if (container.isMissing)
                ActiveKyc.Missing
            else
                ActiveKyc.Form(KycForm.fromJson(container.serializedForm!!, container.role!!))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}