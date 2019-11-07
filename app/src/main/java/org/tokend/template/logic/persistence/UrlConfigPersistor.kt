package org.tokend.template.logic.persistence

import android.content.SharedPreferences
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.data.model.UrlConfig
import org.tokend.template.extensions.tryOrNull

/**
 * Implements [UrlConfig] storage based on [SharedPreferences]
 */
class UrlConfigPersistor(
        private val preferences: SharedPreferences
) {
    fun saveConfig(config: UrlConfig) {
        preferences
                .edit()
                .putString(
                        CONFIG_KEY,
                        GsonFactory().getBaseGson().toJson(config)
                )
                .apply()
    }

    /**
     * @return saved [UrlConfig] or null if it is absent
     */
    fun loadConfig(): UrlConfig? {
        return preferences
                .getString(CONFIG_KEY, null)
                ?.let {
                    tryOrNull {
                        GsonFactory().getBaseGson().fromJson(it, UrlConfig::class.java)
                    }
                }

    }

    companion object {
        private const val CONFIG_KEY = "url_config"
    }
}