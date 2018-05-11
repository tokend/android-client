package org.tokend.template.base.fragments.settings

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.LinearLayout
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.browse
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.base.fragments.ToolbarProvider
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
        initSecurityCategory()
    }

    // region Account
    private fun initAccountCategory() {
        initAccountIdItem()
        initKycItem()
    }

    private fun initAccountIdItem() {
        val accountIdPreference = findPreference("account_id")
        accountIdPreference?.setOnPreferenceClickListener {
            val accountId = walletInfoProvider.getWalletInfo()?.accountId
                    ?: getString(R.string.error_try_again)
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

    // region Security
    private fun initSecurityCategory() {
        initChangePasswordItem()
    }

    private fun initChangePasswordItem() {
        val changePasswordPreference = findPreference("change_password")
        changePasswordPreference?.setOnPreferenceClickListener {
            activity?.let { parentActivity ->
                Navigator.openPasswordChange(parentActivity, 3597)
            }

            true
        }
    }
    // endregion
}