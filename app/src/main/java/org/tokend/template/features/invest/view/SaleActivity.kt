package org.tokend.template.features.invest.view

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_sale.*
import kotlinx.android.synthetic.main.appbar_with_tabs.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.invest.logic.InvestmentInfoHolder
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.repository.InvestmentInfoRepository
import org.tokend.template.features.invest.view.fragments.SalePagerAdapter
import org.tokend.template.util.Navigator

class SaleActivity : BaseActivity(), InvestmentInfoHolder {
    private lateinit var mSale: SaleRecord
    override val sale: SaleRecord
        get() = mSale

    override val investmentInfoRepository: InvestmentInfoRepository
        get() = repositoryProvider.investmentInfo(sale)

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sale)

        try {
            mSale = intent.getSerializableExtra(SALE_EXTRA) as SaleRecord
        } catch (e: Exception) {
            finish()
            return
        }

        investmentInfoRepository.update()

        supportPostponeEnterTransition()

        initToolbar()
        initViewPager()
        initInvestButton()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = sale.name
    }

    private fun initViewPager() {
        val adapter = SalePagerAdapter(sale, this, supportFragmentManager)
        pager.adapter = adapter
        pager.offscreenPageLimit = adapter.count
        appbar_tabs.visibility = View.VISIBLE
        appbar_tabs.setupWithViewPager(pager)
    }

    private fun initInvestButton() {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId

        val canInvest = sale.isAvailable
                && sale.ownerAccountId != accountId

        invest_button.visibility =
                if (canInvest)
                    View.VISIBLE
                else
                    View.GONE

        invest_button.setOnClickListener {
            openInvest()
        }
    }

    private fun openInvest() {
        Navigator.from(this).openInvest(sale)
    }

    companion object {
        const val SALE_EXTRA = "sale"
    }
}