package org.tokend.template.extensions

import org.tokend.sdk.api.base.model.operations.InvestmentOperation
import org.tokend.sdk.api.base.model.operations.OfferMatchOperation
import org.tokend.template.data.model.OfferRecord

fun OfferMatchOperation.Companion.fromOfferRecord(record: OfferRecord): OfferMatchOperation {
    return fromOffer(record.toOffer())
}

fun InvestmentOperation.Companion.fromOfferRecord(record: OfferRecord): InvestmentOperation {
    return fromOffer(record.toOffer())
}