package org.tokend.template.activities

import android.os.Bundle
import org.tokend.template.R

class DashboardActivity : NavigationActivity() {

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_dashboard)

        initToolbar(R.string.dashboard_title)
        initNavigationDrawer()
    }

    override fun getSelectedNavigationItemId(): Long = DASHBOARD_ITEM
}
