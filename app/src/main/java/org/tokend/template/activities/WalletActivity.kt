package org.tokend.template.activities

import android.os.Bundle
import android.support.v7.widget.Toolbar
import kotlinx.android.synthetic.main.collapsing_balance_appbar.*
import org.tokend.template.R

class WalletActivity : NavigationActivity() {

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_wallet)

        initToolbar("12 TKD")
        initNavigationDrawer()
    }

    override fun getSelectedNavigationItemId(): Long = WALLET_ITEM

    override fun getToolbar(): Toolbar? {
        return toolbar
    }
}
