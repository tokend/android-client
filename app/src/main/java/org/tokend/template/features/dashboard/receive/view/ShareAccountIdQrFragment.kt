package org.tokend.template.features.dashboard.receive.view

import android.view.View
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.features.qr.ShareQrFragment

class ShareAccountIdQrFragment : ShareQrFragment() {

    override val data: String
        get() = walletInfoProvider.getWalletInfo()?.accountId ?: ""

    override val shareDialogText: String
        get() = getString(R.string.share_account_id)

    override fun onInitAllowed() {
        super.onInitAllowed()
        toolbar.visibility = View.GONE
    }
}