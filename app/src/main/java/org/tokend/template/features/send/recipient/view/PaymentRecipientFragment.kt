package org.tokend.template.features.send.recipient.view

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_payment_recipient.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_progress.view.*
import org.jetbrains.anko.enabled
import org.tokend.template.R
import org.tokend.template.extensions.clipboardText
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.features.send.model.PaymentRecipient
import org.tokend.template.features.send.recipient.contacts.repository.ContactsRepository
import org.tokend.template.features.send.recipient.contacts.view.adapter.ContactsAdapter
import org.tokend.template.features.send.recipient.contacts.view.adapter.CredentialedContactListItem
import org.tokend.template.features.send.recipient.logic.PaymentRecipientLoader
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.util.validator.EmailValidator
import org.tokend.template.view.ContentLoadingProgressBar
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.wallet.Base32Check
import java.util.concurrent.TimeUnit

class PaymentRecipientFragment : BaseFragment() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { (main_progress as? ContentLoadingProgressBar)?.show() },
            hideLoading = { (main_progress as? ContentLoadingProgressBar)?.hide() }
    )

    private val contactsLoadingIndicator = LoadingIndicatorManager(
            showLoading = { contacts_layout.progress.show() },
            hideLoading = { contacts_layout.progress.hide() }
    )

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private val contactsPermission = PermissionManager(Manifest.permission.READ_CONTACTS, 606)

    private val contactsRepository: ContactsRepository
        get() = repositoryProvider.contacts()

    private val contactsAdapter = ContactsAdapter()

    private var canContinue: Boolean = false
        set(value) {
            field = value
            continue_button.enabled = value
        }

    private val resultSubject = PublishSubject.create<PaymentRecipient>()
    val resultObservable: Observable<PaymentRecipient> = resultSubject

    private var contactsFilter: String? = null
        set(value) {
            if (value != field) {
                val isTheSame = field == value
                field = value
                if (!isTheSame) {
                    onContactsFilterChanged()
                }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_payment_recipient, container, false)
    }

    override fun onInitAllowed() {
        initFields()
        initButtons()
        initContacts()

        subscribeToContacts()

        tryUpdateContacts()

        updateContinueAvailability()
    }

    // region Init
    private fun initFields() {
        recipient_edit_text.onFocusChangeListener = View.OnFocusChangeListener { _, _ ->
            showRecipientAutocompleteIfFocused()
        }

        recipient_edit_text.setOnClickListener {
            showRecipientAutocompleteIfFocused()
        }

        recipient_edit_text.addTextChangedListener(SimpleTextWatcher {
                recipient_edit_text.error = null
                updateContinueAvailability()
                updateRecipientAction()
        })
        recipient_edit_text.onEditorAction {
            tryToLoadRecipient()
        }
    }

    private fun initButtons() {
        recipient_action_button.setOnClickListener {
            onRecipientActionClick()
        }

        continue_button.setOnClickListener {
            tryToLoadRecipient()
        }
    }

    private fun initContacts() {
        contacts_list.apply {
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            adapter = contactsAdapter
            (itemAnimator as? SimpleItemAnimator)?.apply {
                requireContext()
                        .resources
                        .getInteger(android.R.integer.config_shortAnimTime)
                        .toLong()
                        .also { requiredDuration ->
                            addDuration = requiredDuration
                            removeDuration = requiredDuration
                        }
            }
        }

        ElevationUtil.initScrollElevation(contacts_list, appbar_elevation_view)

        contactsAdapter.onItemClick { _, item ->
            if (item is CredentialedContactListItem) {
                recipient_edit_text.setText(item.credential)
                recipient_edit_text.setSelection(item.credential.length)
                tryToLoadRecipient()
            }
        }

        RxTextView.textChanges(recipient_edit_text)
                .skipInitialValue()
                .debounce(FILTER_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { query ->
                    contactsFilter = query.trim().toString().takeIf(String::isNotBlank)
                }
                .addTo(compositeDisposable)
    }
    // endregion

    // region Contacts
    private fun subscribeToContacts() {
        contactsRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayContacts() }
                .addTo(compositeDisposable)

        contactsRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    contactsLoadingIndicator.setLoading(it)
                }
                .addTo(compositeDisposable)
    }

    private fun displayContacts() {
        val contacts = contactsRepository.itemsList
        contactsAdapter.setData(contacts, contactsFilter)
        contacts_empty_view.visibility =
                if (!contactsAdapter.hasData && !contactsRepository.isNeverUpdated)
                    View.VISIBLE
                else
                    View.GONE
    }

    private fun tryUpdateContacts() {
        contactsPermission.check(this, {
            contactsRepository.updateIfNotFresh()
        }, {
            contacts_empty_view.visibility = View.VISIBLE
        })
    }

    private fun onContactsFilterChanged() {
        displayContacts()
    }
    // endregion

    private fun updateRecipientAction() {
        val icon =
                if (recipient_edit_text.text.isNullOrBlank())
                    R.drawable.ic_qr_code_scan
                else
                    R.drawable.ic_close

        recipient_action_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), icon))
    }

    private fun onRecipientActionClick() {
        if (!recipient_edit_text.text.isNullOrBlank()) {
            recipient_edit_text.text.clear()
        } else {
            tryOpenQrScanner()
        }
    }

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
                    .addTo(activityRequestsBag)
                    .doOnSuccess {
                        recipient_edit_text.setText(it)
                        recipient_edit_text.setSelection(recipient_edit_text.text?.length ?: 0)
                        tryToLoadRecipient()
                    }
        }
    }

    private fun readRecipient(raw: CharSequence): String? {
        val filtered = raw
                .trim()
                .split(' ', '\r', '\n')
                .first()

        val validAccountId = Base32Check.isValid(Base32Check.VersionByte.ACCOUNT_ID,
                filtered.toCharArray())
        val validEmail = EmailValidator.isValid(filtered)

        return when {
            validAccountId -> Base32Check.encodeAccountId(Base32Check.decodeAccountId(filtered))
            validEmail -> filtered
            filtered.isEmpty() -> ""
            else -> null
        }
    }

    private fun readAndCheckRecipient(raw: String): String {
        val recipient = readRecipient(raw)

        when {
            recipient == null ->
                recipient_edit_text.error = getString(R.string.error_invalid_recipient)
            recipient.isEmpty() ->
                recipient_edit_text.error = getString(R.string.error_cannot_be_empty)
        }

        return recipient ?: ""
    }

    private fun updateContinueAvailability() {
        canContinue = !recipient_edit_text.hasError()
                && !recipient_edit_text.text.isNullOrBlank()
                && !loadingIndicator.isLoading
    }

    private var recipientLoadingDisposable: Disposable? = null
    private fun tryToLoadRecipient() {
        val recipient = readAndCheckRecipient(recipient_edit_text.text.toString())
        updateContinueAvailability()

        if (!canContinue) {
            return
        }

        recipientLoadingDisposable?.dispose()
        recipientLoadingDisposable =
                PaymentRecipientLoader(
                        repositoryProvider.accountDetails()
                )
                        .load(recipient)
                        .compose(ObservableTransformers.defaultSchedulersSingle())
                        .doOnSubscribe {
                            loadingIndicator.show("recipient")
                            updateContinueAvailability()
                        }
                        .doOnEvent { _, _ ->
                            loadingIndicator.hide("recipient")
                            updateContinueAvailability()
                        }
                        .subscribeBy(
                                onSuccess = this::onRecipientLoaded,
                                onError = this::onRecipientLoadingError
                        )
                        .addTo(compositeDisposable)
    }

    private fun onRecipientLoaded(recipient: PaymentRecipient) {
        val currentAccountId = walletInfoProvider.getWalletInfo()?.accountId

        if (currentAccountId == recipient.accountId) {
            recipient_edit_text.error = getString(R.string.error_cannot_send_to_yourself)
            updateContinueAvailability()
        } else {
            resultSubject.onNext(recipient)
        }
    }

    private fun onRecipientLoadingError(e: Throwable) {
        when (e) {
            is PaymentRecipientLoader.NoRecipientFoundException ->
                recipient_edit_text.error = getString(R.string.error_invalid_recipient)
            else ->
                errorHandlerFactory.getDefault().handle(e)
        }
        updateContinueAvailability()
    }

    override fun onResume() {
        super.onResume()
        checkClipboardForRecipient()
    }

    // region Clipboard suggestion
    private var recipientSuggestion: String? = null
        set(value) {
            val sameAsBefore = field == value
            field = value
            if (value != null) {
                if (!sameAsBefore) {
                    val adapter = ArrayAdapter(requireContext(),
                            android.R.layout.simple_list_item_1, arrayOf(value))
                    recipient_edit_text.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                    showRecipientAutocompleteIfFocused()
                }
            } else {
                recipient_edit_text.setAdapter(null)
            }
        }

    private fun checkClipboardForRecipient() {
        val clipboardText = requireContext().clipboardText
                ?: return
        recipientSuggestion = readRecipient(clipboardText)?.takeIf(String::isNotEmpty)
    }

    private fun showRecipientAutocompleteIfFocused() {
        recipient_edit_text.apply {
            if (hasFocus() && !isPopupShowing) {
                showDropDown()
            }
        }
    }
    // endregion

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
        contactsPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    private companion object {
        private const val FILTER_DEBOUNCE_MS = 400L
    }
}