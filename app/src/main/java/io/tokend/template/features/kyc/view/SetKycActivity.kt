package io.tokend.template.features.kyc.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import com.rengwuxian.materialedittext.MaterialEditText
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.extensions.dip
import io.tokend.template.extensions.textOrGone
import io.tokend.template.features.kyc.capture.model.CameraCaptureResult
import io.tokend.template.features.kyc.capture.model.CameraCaptureTarget
import io.tokend.template.features.kyc.files.model.LocalFile
import io.tokend.template.features.kyc.files.util.WaitForFilePickForegroundService
import io.tokend.template.features.kyc.logic.UpdateCurrentKycRequestUseCase
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.kyc.model.KycFormWithName
import io.tokend.template.features.kyc.model.KycRequestState
import io.tokend.template.logic.TxManager
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.ProfileUtil
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.dialog.CenteredButtonsDialog
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.FilePickerUtil
import io.tokend.template.view.util.ImageViewUtil
import io.tokend.template.view.util.LoadingIndicatorManager
import io.tokend.template.view.util.input.SimpleTextWatcher
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.activity_set_kyc.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar.*

class SetKycActivity : BaseActivity() {

    private val loadingIndicator = LoadingIndicatorManager(
        showLoading = { progress.show() },
        hideLoading = { progress.hide() }
    )

    private val avatar = MutableLiveData<AvatarFile>().also {
        it.value = null
    }

    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value)
            updateChangeAvailability()
        }

    private var canConfirm = false

    private val kycRequestStateRepository
        get() = repositoryProvider.kycRequestState

    private val activeKyc
        get() = repositoryProvider.activeKyc

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_set_kyc)

        initToolbar()
        initTextFields()
        initFilePicker()
        initButtons()
        initSwipeRefresh()
        initEmptyView()

        subscribeToKycRequestState()

        update()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.title_kyc)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)
    }

    private fun initFilePicker() {
        ViewCompat.setTranslationZ(selectImageButton, 5F)
        ViewCompat.setTranslationZ(removeImageButton, 5F)

        removeImageButton.setOnClickListener {
            avatar.value = null
        }

        selectImageButton.setOnClickListener {
            SoftInputUtil.hideSoftInput(this)
            pickAvatar()
        }

        avatar.observe(this) {
            ImageViewUtil.loadImageCircle(
                avatarImageView,
                it?.uri,
                ProfileUtil.getAvatarPlaceholder(
                    session.login,
                    this,
                    dip(150)
                )
            )
            selectImageButton.setImageResource(
                if (it == null) {
                    removeImageButton.visibility = View.GONE
                    R.drawable.ic_plus_white
                } else {
                    removeImageButton.visibility = View.VISIBLE
                    R.drawable.ic_baseline_edit
                }
            )
        }
    }

    private fun initButtons() {
        confirm_kyc_button.setOnClickListener {
            submitForm()
        }
    }

    private fun initEmptyView() {
        error_empty_view.showEmpty(getString(R.string.loading_data))
    }

    private fun pickAvatar() {
        CenteredButtonsDialog(
            context = this,
            title = null,
            message = null,
            buttons = listOf(
                CenteredButtonsDialog.Button(
                    text = getString(R.string.take_a_selfie),
                    tag = 0
                ),
                CenteredButtonsDialog.Button(
                    text = getString(R.string.pick_from_gallery),
                    tag = 1
                ),
                CenteredButtonsDialog.Button(
                    text = getString(R.string.cancel),
                    colorRes = R.color.secondary_text
                )
            )
        )
            .show { button ->
                when (button.tag) {
                    0 -> captureAvatarFromCamera()
                    1 -> pickAvatarFromGallery()
                }
            }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun update(force: Boolean = false) {
        if (force) {
            kycRequestStateRepository.update()
        } else {
            kycRequestStateRepository.updateIfNotFresh()
        }
    }

    private fun subscribeToKycRequestState() {
        kycRequestStateRepository
            .itemSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                error_empty_view.hide()

                initKycRequestView(it)
                (it as? KycRequestState.Submitted<*>?).let { kycRequest ->
                    initKycView(kycRequest?.formData)
                    updateKyc(kycRequest)
                }
            }.addTo(compositeDisposable)

        kycRequestStateRepository
            .loadingSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                swipe_refresh.isRefreshing = it
            }.addTo(compositeDisposable)

        kycRequestStateRepository
            .errorsSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (kycRequestStateRepository.isNeverUpdated) {
                    error_empty_view.showError(it, errorHandlerFactory.getDefault()) {
                        update(force = true)
                    }
                } else {
                    errorHandlerFactory.getDefault().handle(it)
                }
            }.addTo(compositeDisposable)
    }

    private fun initKycRequestView(it: KycRequestState?) {
        when {
            it != null && it is KycRequestState.Submitted.Pending<*> -> {
                kycRequestStateInfoLayout.visibility = View.VISIBLE
                kycRejectReasonTextView.visibility = View.GONE
                kycStatusTextView.text = getString(R.string.kyc_recovery_pending_message)
                kycStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
            it != null && it is KycRequestState.Submitted.Rejected<*> -> {
                showRejectView(it.rejectReason)
                kycStatusTextView.text = getString(R.string.kyc_recovery_rejected_message)
            }
            it != null && it is KycRequestState.Submitted.PermanentlyRejected<*> -> {
                showRejectView(it.rejectReason)
                kycStatusTextView.text =
                    getString(R.string.kyc_recovery_permanently_rejected_message)
            }
            it != null && it is KycRequestState.Submitted.ApprovedToBlock -> {
                error_empty_view.showError(
                    error =
                    if (it.blockReason != null)
                        getString(
                            R.string.template_error_account_blocked_reason,
                            it.blockReason
                        )
                    else
                        getString(R.string.error_account_blocked),
                    buttonAction = null
                )
            }
            else -> {
                kycRequestStateInfoLayout.visibility = View.GONE
                kycRejectReasonTextView.visibility = View.GONE
            }
        }
    }

    private fun showRejectView(rejectReason: String?) {
        kycRequestStateInfoLayout.visibility = View.VISIBLE
        kycRejectReasonTextView.visibility = View.VISIBLE
        kycStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error))
        kycRejectReasonTextView.textOrGone =
            getString(R.string.template_rejection_reason, rejectReason)
    }

    private fun initKycView(kycForm: KycForm?) {
        avatar.value = ProfileUtil.getAvatarUrl(
            kycForm,
            urlConfigProvider
        )?.let { url -> AvatarFile.Remote(url) }

        (kycForm as? KycFormWithName)?.let { formWithName ->
            current_firstname_edit_text.setText(formWithName.firstName)
            current_lastname_edit_text.setText(formWithName.lastName)
        }
    }

    private fun updateKyc(kycRequestState: KycRequestState.Submitted<*>?) {
        val kycForm = kycRequestState?.formData
        if (kycForm != null && kycForm != activeKyc.itemFormData && kycRequestState is KycRequestState.Submitted.Approved<*>) {
            activeKyc.set(ActiveKyc.Form(kycForm))
        }
    }

    private fun captureAvatarFromCamera() {
        Navigator.from(this)
            .openCameraCapture(
                target = CameraCaptureTarget.SELFIE,
                canSkip = false
            )
            .doOnSuccess {
                if (it is CameraCaptureResult.Success) {
                    avatar.value = AvatarFile.Local(LocalFile.fromFile(it.imageFile))
                }
            }
            .addTo(activityRequestsBag)
    }

    private fun pickAvatarFromGallery() {
        startWaitingService()
        FilePickerUtil.pickFile(this, getAllowedMimeTypes(), false)
            .doOnSuccess {
                avatar.value = AvatarFile.Local(it)
            }
            .addTo(activityRequestsBag)
    }

    private fun submitForm() {
        val firstNameText = current_firstname_edit_text.text.toString().trim()
        val lastNameText = current_lastname_edit_text.text.toString().trim()

        val form = KycForm.General(
            firstName = firstNameText,
            lastName = lastNameText,
        )

        val newDocuments = (avatar.value as? AvatarFile.Local?)?.let {
            mapOf(
                KycForm.General.AVATAR_DOCUMENT_KEY to it.localFile
            )
        }

        val submittedDocuments = (avatar.value as? AvatarFile.Remote?)?.let {
            (kycRequestStateRepository.item as? KycRequestState.Submitted<*>)?.formData?.documents
        }

        SoftInputUtil.hideSoftInput(this)

        UpdateCurrentKycRequestUseCase(
            form,
            walletInfoProvider,
            accountProvider,
            repositoryProvider,
            apiProvider,
            TxManager(apiProvider),
            alreadySubmittedDocuments = submittedDocuments,
            newDocuments = newDocuments,
            contentResolver = contentResolver
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe {
                isLoading = true
            }
            .doOnTerminate {
                isLoading = false
            }
            .subscribeBy(
                onComplete = this::onFormSubmitted,
                onError = { errorHandlerFactory.getDefault().handle(it) }
            )
            .addTo(compositeDisposable)
    }

    private fun onFormSubmitted() {
        if (kycRequestStateRepository.item is KycRequestState.Submitted.Approved<*>) {
            toastManager.long(R.string.personal_info_updated)
        } else {
            toastManager.long(R.string.personal_info_update_request_created)
        }
        finish()
    }

    private fun initTextFields() {
        confirm_kyc_button.isEnabled = canConfirm
        current_firstname_edit_text.addTextChangedListener(SimpleTextWatcher {
            checkField(current_firstname_edit_text)
            updateChangeAvailability()
        })
        current_lastname_edit_text.addTextChangedListener(SimpleTextWatcher {
            checkField(current_lastname_edit_text)
            updateChangeAvailability()
        })
    }

    private fun checkField(editText: MaterialEditText) {
        if (editText.text.isNullOrEmpty()) {
            editText.error = getString(R.string.error_cannot_be_empty)
        } else {
            editText.error = null
        }
    }

    private fun updateChangeAvailability() {
        canConfirm = !isLoading
                && !current_firstname_edit_text.text.isNullOrEmpty()
                && !current_lastname_edit_text.text.isNullOrEmpty()
        confirm_kyc_button.isEnabled = canConfirm
        current_lastname_edit_text.isEnabled = !isLoading
        current_firstname_edit_text.isEnabled = !isLoading
        selectImageButton.isEnabled = !isLoading
        removeImageButton.isEnabled = !isLoading
    }

    private fun startWaitingService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, WaitForFilePickForegroundService::class.java)
        )
    }

    private fun stopWaitingService() {
        stopService(Intent(this, WaitForFilePickForegroundService::class.java))
    }

    private fun getAllowedMimeTypes() = arrayOf("image/jpg", "image/jpeg", "image/png")

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stopWaitingService()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWaitingService()
        FilePickerUtil.removeTemporaryFile(this)
    }

    private sealed class AvatarFile(open val uri: String) {
        class Remote(uri: String) : AvatarFile(uri)

        class Local(val localFile: LocalFile) : AvatarFile(localFile.uri.toString())
    }
}