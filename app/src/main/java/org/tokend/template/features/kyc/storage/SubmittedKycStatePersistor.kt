package org.tokend.template.features.kyc.storage

import android.content.SharedPreferences
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.extensions.tryOrNull
import org.tokend.template.features.kyc.model.KycState
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Represents single submitted KYC state storage based on SharedPreferences.
 */
class SubmittedKycStatePersistor(
        private val preferences: SharedPreferences
) {
    private class Container(
            @SerializedName("state")
            val serializedState: JsonElement,
            @SerializedName("form_class")
            val formClassName: String,
            @SerializedName("state_class")
            val stateClassName: String
    )

    private val gson = GsonFactory().getBaseGson()

    /**
     * Saves given [state]
     */
    fun saveState(state: KycState.Submitted<*>) {
        preferences
                .edit()
                .putString(
                        KEY,
                        gson.toJson(
                                Container(
                                        serializedState = gson.toJsonTree(state),
                                        formClassName = state.formData.javaClass.name,
                                        stateClassName = state.javaClass.name
                                )
                        )
                )
                .apply()
    }

    /**
     * @return saved state
     */
    fun loadState(): KycState.Submitted<*>? {
        return preferences
                .getString(KEY, null)
                ?.let {
                    tryOrNull {
                        val container = gson.fromJson(it, Container::class.java)
                        deserializeState(container)
                    }
                }
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