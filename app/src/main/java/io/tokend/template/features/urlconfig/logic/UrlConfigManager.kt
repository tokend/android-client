package io.tokend.template.features.urlconfig.logic

import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.di.providers.UrlConfigProvider
import io.tokend.template.features.urlconfig.model.UrlConfig
import org.tokend.sdk.factory.GsonFactory

/**
 * Manages network configuration of the app.
 */
class UrlConfigManager(
    private val urlConfigProvider: UrlConfigProvider,
    private val urlConfigPersistence: ObjectPersistence<UrlConfig>
) {
    private var listener: (() -> Unit)? = null

    /**
     * Sets given config to the provider and saves it to the persist
     */
    fun setFromJson(jsonConfig: String): Boolean {
        return try {
            val config = GsonFactory().getBaseGson().fromJson(jsonConfig, UrlConfig::class.java)

            urlConfigProvider.setConfig(config)
            urlConfigPersistence.saveItem(config)

            listener?.invoke()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * @return [UrlConfig] selected by user, null if it is absent or selection is unsupported
     */
    fun get(): UrlConfig? {
        return if (urlConfigProvider.hasConfig())
            urlConfigProvider.getConfig()
        else
            null
    }

    fun onConfigUpdated(listener: () -> Unit) {
        this.listener = listener
    }
}