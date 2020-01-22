package org.tokend.template.features.kyc.storage

import android.content.SharedPreferences
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.extensions.tryOrNull
import org.tokend.template.features.kyc.model.KycState
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Represents single submitted KYC state storage based on SharedPreferences.
 */
class SubmittedKycStatePersistence(
        private val preferences: SharedPreferences
): ObjectPersistence<KycState.Submitted<*>> {
    private class Container(
            @SerializedName("state")
            val serializedState: JsonElement,
            @SerializedName("form_class")
            val formClassName: String,
            @SerializedName("state_class")
            val stateClassName: String
    )

    private val gson = GsonFactory().getBaseGson()

    override fun saveItem(item: KycState.Submitted<*>) {
        preferences
                .edit()
                .putString(
                        KEY,
                        gson.toJson(
                                Container(
                                        serializedState = gson.toJsonTree(item),
                                        formClassName = item.formData.javaClass.name,
                                        stateClassName = item.javaClass.name
                                )
                        )
                )
                .apply()
    }

    override fun loadItem(): KycState.Submitted<*>? {
        return preferences
                .getString(KEY, null)
                ?.let {
                    tryOrNull {
                        val container = gson.fromJson(it, Container::class.java)
                        deserializeState(container)
                    }
                }
    }

    override fun hasItem(): Boolean {
        return loadItem() != null
    }

    override fun clear() {
        preferences
                .edit()
                .remove(KEY)
                .apply()
    }

    private fun deserializeState(container: Container): KycState.Submitted<*> {
        val type = object : ParameterizedType {
            override fun getRawType(): Type {
                return Class.forName(container.stateClassName)
            }

            override fun getActualTypeArguments(): Array<Type> {
                return arrayOf(Class.forName(container.formClassName))
            }

            override fun getOwnerType(): Type? = null
        }

        return gson.fromJson(container.serializedState, type)
    }

    companion object {
        private const val KEY = "submitted_kyc"
    }
}