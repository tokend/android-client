package org.tokend.template.features.invest.view

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_multiple_fragments.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.invest.logic.InvestmentInfoHolder
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.repository.InvestmentInfoRepository
import org.tokend.template.features.invest.view.fragments.SalePagerAdapter

class SaleActivity : BaseActivity(), InvestmentInfoHolder {
    private lateinit var mSale: SaleRecord
    override val sale: SaleRecord
        get() = mSale

    private lateinit var mInvestmentInfoRepository: InvestmentInfoRepository
    override val investmentInfoRepository: InvestmentInfoRepository
        get() = mInvestmentInfoRepository

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_multiple_fragments)

        try {
            mSale = intent.getSerializableExtra(SALE_EXTRA) as SaleRecord
        } catch (e: Exception) {
            finish()
            return
        }

        mInvestmentInfoRepository = InvestmentInfoRepository(
                sale,
                repositoryProvider.offers(),
                repositoryProvider.sales()
        ).also { it.update() }

        supportPostponeEnterTransition()

        initToolbar()
        initViewPager()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = sale.name
    }

    private fun initViewPager() {
        val adapter = SalePagerAdapter(this, supportFragmentManager)
        pager.adapter = adapter
        pager.offscreenPageLimit = adapter.count
        toolbar_tabs.setupWithViewPager(pager)
    }

    companion object {
        const val SALE_EXTRA = "sale"
    }
}