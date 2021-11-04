package org.tokend.template.features.kyc.view

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
import kotlinx.android.synthetic.main.activity_set_kyc.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.dip
import org.tokend.template.features.kyc.capture.model.CameraCaptureResult
import org.tokend.template.features.kyc.capture.model.CameraCaptureTarget
import org.tokend.template.features.kyc.files.model.LocalFile
import org.tokend.template.features.kyc.files.util.WaitForFilePickForegroundService
import org.tokend.template.features.kyc.logic.UpdateCurrentKycRequestUseCase
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.model.KycForm
import org.tokend.template.features.kyc.model.KycFormWithName
import org.tokend.template.features.kyc.model.KycRequestState
import org.tokend.template.logic.TxManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ProfileUtil
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.view.dialog.CenteredButtonsDialog
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.FilePickerUtil
import org.tokend.template.view.util.ImageViewUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil

class SetKycActivity : BaseActivity() {

    private val loadingIndicator = LoadingIndicatorManager(
        showLoading = { progress.show() },
        hideLoading = { progress.hide() }
    )

    private val avatar = MutableLiveData<AvatarFile>().also {
        it.value = null
    }

    private val kycRequestStateLiveData = MutableLiveData<KycRequestState>().also {
        it.value = null
    }

    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value)
            updateChangeAvailability()
        }

    private var canConfirm = false

    private val activeKyc
        get() = repositoryProvider.activeKyc()

    private val kycRequestState
        get() = repositoryProvider.kycRequestState()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_set_kyc)

        initToolbar()
        initTextFields()
        initFilePicker()
        initButtons()
        initSwipeRefresh()
        subscribeToKycRequestState()
        subscribeToKyc()

        kycRequestState.updateIfNotFresh()

        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.title_kyc)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
                    session.getWalletInfo()!!.email,
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

    private fun subscribeToKyc() {
        activeKyc.itemSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                avatar.value = ProfileUtil.getAvatarUrl(
                    it,
                    urlConfigProvider
                )?.let { url -> AvatarFile.Remote(url) }

                (activeKyc.itemFormData as? KycFormWithName)?.let { formWithName ->
                    current_firstname_edit_text.setText(formWithName.firstName)
                    current_lastname_edit_text.setText(formWithName.lastName)
                }
            }.addTo(compositeDisposable)
    }

    private fun initButtons() {
        confirm_kyc_button.setOnClickListener {
            submitForm()
        }
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
        swipe_refresh.setOnRefreshListener {
            kycRequestState.update()
        }
    }

    private fun subscribeToKycRequestState() {
        kycRequestStateLiveData.observe(this) {
            when {
                it != null && it is KycRequestState.Submitted.Pending<*> -> {
                    kycRequestStateInfoLayout.visibility = View.VISIBLE
                    kycRejectReasonTextView.visibility = View.GONE
                    kycStatusTextView.text = getString(R.string.kyc_recovery_pending_message)
                    kycStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.orange))
                }
                it != null && it is KycRequestState.Submitted.Rejected<*> -> {
                    kycRequestStateInfoLayout.visibility = View.VISIBLE
                    kycRejectReasonTextView.visibility = View.VISIBLE
                    kycStatusTextView.text = getString(R.string.kyc_recovery_rejected_message)
                    kycStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error))
                    kycRejectReasonTextView.text =
                        getString(R.string.template_rejection_reason, it.rejectReason)
                }
                else -> {
                    kycRequestStateInfoLayout.visibility = View.GONE
                    kycRejectReasonTextView.visibility = View.GONE
                }
            }
        }

        kycRequestState
            .itemSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                kycRequestStateLiveData.value = it
            }.addTo(compositeDisposable)

        kycRequestState
            .loadingSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                swipe_refresh.isRefreshing = it
            }.addTo(compositeDisposable)
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
        FilePickerUtil.pickFile(this, ALLOWED_MIME_TYPES, false)
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
            activeKyc.itemFormData?.documents
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
                onComplete = {
                    onFormSubmitted(form)
                },
                onError = { errorHandlerFactory.getDefault().handle(it) }
            )
            .addTo(compositeDisposable)
    }

    private fun onFormSubmitted(form: KycForm) {
        if (kycRequestState.item is KycRequestState.Submitted.Approved<*>) {
            toastManager.long(R.string.personal_info_updated)
            repositoryProvider
                .activeKyc()
                .set(ActiveKyc.Form(form))
        } else {
            toastManager.long(R.string.personal_info_update_request_created)
        }
        finish()
    }

    private fun initTextFields() {
        confirm_kyc_button.isEnabled = canConfirm
        current_firstname_edit_text.addTextChangedListener(SimpleTextWatcher() {
            checkField(current_firstname_edit_text)
            updateChangeAvailability()
        })
        current_lastname_edit_text.addTextChangedListener(SimpleTextWatcher() {
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

    companion object {
        private val ALLOWED_MIME_TYPES = arrayOf("image/jpg", "image/jpeg", "image/png")
    }

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