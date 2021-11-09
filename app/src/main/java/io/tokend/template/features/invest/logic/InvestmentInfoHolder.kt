package io.tokend.template.features.invest.logic

import io.tokend.template.features.invest.model.SaleRecord
import io.tokend.template.features.invest.repository.InvestmentInfoRepository


interface InvestmentInfoHolder {
    val sale: SaleRecord

    val investmentInfoRepository: InvestmentInfoRepository
}