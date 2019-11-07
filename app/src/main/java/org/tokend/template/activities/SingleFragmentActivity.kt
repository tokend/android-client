package org.tokend.template.activities

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.rxkotlin.addTo
import org.tokend.template.R
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.assets.buy.view.AtomicSwapAsksFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.qr.ShareQrFragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.WalletEventsListener

class SingleFragmentActivity : BaseActivity(), WalletEventsListener {
    private var screenId: Long = 0
    private var onBackPressedListener: OnBackPressedListener? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_single_fragment)
        window.setBackgroundDrawable(null)

        screenId = intent.getLongExtra(FRAGMENT_ID_EXTRA, 0)

        val fragment = getFragment()
        if (fragment != null) {
            displayFragment(fragment)
        } else {
            finishWithError(IllegalArgumentException("No fragment found for ID $screenId"))
        }
    }

    private fun getFragment(): Fragment? {
        val bundle = intent.getBundleExtra(FRAGMENT_BUNDLE_EXTRA)

        return when (screenId) {
            SendFragment.ID -> bundle?.let(SendFragment.Companion::newInstance)
            DepositFragment.ID -> bundle?.let(DepositFragment.Companion::newInstance)
            WithdrawFragment.ID -> bundle?.let(WithdrawFragment.Companion::newInstance)
            ExploreAssetsFragment.ID -> ExploreAssetsFragment()
            ShareQrFragment.ID -> bundle?.let(ShareQrFragment.Companion::newInstance)
            AtomicSwapAsksFragment.ID -> bundle?.let(AtomicSwapAsksFragment.Companion::newInstance)
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
                        fragmentToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
                        fragmentToolbar.setNavigationOnClickListener { onBackPressed() }
                        fragment.onCreateOptionsMenu(fragmentToolbar.menu, menuInflater)
                    }
                    .addTo(compositeDisposable)
        }

        if (fragment is OnBackPressedListener) {
            this.onBackPressedListener = fragment
        }
    }

    override fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest) {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest) {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        if (onBackPressedListener?.onBackPressed() != false) {
            super.onBackPressed()
        }
    }

    companion object {
        private const val FRAGMENT_ID_EXTRA = "screenId"
        private const val FRAGMENT_BUNDLE_EXTRA = "fragment_bundle"

        fun getBundle(fragmentId: Long,
                      fragmentBundle: Bundle?) = Bundle().apply {
            putLong(FRAGMENT_ID_EXTRA, fragmentId)
            putBundle(FRAGMENT_BUNDLE_EXTRA, fragmentBundle)
        }
    }
}

