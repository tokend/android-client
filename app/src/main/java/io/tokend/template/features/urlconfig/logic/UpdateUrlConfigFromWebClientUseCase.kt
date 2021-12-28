package io.tokend.template.features.urlconfig.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.tokend.template.features.urlconfig.model.InvalidUrlConfigSourceException
import io.tokend.template.features.urlconfig.model.UrlConfig
import io.tokend.template.logic.providers.UrlConfigProvider
import okhttp3.HttpUrl
import org.tokend.sdk.api.base.BaseApi
import org.tokend.sdk.api.custom.CustomRequestsService
import org.tokend.sdk.factory.ServiceFactory
import java.net.HttpURLConnection

/**
 * Updates [urlConfigProvider] with the [UrlConfig] obtained from the web client's env.js.
 */
class UpdateUrlConfigFromWebClientUseCase(
    private val webClientUrl: HttpUrl,
    private val urlConfigProvider: UrlConfigProvider,
) {
    private lateinit var urlConfig: UrlConfig

    fun perform(): Completable {
        return getUrlConfig()
            .doOnSuccess { urlConfig ->
                this.urlConfig = urlConfig
            }
            .flatMap {
                applyNewConfig()
            }
            .ignoreElement()
    }

    private fun getUrlConfig(): Single<UrlConfig> = Single.defer {
        val cleanWebClientUrl = UrlConfig(
            client = "${webClientUrl.scheme()}://${webClientUrl.host()}",
            api = "",
            storage = ""
        ).client

        val envJsResponse = ServiceFactory(
            url = cleanWebClientUrl,
            withLogs = false,
            asyncCallbackExecutor = BaseApi.DEFAULT_ASYNC_CALLBACK_EXECUTOR
        )
            .getCustomService(CustomRequestsService::class.java)
            .get(
                url = HttpUrl.get(cleanWebClientUrl)
                    .newBuilder()
                    .addEncodedPathSegments("static/env.js")
                    .build()
                    .toString(),
                headers = emptyMap(),
                query = emptyMap()
            )
            .execute()

        if (!envJsResponse.isSuccessful
            && envJsResponse.code() == HttpURLConnection.HTTP_NOT_FOUND
        ) {
            throw InvalidUrlConfigSourceException(cleanWebClientUrl)
        }

        return@defer try {
            val envJsString = envJsResponse
                .body()
                .string()

            // env.js is a JS object which can be C-R-A-Z-Y.
            // We can't parse it as JSON.

            val api = Regex("['\"]?HORIZON_SERVER['\"]?\\s*:\\s*['\"](.+?)['\"]")
                .find(envJsString)!!
                .groupValues[1]

            val storage = Regex("['\"]?FILE_STORAGE['\"]?\\s*:\\s*['\"](.+?)['\"]")
                .find(envJsString)!!
                .groupValues[1]

            val urlConfig =
                UrlConfig(
                    api = api,
                    storage = storage,
                    client = cleanWebClientUrl
                )
            Single.just(urlConfig)
        } catch (_: Exception) {
            throw InvalidUrlConfigSourceException(cleanWebClientUrl)
        }
    }.subscribeOn(Schedulers.newThread())

    private fun applyNewConfig(): Single<Boolean> {
        urlConfigProvider.setConfig(urlConfig)
        return Single.just(true)
    }
}