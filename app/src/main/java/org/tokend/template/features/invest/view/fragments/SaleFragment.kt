package org.tokend.template.features.invest.view.fragments

import org.tokend.template.features.invest.logic.InvestmentInfoHolder
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.repository.InvestmentInfoRepository
import org.tokend.template.fragments.BaseFragment

abstract class SaleFragment: BaseFragment() {
    protected lateinit var investmentInfoRepository: InvestmentInfoRepository
    protected lateinit var initSale: SaleRecord

    protected val sale: SaleRecord
        get() = investmentInfoRepository.item?.detailedSale ?: initSale

    override fun onInitAllowed() {
        initSale = (requireActivity() as? InvestmentInfoHolder)
                ?.sale
                ?: throw IllegalStateException("Parent activity must hold SaleRecord")

        investmentInfoRepository = (requireActivity() as? InvestmentInfoHolder)
                ?.investmentInfoRepository
                ?: throw IllegalStateException("Parent activity must hold InvestmentInfoRepository")
    }
}