package org.tokend.template.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.settings.view.PreferenceDividerDecoration
import org.tokend.template.view.ToastManager
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
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

        listView.addItemDecoration(
                PreferenceDividerDecoration(context,
                        R.drawable.line_divider, R.dimen.divider_height)
                        .setPaddingLeft(resources
                                .getDimensionPixelSize(R.dimen.divider_with_icon_padding_left))
                        .drawBetweenItems(true)
                        .drawTop(false)
                        .drawBottom(false)
                        .drawBetweenCategories(false))
    }

    protected fun updateSummary(key: String, value: String) {
        val preference = findPreference(key)
        if (preference != null) {
            preference.summary = value
        }
    }

    protected fun updateSummary(key: String) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.dispose()
    }

    companion object {
        const val ID = 1116L
    }
}