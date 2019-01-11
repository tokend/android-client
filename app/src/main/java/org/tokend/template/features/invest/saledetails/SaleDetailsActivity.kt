package org.tokend.template.features.invest.saledetails

import android.os.Bundle
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.activity_sale_details.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.getNullableStringExtra
import org.tokend.template.features.invest.activities.SaleActivity
import org.tokend.template.features.invest.model.SaleRecord

class SaleDetailsActivity : BaseActivity() {
    private lateinit var sale: SaleRecord

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sale_details)

        try {
            sale = GsonFactory().getBaseGson().fromJson(
                    intent.getNullableStringExtra(SaleActivity.SALE_JSON_EXTRA),
                    SaleRecord::class.java)
        } catch (e: JsonSyntaxException) {
            finish()
            return
        }

        supportPostponeEnterTransition()

        initToolbar()
        initViewPager()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initViewPager() {
        val adapter = SaleDetailsPagerAdapter(sale, this, supportFragmentManager)
        pager.adapter = adapter
        toolbar_tabs.setupWithViewPager(pager)
    }

    companion object {
        const val SALE_JSON_EXTRA = "sale_json"
    }
}
