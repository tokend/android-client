package org.tokend.template.activities

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.rxkotlin.addTo
import org.tokend.template.R
import org.tokend.template.extensions.getStringExtra
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.dashboard.view.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.qr.ShareQrFragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.fragments.FragmentFactory
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.wallet.WalletEventsListener

class SingleFragmentActivity : BaseActivity(), WalletEventsListener {
    private var asset: String? = null
    private var screenId: Long? = null
    private var title: String? = null
    private var data: String? = null
    private var shareDialogText: String? = null
    private var shareText: String? = null
    private var topText: String? = null
    private val factory = FragmentFactory()
    private var onBackPressedListener: OnBackPressedListener? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_single_fragment)
        asset = intent.getStringExtra(ASSET_EXTRA)
        screenId = intent.getLongExtra(SCREEN_ID, DashboardFragment.ID)
        title = intent.getStringExtra(TITLE_EXTRA, "")
        data = intent.getStringExtra(DATA_EXTRA, "")
        shareDialogText = intent.getStringExtra(SHARE_DIALOG_TEXT_EXTRA, "")
        shareText = intent.getStringExtra(SHARE_TEXT_EXTRA, data ?: "")
        topText = intent.getStringExtra(TOP_TEXT_EXTRA, "")

        getFragment()?.also { displayFragment(it) }
                ?: finish()
    }

    private fun getFragment(): Fragment? {
        return when (screenId) {
            WalletFragment.ID -> factory.getWalletFragment(asset, false)
            SendFragment.ID -> factory.getSendFragment(asset)
            DepositFragment.ID -> factory.getDepositFragment(asset)
            WithdrawFragment.ID -> factory.getWithdrawFragment(asset)
            ExploreAssetsFragment.ID -> factory.getExploreFragment()
            ShareQrFragment.ID -> factory.getShareQrFragment(
                    title,
                    data,
                    shareDialogText,
                    shareText,
                    topText
            )
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
                        fragmentToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white)
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
        const val SCREEN_ID = "screenId"
        const val ASSET_EXTRA = "asset"
        const val TITLE_EXTRA = "title"
        const val DATA_EXTRA = "data"
        const val SHARE_DIALOG_TEXT_EXTRA = "share_dialog_text"
        const val SHARE_TEXT_EXTRA = "share_text"
        const val TOP_TEXT_EXTRA = "top_text"
    }
}

