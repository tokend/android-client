package org.tokend.template.features.invest.logic

import android.content.Context
import com.google.gson.JsonParser
import io.reactivex.Single
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.factory.HttpClientFactory
import org.tokend.template.BuildConfig
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.il.AsyncDrawableLoader
import ru.noties.markwon.spans.SpannableTheme

/**
 * Loads sale overview blob content and transforms it to markdown
 */
class SaleOverviewMarkdownLoader(context: Context,
                                 private val blobManager: BlobManager) {
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
        return blobManager
                .getBlob(blobId)
                .map(Blob::valueString)
                .map { rawValue ->
                    try {
                        // Unescape content.
                        JsonParser().parse(rawValue).asString
                    } catch (e: Exception) {
                        rawValue
                    }
                }
                .map { markdownString ->
                    Markwon.markdown(markdownConfiguration, markdownString)
                }
    }
}