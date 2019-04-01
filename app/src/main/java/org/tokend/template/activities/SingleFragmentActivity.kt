package org.tokend.template.activities

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.rxkotlin.addTo
import org.tokend.template.R
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.fragments.FragmentFactory
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.wallet.WalletEventsListener

class SingleFragmentActivity : BaseActivity(), WalletEventsListener {
    private var asset: String? = null
    private var screenId: Long? = null
    private val factory = FragmentFactory()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_single_fragment)
        asset = intent.getStringExtra(ASSET_EXTRA)
        screenId = intent.getLongExtra(SCREEN_ID, DashboardFragment.ID)

        getFragment()?.also { displayFragment(it) }
                ?: finish()
    }

    private fun getFragment(): Fragment? {
        return when (screenId) {
            WalletFragment.ID -> factory.getWalletFragment(asset, false)
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
                    .subscribe { fragmentToolbar ->
                        setSupportActionBar(fragmentToolbar)
                        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                        supportActionBar!!.title = asset
                    }
                    .addTo(compositeDisposable)
        }
    }

    override fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest) {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest) {
        setResult(Activity.RESULT_OK)
    }

    override fun onBackPressed() {
        if (screenId == SendFragment.ID) {
            val fragment = supportFragmentManager.fragments.first() as SendFragment
            if (fragment.onBackPressed()) super.onBackPressed()
        } else super.onBackPressed()
    }

    companion object {
        const val SCREEN_ID = "screenId"
        const val ASSET_EXTRA = "asset"
    }
}

