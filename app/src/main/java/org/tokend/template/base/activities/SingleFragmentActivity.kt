package org.tokend.template.base.activities

import android.app.Activity
import android.app.FragmentTransaction
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import org.tokend.template.R
import org.tokend.template.base.fragments.SendFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.fragments.WalletFragment
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.util.FragmentFactory

class SingleFragmentActivity : BaseActivity() {
    private var asset: String? = null
    private var screenId: Long? = null
    private val factory = FragmentFactory()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_explore_asset)
        asset = intent.getStringExtra(ASSET_EXTRA)
        screenId = intent.getLongExtra(SCREEN_ID, DashboardFragment.ID)

        getFragment()?.also { displayFragment(it) }
                ?: finish()
    }

    private fun getFragment(): Fragment? {
        return when (screenId) {
            WalletFragment.ID -> factory.getWalletFragment(asset)
            SendFragment.ID -> factory.getSendFragment(asset)
            else -> null
        }
    }

    private fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.wallet_fragment_container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()

        // Bind navigation drawer to fragment's toolbar.
        if (fragment is ToolbarProvider) {
            fragment.toolbarSubject
                    .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                    .subscribe { fragmentToolbar ->
                        setSupportActionBar(fragmentToolbar)
                        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                        supportActionBar!!.title = asset
                    }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SendFragment.PAYMENT_CONFIRMATION_REQUEST -> finish()
            }
        }
    }

    companion object {
        const val SCREEN_ID = "screenId"
        const val ASSET_EXTRA = "asset"
    }
}

