package org.tokend.template.activities.settings

import org.jetbrains.anko.browse
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.util.Navigator

class GeneralSettingsFragment : SettingsFragment() {
    override fun getScreenKey(): String? = null

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