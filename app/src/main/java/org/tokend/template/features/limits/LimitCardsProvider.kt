package org.tokend.template.features.limits

import android.content.Context
import android.view.ViewGroup
import org.tokend.sdk.api.accounts.model.limits.LimitEntry
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.formatter.AmountFormatter

class LimitCardsProvider(context: Context,
                         asset: String,
                         entries: Collection<LimitEntry>,
                         amountFormatter: AmountFormatter) {

    private val cards = arrayListOf<LimitCard>()

    init {
        val localizedName = LocalizedName(context)

        entries.forEach { entry ->
            LimitCard(context,
                    localizedName.forLimitType(entry.limit.statsOpType),
                    asset,
                    entry,
                    amountFormatter)
                    .also { cards.add(it) }
        }
    }

    fun addTo(rootLayout: ViewGroup) {
        cards.forEach { card ->
            card.addTo(rootLayout)
        }
    }
}