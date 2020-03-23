package org.tokend.template.features.localaccount.importt.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_import_local_account.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.hasError
import org.tokend.template.features.localaccount.importt.logic.ImportLocalAccountFromMnemonicUseCase
import org.tokend.template.features.localaccount.importt.logic.ImportLocalAccountFromSecretSeedUseCase
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicException
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.userkey.pin.SetUpPinCodeActivity
import org.tokend.template.features.userkey.view.ActivityUserKeyProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.errorhandler.CompositeErrorHandler
import org.tokend.template.util.errorhandler.ErrorHandler
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.EditTextErrorHandler
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil
import org.tokend.wallet.Base32Check
import java.util.concurrent.TimeUnit

class ImportLocalAccountActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private var canImport: Boolean = false
        set(value) {
            field = value
            import_local_account_button.isEnabled = value
        }

    private val setUpPinCodeProvider = ActivityUserKeyProvider(
            SetUpPinCodeActivity::class.java,
            this,
            null
    )

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_import_local_account)

        initToolbar()
        initButtons()
        initFields()

        canImport = false
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.import_local_account_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initButtons() {
        import_local_account_button.setOnClickListener {
            importAccount()
        }
    }

    private fun initFields() {
        SoftInputUtil.showSoftInputOnView(import_data_edit_text)
        import_data_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                import_data_edit_text.error = null
                updateImportAvailability()
            }
        })
    }

    private fun updateImportAvailability() {
        canImport = !import_data_edit_text.text.isNullOrBlank()
                && !import_data_edit_text.hasError()
                && !loadingIndicator.isLoading
    }

    private fun importAccount() {
        val importData = import_data_edit_text
                .text
                ?.trim()
                ?.toString()
                ?: return

        val importDataChars = importData.toCharArray()
        val dataIsSeed = Base32Check.isValid(Base32Check.VersionByte.SEED, importDataChars)

        val importUseCase =
                if (dataIsSeed) {
                    ImportLocalAccountFromSecretSeedUseCase(
                            importDataChars,
                            defaultDataCipher,
                            setUpPinCodeProvider,
                            repositoryProvider.localAccount()
                    )
                } else {
                    ImportLocalAccountFromMnemonicUseCase(
                            importData,
                            mnemonicCode,
                            defaultDataCipher,
                            setUpPinCodeProvider,
                            repositoryProvider.localAccount()
                    )
                }

        importUseCase
                .perform()
                .delay(IMPORT_VISUAL_DELAY_MS, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    loadingIndicator.show()
                    updateImportAvailability()
                }
                .doOnEvent { _, _ ->
                    loadingIndicator.hide()
                    updateImportAvailability()
                }
                .subscribeBy(
                        onSuccess = this::onSuccessfulImport,
                        onError = importErrorHandler::handleIfPossible
                )
                .addTo(compositeDisposable)
    }

    private val importErrorHandler: ErrorHandler
        get() = CompositeErrorHandler(
                EditTextErrorHandler(import_data_edit_text) { error ->
                    when (error) {
                        is MnemonicException ->
                            getString(R.string.error_invalid_mnemonic_phrase)
                        else ->
                            null
                    }
                },
                errorHandlerFactory.getDefault()
        )
                .doOnSuccessfulHandle(this::updateImportAvailability)

    private fun onSuccessfulImport(localAccount: LocalAccount) {
        setResult(Activity.RESULT_OK)
        toastManager.short(R.string.local_account_imported)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setUpPinCodeProvider.handleActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val IMPORT_VISUAL_DELAY_MS = 1000L
    }
}
