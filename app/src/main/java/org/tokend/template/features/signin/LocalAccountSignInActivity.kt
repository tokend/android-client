package org.tokend.template.features.signin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_local_account_sign_in.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.localaccount.logic.CreateLocalAccountUseCase
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.features.localaccount.view.util.LocalAccountLogoUtil
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInWithLocalAccountUseCase
import org.tokend.template.features.userkey.pin.PinCodeActivity
import org.tokend.template.features.userkey.pin.SetUpPinCodeActivity
import org.tokend.template.features.userkey.view.ActivityUserKeyProvider
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import java.util.concurrent.TimeUnit

class LocalAccountSignInActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val localAccountRepository: LocalAccountRepository
        get() = repositoryProvider.localAccount()

    private val localAccount: LocalAccount?
        get() = localAccountRepository.item

    private var isLoading: Boolean = false
        set(value) {
            field = value
            if (value) {
                showLoading()
            } else {
                updateLocalAccountView()
            }
        }

    private val setUpPinCodeProvider = ActivityUserKeyProvider(
            SetUpPinCodeActivity::class.java,
            this,
            null
    )

    private val pinCodeProvider = ActivityUserKeyProvider(
            PinCodeActivity::class.java,
            this,
            null
    )

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_local_account_sign_in)

        initToolbar()
        initButtons()

        subscribeToLocalAccount()
        localAccountRepository.updateIfNotFresh()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.sign_in_with_local_account_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initButtons() {
        generate_local_account_button.setOnClickListener {
            generateLocalAccount()
        }

        sign_in_button.setOnClickListener {
            signIn()
        }

        local_account_details_button.setOnClickListener {
            Navigator.from(this).openLocalAccountDetails()
        }

        import_local_account_button.setOnClickListener {
            Navigator.from(this).openLocalAccountImport()
        }
    }

    private fun subscribeToLocalAccount() {
        val localAccountUpdatesSubject = PublishSubject.create<Boolean>()

        localAccountRepository
                .itemSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { localAccountUpdatesSubject.onNext(true) }
                .addTo(compositeDisposable)

        localAccountRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { isLoading ->
                    if (!isLoading) {
                        localAccountUpdatesSubject.onNext(true)
                    }
                }
                .addTo(compositeDisposable)

        localAccountUpdatesSubject
                .debounce(50, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { onLocalAccountUpdated() }
                .addTo(compositeDisposable)

        localAccountUpdatesSubject.onNext(true)
    }

    private fun onLocalAccountUpdated() {
        updateLocalAccountView()
    }

    private fun updateLocalAccountView() {
        val localAccount = this.localAccount

        if (isLoading) {
            return
        }

        if (localAccount == null || localAccount.isErased) {
            if (localAccountRepository.isNeverUpdated) {
                showLoading()
            } else {
                showNoAccount()
            }
        } else {
            showLocalAccount()
        }
    }

    private fun showNoAccount() {
        loading_layout.visibility = View.GONE
        no_local_account_layout.visibility = View.VISIBLE
        current_local_account_layout.visibility = View.GONE
    }

    private fun showLocalAccount() {
        loading_layout.visibility = View.GONE
        no_local_account_layout.visibility = View.GONE
        current_local_account_layout.visibility = View.VISIBLE
        displayLocalAccountInfo()
    }

    private fun showLoading() {
        loading_layout.visibility = View.VISIBLE
        no_local_account_layout.visibility = View.GONE
        current_local_account_layout.visibility = View.GONE
    }

    private fun displayLocalAccountInfo() {
        val localAccount = this.localAccount
                ?: return

        LocalAccountLogoUtil.setLogo(account_logo_image_view, localAccount)
        local_account_name_text_view.text = localAccount.accountId
    }

    private var generatingDisposable: Disposable? = null
    private fun generateLocalAccount() {
        generatingDisposable?.dispose()

        generatingDisposable = CreateLocalAccountUseCase(
                defaultDataCipher,
                setUpPinCodeProvider,
                localAccountRepository
        )
                .perform()
                .delay(GENERATION_VISUAL_DELAY_MS, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnEvent { _, _ ->
                    isLoading = false
                }
                .subscribeBy(
                        onError = { errorHandlerFactory.getDefault().handle(it) })
                .addTo(compositeDisposable)
    }

    private fun signIn() {
        SignInWithLocalAccountUseCase(
                localAccountRepository,
                defaultDataCipher,
                pinCodeProvider,
                session,
                credentialsPersistor,
                PostSignInManager(repositoryProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                }
                .subscribeBy(
                        onComplete = this::onSignInCompleted,
                        onError = {
                            isLoading = false
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
    }

    private fun onSignInCompleted() {
        Navigator.from(this).toMainActivity()
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pinCodeProvider.handleActivityResult(requestCode, resultCode, data)
        setUpPinCodeProvider.handleActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val GENERATION_VISUAL_DELAY_MS = 1000L
    }
}
