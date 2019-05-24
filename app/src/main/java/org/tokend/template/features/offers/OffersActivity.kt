package org.tokend.template.features.offers

import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_white.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.fragments.FragmentFactory

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
                .add(R.id.container, FragmentFactory().getOffersFragment(onlyPrimary))
                .commit()
    }

    companion object {
        const val ONLY_PRIMARY_EXTRA = "only_primary"
    }
}
