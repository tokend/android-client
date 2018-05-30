package org.tokend.template.base.activities

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.app.FragmentTransaction
import android.support.v7.widget.Toolbar
import com.mikepenz.materialdrawer.Drawer
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.base.fragments.SendFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.fragments.WalletFragment
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.explore.ExploreAssetsFragment

class SingleFragmentActivity : BaseActivity() {

    private var asset: String? = null
    private var screenId : Long? = null

    private var fragment: Fragment? = null

    private var navigationDrawer: Drawer? = null


    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_explore_asset)
        asset = intent.getStringExtra(ASSET_EXTRA)
        screenId = intent.getLongExtra(SCREEN_ID,DashboardFragment.ID)

        initFragment()
        displayFragment(fragment!!)
    }

    private fun initFragment() {
        when(screenId){
            WalletFragment.ID -> fragment = WalletFragment.newInstance(asset)
            SendFragment.ID -> fragment = SendFragment.newInstance(asset)
        }
    }

    private fun displayFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
                .replace(R.id.wallet_fragment_container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()


        // Bind navigation drawer to fragment's toolbar.
        var fragmentToolbarDisposable: Disposable? = null

        fragmentToolbarDisposable?.dispose()
        if (fragment is ToolbarProvider) {
            fragmentToolbarDisposable = fragment.toolbarSubject
                    .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                    .subscribe { fragmentToolbar ->
                        navigationDrawer?.setToolbar(this, fragmentToolbar,
                                true)
                        setSupportActionBar(fragmentToolbar)
                        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                    }

        }
    }

    companion object {
        private const val SCREEN_ID = "screenId"
        private const val ASSET_EXTRA = "asset"
    }
}

