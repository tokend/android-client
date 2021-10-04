package org.tokend.template.features.kyc.storage

import android.content.SharedPreferences
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.model.KycForm

class ActiveKycPersistence(
        preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<ActiveKyc>(ActiveKyc::class.java, preferences, "active_kyc") {
    private class Container(
            @SerializedName("is_missing")
            val isMissing: Boolean,
            @SerializedName("form_class")
            val formClass: String?,
            @SerializedName("form_data")
            val serializedForm: JsonElement?
    )

    override fun serializeItem(item: ActiveKyc): String {
        return gson.toJson(
                when (item) {
                    is ActiveKyc.Missing -> Container(
                            isMissing = true,
                            serializedForm = null,
                            formClass = null
                    )
                    is ActiveKyc.Form -> Container(
                            isMissing = false,
                            serializedForm = gson.toJsonTree(item.formData),
                            formClass = item.formData::class.java.name
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
                ActiveKyc.Form(gson.fromJson<KycForm>(
                        container.serializedForm!!,
                        Class.forName(container.formClass!!)
                ))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}