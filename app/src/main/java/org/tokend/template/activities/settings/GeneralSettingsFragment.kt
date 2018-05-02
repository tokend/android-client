package org.tokend.template.activities.settings

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.LinearLayout
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.browse
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.Navigator

class GeneralSettingsFragment : SettingsFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    override fun getScreenKey(): String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Include toolbar.
        if (view is LinearLayout) {
            val appbar = layoutInflater.inflate(R.layout.appbar, view, false)
            view.addView(appbar, 0)

            toolbar.title = getString(R.string.settings_title)
            toolbarSubject.onNext(toolbar)
        }
    }

    override fun reloadPreferences() {
        super.reloadPreferences()

        initAccountCategory()
    }

    // region Account
    private fun initAccountCategory() {
        initAccountIdItem()
        initKycItem()
    }

    private fun initAccountIdItem() {
        val accountIdPreference = findPreference("account_id")
        accountIdPreference?.setOnPreferenceClickListener {
            val accountId = "account id"
            activity?.let { parentActivity ->
                Navigator.openQrShare(parentActivity,
                        data = accountId,
                        title = getString(R.string.account_id_title),
                        shareDialogText = getString(R.string.share_account_id)
                )
            }

            true
        }
    }

    private fun initKycItem() {
        val kycPreference = findPreference("kyc")
        kycPreference?.setOnPreferenceClickListener {
            activity?.browse(BuildConfig.WEB_CLIENT_URL, true)
            true
        }
    }
    // endregion
}