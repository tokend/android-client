package org.tokend.template.features.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.SwitchPreferenceCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.include_appbar_elevation.view.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.browse
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.App
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.data.repository.TfaFactorsRepository
import org.tokend.template.features.localaccount.mnemonic.view.MnemonicPhraseDialog
import org.tokend.template.features.settings.view.OpenSourceLicensesDialog
import org.tokend.template.features.tfa.logic.DisableTfaUseCase
import org.tokend.template.features.tfa.logic.EnableTfaUseCase
import org.tokend.template.features.tfa.model.TfaFactorRecord
import org.tokend.template.features.tfa.view.confirmation.TfaConfirmationDialogFactory
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.fingerprint.FingerprintUtil
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.dialog.SecretSeedDialog
import org.tokend.template.view.dialog.SignOutDialogFactory
import org.tokend.template.view.dialog.SingleCheckDialog
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.LocalizedName

class GeneralSettingsFragment : SettingsFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create()

    override fun getScreenKey(): String? = null

    private var fingerprintPreference: SwitchPreferenceCompat? = null

    private val tfaRepository: TfaFactorsRepository
        get() = repositoryProvider.tfaFactors()
    private var tfaPreference: SwitchPreferenceCompat? = null
    private val tfaFactor: TfaFactorRecord?
        get() = tfaRepository.itemsList.find { it.type == TFA_FACTOR_TYPE }
    private val isTfaEnabled: Boolean
        get() = tfaFactor?.let { it.priority > 0 } ?: false

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress?.show() },
            hideLoading = { progress?.hide() }
    )

    @SuppressLint("InlinedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Include toolbar and progress.
        if (view is LinearLayout) {
            val appbar = layoutInflater.inflate(R.layout.appbar, view, false)
            view.addView(appbar, 0)

            toolbar.title = getString(R.string.settings_title)
            toolbarSubject.onNext(toolbar)

            val progress = layoutInflater.inflate(R.layout.layout_progress, view, false)
            view.addView(progress, 1)
        }

        // Include toolbar elevation.
        try {
            val listContainer = view.findViewById<FrameLayout>(android.R.id.list_container)
            // Because of <merge> root
            val dummyContainer = FrameLayout(requireContext())
            layoutInflater.inflate(R.layout.include_appbar_elevation, dummyContainer, true)
            val elevationView = dummyContainer.appbar_elevation_view
            dummyContainer.removeAllViews()
            listContainer.addView(elevationView)
            ElevationUtil.initScrollElevation(listView, elevationView)
        } catch (e: Exception) {
            // Ok, no elevation, not a big problem...
        }

        // Disable list overscroll.
        listView.overScrollMode = ScrollView.OVER_SCROLL_NEVER
    }

    override fun reloadPreferences() {
        super.reloadPreferences()

        initAccountCategory()
        initSecurityCategory()
        initInfoCategory()
        initAppCategory()
    }

    // region Account
    private fun initAccountCategory() {
        initAccountIdItem()
        initKycItem()
        initSecretSeedItem()
        initLocalAccountMnemonicItem()
        initSignOutItem()
        hideCategoryIfEmpty("account")
    }

    private fun initAccountIdItem() {
        val accountIdPreference = findPreference("account_id")
        accountIdPreference?.setOnPreferenceClickListener {
            val walletInfo = walletInfoProvider.getWalletInfo()
                    ?: return@setOnPreferenceClickListener false

            activity?.let { parentActivity ->
                Navigator.from(parentActivity).openAccountQrShare(walletInfo)
            }

            true
        }
    }

    private fun initKycItem() {
        val kycPreference = findPreference("kyc")
        kycPreference?.setOnPreferenceClickListener {
            activity?.browse(urlConfigProvider.getConfig().kyc, true)
            true
        }
    }

    private fun initSecretSeedItem() {
        val seedPreference = findPreference("secret_seed") ?: return
        val account = accountProvider.getAccount() ?: return

        seedPreference.setOnPreferenceClickListener {
            SecretSeedDialog(
                    requireContext(),
                    account,
                    toastManager
            ).show()

            true
        }
    }

    private fun initLocalAccountMnemonicItem() {
        val mnemonicPreference = findPreference("mnemonic") ?: return

        val localAccount = repositoryProvider.localAccount().item
        val entropy =
                if (localAccount != null && localAccount.hasEntropy && localAccount.isDecrypted)
                    localAccount.entropy
                else
                    null

        if (!session.isLocalAccountUsed || entropy == null) {
            mnemonicPreference.isVisible = false
            return
        }

        mnemonicPreference.setOnPreferenceClickListener {
            MnemonicPhraseDialog(
                    requireContext(),
                    mnemonicCode.toMnemonic(entropy).joinToString(" "),
                    toastManager
            ).show()

            true
        }
    }

    private fun initSignOutItem() {
        val signOutPreference = findPreference("sign_out")
        signOutPreference?.setOnPreferenceClickListener {
            SignOutDialogFactory.getDialog(requireContext()) {
                (requireActivity().application as App).signOut(requireActivity())
            }.show()
            true
        }
    }
// endregion

    // region Security
    private fun initSecurityCategory() {
        if (session.isAuthenticatorUsed) {
            preferenceScreen.removePreference(findPreference("security"))
        } else {
            initBackgroundLockItem()
            initFingerprintItem()
            initTfaItem()
            initChangePasswordItem()
            hideCategoryIfEmpty("security")
        }
    }

    private fun initBackgroundLockItem() {
        val lockPreference = findPreference("background_lock")
                as? SwitchPreferenceCompat
                ?: return

        if (session.isLocalAccountUsed) {
            lockPreference.isVisible = false
            return
        }

        lockPreference.isVisible = backgroundLockManager.canBackgroundLockBeDisabled
        lockPreference.isChecked = backgroundLockManager.isBackgroundLockEnabled
        lockPreference.setOnPreferenceClickListener {
            backgroundLockManager.isBackgroundLockEnabled = lockPreference.isChecked
            true
        }
    }

    private fun initFingerprintItem() {
        fingerprintPreference = findPreference("fingerprint") as? SwitchPreferenceCompat
        fingerprintPreference?.isVisible = FingerprintUtil(requireContext()).isFingerprintAvailable
    }

    private fun initTfaItem() {
        tfaPreference = findPreference("tfa") as? SwitchPreferenceCompat

        if (session.isLocalAccountUsed) {
            tfaPreference?.isVisible = false
            return
        }

        tfaPreference?.setOnPreferenceClickListener {
            tfaPreference?.isChecked = isTfaEnabled
            switchTfa()
            false
        }

        tfaRepository.updateIfNotFresh()
        subscribeToTfaFactors()
    }

    private fun initChangePasswordItem() {
        val changePasswordPreference = findPreference("change_password")

        if (session.isLocalAccountUsed) {
            changePasswordPreference?.isVisible = false
            return
        }

        changePasswordPreference?.setOnPreferenceClickListener {
            activity?.let { parentActivity ->
                Navigator.from(parentActivity).openPasswordChange(3597)
            }

            true
        }
    }
// endregion

    // region TFA
    private fun subscribeToTfaFactors() {
        tfaRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    updateTfaPreference()
                }
                .addTo(compositeDisposable)

        tfaRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "tfa")
                    updateTfaPreference()
                }
                .addTo(compositeDisposable)
    }

    private fun updateTfaPreference() {
        tfaPreference?.isEnabled = !tfaRepository.isLoading && tfaRepository.isFresh
        tfaPreference?.isChecked = isTfaEnabled
    }

    private fun switchTfa() {
        if (isTfaEnabled) {
            disableTfa()
        } else {
            addAndEnableNewTfaFactor()
        }
    }

    private fun disableTfa() {
        DisableTfaUseCase(
                TFA_FACTOR_TYPE,
                tfaRepository
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .subscribeBy(
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun addAndEnableNewTfaFactor() {
        val confirmationDialogFactory =
                TfaConfirmationDialogFactory(requireContext(), toastManager)

        EnableTfaUseCase(
                TFA_FACTOR_TYPE,
                tfaRepository,
                confirmationDialogFactory
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .subscribeBy(
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }
// endregion

    // region Info
    private fun initInfoCategory() {
        initLimitsItem()
        initFeesItem()
        initTermsItem()
        initOpenSourceLicensesItem()
        hideCategoryIfEmpty("info")
    }

    private fun initLimitsItem() {
        val limitsPreference = findPreference("limits")
        limitsPreference?.setOnPreferenceClickListener {
            Navigator.from(this).openLimits()
            true
        }
        if (!BuildConfig.IS_LIMITS_ALLOWED) {
            (findPreference("info") as? PreferenceCategory)
                    ?.removePreference(limitsPreference)
        }
    }

    private fun initFeesItem() {
        val feesPreference = findPreference("fees")
        feesPreference?.setOnPreferenceClickListener {
            Navigator.from(this).openFees()
            true
        }
        if (!BuildConfig.IS_FEES_ALLOWED) {
            (findPreference("info") as? PreferenceCategory)
                    ?.removePreference(feesPreference)
        }
    }

    private fun initTermsItem() {
        val termsPreference = findPreference("terms")
        termsPreference?.setOnPreferenceClickListener {
            requireContext().browse(urlConfigProvider.getConfig().terms)
        }
    }

    private fun initOpenSourceLicensesItem() {
        val openSourceLicensesPreference = findPreference("open_source_licenses")
        openSourceLicensesPreference?.setOnPreferenceClickListener {
            OpenSourceLicensesDialog(requireContext(), R.style.AlertDialogStyle)
                    .show()

            true
        }
    }
// endregion

    // region App
    private fun initAppCategory() {
        initLanguageItem()
        hideCategoryIfEmpty("app")
    }

    private fun initLanguageItem() {
        val languagePreference = findPreference("language")
                ?: return

        val currentLocale = localeManager.getLocale()
        val availableLocales = localeManager.availableLocales
        val localizedName = LocalizedName(requireContext())

        languagePreference.summary = localizedName.forLocale(currentLocale)

        val dialog = SingleCheckDialog(
                requireContext(),
                availableLocales.map(localizedName::forLocale)
        )
        dialog.setDefaultCheckIndex(availableLocales.indexOf(currentLocale))
        dialog.setPositiveButtonListener { _, index ->
            availableLocales
                    .getOrNull(index)
                    ?.also(localeManager::setLocale)
        }
        dialog.setTitle(R.string.select_language)

        languagePreference.setOnPreferenceClickListener {
            dialog.show()
            true
        }

        if (availableLocales.size == 1) {
            languagePreference.isVisible = false
        }
    }
    // endregion

    companion object {
        private val TFA_FACTOR_TYPE = TfaFactor.Type.TOTP
        val ID = "general-settings".hashCode().toLong()

        fun newInstance() = GeneralSettingsFragment()
    }
}