package org.tokend.template.features.settings

import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.SwitchPreferenceCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.isSelectable
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.R
import org.tokend.template.data.repository.tfa.TfaFactorsRepository
import org.tokend.template.features.settings.view.OpenSourceLicensesDialog
import org.tokend.template.features.tfa.logic.DisableTfaUseCase
import org.tokend.template.features.tfa.logic.EnableTfaUseCase
import org.tokend.template.features.tfa.model.TfaFactorCreationResult
import org.tokend.template.features.tfa.model.TfaFactorRecord
import org.tokend.template.features.tfa.view.TotpFactorConfirmationDialog
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.persistance.FingerprintUtil
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.LoadingIndicatorManager
import java.nio.CharBuffer

class GeneralSettingsFragment : SettingsFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    override fun getScreenKey(): String? = null

    private val TFA_FACTOR_TYPE = TfaFactor.Type.TOTP

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
    }

    override fun reloadPreferences() {
        super.reloadPreferences()

        initAccountCategory()
        initSecurityCategory()
        initInfoCategory()
    }

    // region Account
    private fun initAccountCategory() {
        initAccountIdItem()
        initKycItem()
        initSecretSeedItem()
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
                        shareLabel = getString(R.string.share_account_id)
                )
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

        seedPreference.setOnPreferenceClickListener {
            val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                    .setMessage(
                            CharBuffer.wrap(accountProvider.getAccount()?.secretSeed
                                    ?: getString(R.string.error_try_again).toCharArray())
                    )
                    .setTitle(R.string.secret_seed)
                    .setPositiveButton(R.string.ok, null)
                    .show()

            dialog.findViewById<TextView>(android.R.id.message)?.let { textView ->
                textView.isSelectable = true
                textView.typeface = Typeface.MONOSPACE
            }

            true
        }
    }
// endregion

    // region Security
    private fun initSecurityCategory() {
        if (session.isAuthenticatorUsed) {
            preferenceScreen.removePreference(findPreference("security"))
        } else {
            initFingerprintItem()
            initTfaItem()
            initChangePasswordItem()
        }
    }

    private fun initFingerprintItem() {
        fingerprintPreference = findPreference("fingerprint") as? SwitchPreferenceCompat
        fingerprintPreference?.isVisible = FingerprintUtil(requireContext()).isFingerprintAvailable
    }

    private fun initTfaItem() {
        tfaPreference = findPreference("tfa") as? SwitchPreferenceCompat
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
        changePasswordPreference?.setOnPreferenceClickListener {
            activity?.let { parentActivity ->
                Navigator.openPasswordChange(parentActivity, 3597)
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
        tfaPreference?.isEnabled = !tfaRepository.isLoading
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
        val totpConfirmationDialog = TotpFactorConfirmationDialog(
                requireContext(),
                toastManager,
                R.style.AlertDialogStyle
        )

        val confirmation = { creationResult: TfaFactorCreationResult ->
            when (creationResult.newFactor.type) {
                TfaFactor.Type.TOTP ->
                    totpConfirmationDialog.show(creationResult.confirmationAttributes)
                else -> Single.just(true)
            }
        }

        EnableTfaUseCase(
                TFA_FACTOR_TYPE,
                tfaRepository,
                confirmation
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
        initTermsItem()
        initOpenSourceLicensesItem()
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
}