package io.tokend.template.features.urlconfig.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.extensions.tryOrNull
import io.tokend.template.features.urlconfig.model.InvalidUrlConfigSourceException
import io.tokend.template.features.urlconfig.model.UrlConfig
import io.tokend.template.logic.providers.UrlConfigProvider
import okhttp3.HttpUrl
import org.tokend.sdk.factory.GsonFactory

/**
 * Updates [urlConfigProvider] with the [UrlConfig] obtained from the [scannedContent].
 *
 * May perform [UpdateUrlConfigFromWebClientUseCase] if [scannedContent] is a web client URL.
 */
class UpdateUrlConfigFromScannedUseCase(
    private val scannedContent: String,
    private val urlConfigProvider: UrlConfigProvider
) {
    private var parsedUrlConfig: UrlConfig? = null
    private var parsedWebClientUrl: HttpUrl? = null

    fun perform(): Completable {
        parsedUrlConfig = getParsedUrlConfig()
        parsedWebClientUrl = getParsedWebClientUrl()

        return when {
            parsedUrlConfig != null ->
                applyParsedConfig()
            parsedWebClientUrl != null ->
                updateConfigFromWebClientUrl()
            else ->
                Single.error(InvalidUrlConfigSourceException(scannedContent))
        }.ignoreElement()
    }

    private fun getParsedUrlConfig(): UrlConfig? = tryOrNull {
        GsonFactory().getBaseGson().fromJson(scannedContent, UrlConfig::class.java)
            .apply {
                assert(api.isNotEmpty())
                assert(storage.isNotEmpty())
                assert(client.isNotEmpty())
            }
    }

    private fun getParsedWebClientUrl(): HttpUrl? = tryOrNull {
        HttpUrl.parse(scannedContent)
    }

    private fun applyParsedConfig(): Single<Boolean> {
        urlConfigProvider.setConfig(parsedUrlConfig!!)
        return Single.just(true)
    }

    private fun updateConfigFromWebClientUrl(): Single<Boolean> {
        return UpdateUrlConfigFromWebClientUseCase(
            parsedWebClientUrl!!,
            urlConfigProvider
        )
            .perform()
            .toSingleDefault(true)
    }
}