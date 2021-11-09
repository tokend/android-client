package io.tokend.template.features.offers

import android.os.Bundle
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.trade.offers.view.OffersFragment
import kotlinx.android.synthetic.main.toolbar.*

class OffersActivity : BaseActivity() {
    private val onlyPrimary: Boolean
        get() = intent.getBooleanExtra(ONLY_PRIMARY_EXTRA, false)

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_offers)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (onlyPrimary) {
            setTitle(R.string.pending_investments_title)
        } else {
            setTitle(R.string.pending_offers_title)
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, OffersFragment.newInstance(OffersFragment.getBundle(onlyPrimary)))
            .commit()
    }

    companion object {
        private const val ONLY_PRIMARY_EXTRA = "only_primary"

        fun getBundle(onlyPrimary: Boolean) = Bundle().apply {
            putBoolean(ONLY_PRIMARY_EXTRA, onlyPrimary)
        }
    }
}
