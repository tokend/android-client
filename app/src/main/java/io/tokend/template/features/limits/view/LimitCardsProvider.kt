package io.tokend.template.features.limits.view

import android.content.Context
import android.view.ViewGroup
import io.tokend.template.view.util.LocalizedName
import io.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.sdk.api.accounts.model.limits.LimitEntry

class LimitCardsProvider(
    context: Context,
    asset: String,
    entries: Collection<LimitEntry>,
    amountFormatter: AmountFormatter
) {

    private val cards = arrayListOf<LimitCard>()

    init {
        val localizedName = LocalizedName(context)

        entries.forEach { entry ->
            LimitCard(
                context,
                localizedName.forLimitType(entry.limit.statsOpType),
                asset,
                entry,
                amountFormatter
            )
                .also { cards.add(it) }
        }
    }

    fun addTo(rootLayout: ViewGroup) {
        cards.forEach { card ->
            card.addTo(rootLayout)
        }
    }
}