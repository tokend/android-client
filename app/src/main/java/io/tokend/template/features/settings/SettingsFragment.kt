package io.tokend.template.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import io.reactivex.disposables.CompositeDisposable
import io.tokend.template.App
import io.tokend.template.R
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import io.tokend.template.features.settings.view.PreferenceDividerDecoration
import io.tokend.template.logic.session.Session
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.persistence.BackgroundLockManager
import io.tokend.template.util.errorhandler.ErrorHandlerFactory
import io.tokend.template.util.locale.AppLocaleManager
import io.tokend.template.view.ToastManager
import io.tokend.template.view.util.formatter.AmountFormatter
import javax.inject.Inject

abstract class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    lateinit var walletInfoProvider: WalletInfoProvider

    @Inject
    lateinit var repositoryProvider: RepositoryProvider

    @Inject
    lateinit var urlConfigProvider: UrlConfigProvider

    @Inject
    lateinit var errorHandlerFactory: ErrorHandlerFactory

    @Inject
    lateinit var toastManager: ToastManager

    @Inject
    lateinit var accountProvider: AccountProvider

    @Inject
    lateinit var session: Session

    @Inject
    lateinit var amountFormatter: AmountFormatter

    @Inject
    lateinit var localeManager: AppLocaleManager

    @Inject
    lateinit var credentialsPersistence: CredentialsPersistence

    @Inject
    lateinit var backgroundLockManager: BackgroundLockManager

    @Inject
    lateinit var mnemonicCode: MnemonicCode

    protected val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as? App)?.stateComponent?.inject(this)

        reloadPreferences()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    protected open fun reloadPreferences() {
        preferenceScreen = null
        setPreferencesFromResource(R.xml.preferences, getScreenKey())

        preferenceScreen.shouldDisableView = true
    }

    protected abstract fun getScreenKey(): String?

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))

        listView.addItemDecoration(
            PreferenceDividerDecoration(
                context,
                R.drawable.line_divider, R.dimen.divider_height
            )
                .setPaddingLeft(
                    resources
                        .getDimensionPixelSize(R.dimen.divider_with_icon_padding_left)
                )
                .setPaddingRignt(
                    resources
                        .getDimensionPixelSize(R.dimen.standard_margin)
                )
                .drawBetweenItems(true)
                .drawTop(false)
                .drawBottom(false)
                .drawBetweenCategories(false)
        )
    }

    private fun updateSummary(key: String, value: String) {
        val preference = findPreference(key)
        if (preference != null) {
            preference.summary = value
        }
    }

    private fun updateSummary(key: String) {
        val preference = findPreference(key)
        if (preference != null && preference is ListPreference) {
            val entry = preference.entry
            if (entry != null) {
                updateSummary(key, entry.toString())
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updateSummary(key)
    }

    protected open fun hideCategoryIfEmpty(key: String) {
        (findPreference(key) as? PreferenceGroup)?.apply {
            isVisible = preferenceCount > 0
                    && (0 until preferenceCount)
                .map(this::getPreference)
                .any(Preference::isVisible)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.dispose()
    }
}