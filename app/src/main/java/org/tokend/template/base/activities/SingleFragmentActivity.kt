package org.tokend.template.base.activities

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import org.tokend.template.R
import org.tokend.template.base.fragments.SendFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.fragments.WalletFragment
import org.tokend.template.base.logic.payment.PaymentRequest
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.util.FragmentFactory

class SingleFragmentActivity : BaseActivity(), WalletEventsListener {
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
                .add(R.id.wallet_fragment_container, fragment)
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

    override fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest) {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest) {
        setResult(Activity.RESULT_OK)
    }

    companion object {
        const val SCREEN_ID = "screenId"
        const val ASSET_EXTRA = "asset"
    }
}

