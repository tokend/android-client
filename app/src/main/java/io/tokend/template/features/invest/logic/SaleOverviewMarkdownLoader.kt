package io.tokend.template.features.invest.logic

import android.content.Context
import io.reactivex.Single
import io.tokend.template.BuildConfig
import io.tokend.template.data.repository.BlobsRepository
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.factory.HttpClientFactory
import org.tokend.sdk.factory.JsonApiTools
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.il.AsyncDrawableLoader
import ru.noties.markwon.spans.SpannableTheme

/**
 * Loads sale overview blob content and transforms it to markdown
 */
class SaleOverviewMarkdownLoader(
    context: Context,
    private val blobsRepository: BlobsRepository
) {
    private val markdownConfiguration = SpannableConfiguration
        .builder(context)
        .theme(
            SpannableTheme
                .builderWithDefaults(context)
                .headingBreakHeight(0)
                .thematicBreakHeight(0)
                .build()
        )
        .asyncDrawableLoader(
            AsyncDrawableLoader
                .builder()
                .client(
                    HttpClientFactory()
                        .getBaseHttpClientBuilder(
                            withLogs = BuildConfig.WITH_LOGS
                        )
                        .build()
                )
                .build()
        )
        .build()

    fun load(blobId: String): Single<CharSequence> {
        return blobsRepository
            .getById(blobId)
            .map(Blob::valueString)
            .map { rawValue ->
                try {
                    // Unescape content.
                    JsonApiTools.objectMapper
                        .readTree("\"${rawValue}\"")
                        .asText()
                } catch (e: Exception) {
                    rawValue
                }
            }
            .map { markdownString ->
                Markwon.markdown(markdownConfiguration, markdownString)
            }
    }
}