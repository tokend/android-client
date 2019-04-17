package org.tokend.template.features.invest.logic

import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.repository.InvestmentInfoRepository


interface InvestmentInfoHolder {
    val sale: SaleRecord

    val investmentInfoRepository: InvestmentInfoRepository
}