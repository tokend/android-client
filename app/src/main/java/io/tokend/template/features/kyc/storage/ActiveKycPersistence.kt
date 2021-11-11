package io.tokend.template.features.kyc.storage

import android.content.SharedPreferences
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm

class ActiveKycPersistence(
    preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<ActiveKyc>(ActiveKyc::class.java, preferences, "active_kyc") {
    private class Container(
        @SerializedName("is_missing")
        val isMissing: Boolean,
        @SerializedName("role")
        val role: AccountRole?,
        @SerializedName("form_data")
        val serializedForm: JsonElement?
    )

    override fun serializeItem(item: ActiveKyc): String {
        return gson.toJson(
            when (item) {
                is ActiveKyc.Missing -> Container(
                    isMissing = true,
                    role = null,
                    serializedForm = null,
                )
                is ActiveKyc.Form -> Container(
                    isMissing = false,
                    role = item.formData.getRole(),
                    serializedForm = gson.toJsonTree(item.formData),
                )
            }
        )
    }

    override fun deserializeItem(serialized: String): ActiveKyc? {
        return try {
            val container = gson.fromJson(serialized, Container::class.java)
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