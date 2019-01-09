package org.tokend.template.features.limits

import android.content.Context
import android.view.ViewGroup
import org.tokend.sdk.api.accounts.model.limits.LimitEntry
import org.tokend.template.R
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.wallet.xdr.StatsOpType

class LimitCardsProvider(context: Context,
                         asset: String,
                         entries: Collection<LimitEntry>,
                         private val amountFormatter: AmountFormatter) {

    private val cards = arrayListOf<LimitCard>()
    private val limitTypes = context.resources.getStringArray(R.array.limit_types)

    init {
        val paymentEntry = entries.find { it.limit.statsOpType == StatsOpType.PAYMENT_OUT}
        val depositEntry = entries.find { it.limit.statsOpType == StatsOpType.WITHDRAW}

        LimitCard(context,
                limitTypes[0],
                asset,
                paymentEntry?.limit?.daily,
                paymentEntry?.statistics?.daily,
                depositEntry?.limit?.daily,
                depositEntry?.statistics?.daily,
                amountFormatter)
                .also { cards.add(it) }

        LimitCard(context,
                limitTypes[1],
                asset,
                paymentEntry?.limit?.weekly,
                paymentEntry?.statistics?.weekly,
                depositEntry?.limit?.weekly,
                depositEntry?.statistics?.weekly,
                amountFormatter)
                .also { cards.add(it) }

        LimitCard(context,
                limitTypes[2],
                asset,
                paymentEntry?.limit?.monthly,
                paymentEntry?.statistics?.monthly,
                depositEntry?.limit?.monthly,
                depositEntry?.statistics?.monthly,
                amountFormatter)
                .also { cards.add(it) }

        LimitCard(context,
                limitTypes[3],
                asset,
                paymentEntry?.limit?.annual,
                paymentEntry?.statistics?.annual,
                depositEntry?.limit?.annual,
                depositEntry?.statistics?.annual,
                amountFormatter)
                .also { cards.add(it) }
    }

    fun addTo(rootLayout: ViewGroup) {
        cards.forEach { card ->
            card.addTo(rootLayout)
        }
    }
}