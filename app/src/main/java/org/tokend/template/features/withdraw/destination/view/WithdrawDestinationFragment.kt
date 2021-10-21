package org.tokend.template.features.withdraw.destination.view

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_withdraw_destination.*
import org.tokend.template.R
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.withArguments
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.view.util.input.SimpleTextWatcher

class WithdrawDestinationFragment : BaseFragment() {

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)

    private var canContinue: Boolean = false
        set(value) {
            field = value
            continue_button.isEnabled = value
        }

    private val resultSubject = PublishSubject.create<String>()
    val resultObservable: Observable<String> = resultSubject

    private val amountToSend: String?
        get() = arguments?.getString(AMOUNT_EXTRA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_withdraw_destination, container, false)
    }

    override fun onInitAllowed() {
        initFields()
        initButtons()

        updateContinueAvailability()
    }

    private fun initFields() {
        destination_edit_text.addTextChangedListener(SimpleTextWatcher {
            destination_edit_text.error = null
            updateContinueAvailability()
        })
        destination_edit_text.onEditorAction {
            tryToContinue()
        }
        destination_edit_text.requestFocus()

        amountToSend?.let {
            title_text_view.text = getString(R.string.template_to_withdraw, it)
        }
    }

    private fun initButtons() {
        scan_qr_button.setOnClickListener {
            tryOpenQrScanner()
        }

        continue_button.setOnClickListener {
            tryToContinue()
        }
    }

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
                    .addTo(activityRequestsBag)
                    .doOnSuccess {
                        destination_edit_text.setText(it)
                        destination_edit_text.setSelection(destination_edit_text.text?.length ?: 0)
                        checkDestination()
                        updateContinueAvailability()
                    }
        }
    }

    private fun checkDestination() {
        val destination = destination_edit_text.text.toString().trim()

        destination_edit_text.error =
                if (destination.isEmpty())
                    getString(R.string.error_cannot_be_empty)
                else
                    null
    }

    private fun tryToContinue() {
        checkDestination()
        updateContinueAvailability()

        if (canContinue) {
            val destination = destination_edit_text.text.toString()
            resultSubject.onNext(destination)
        }
    }

    private fun updateContinueAvailability() {
        canContinue = !destination_edit_text.hasError()
                && !destination_edit_text.text.isNullOrBlank()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val AMOUNT_EXTRA = "amount_to_withdraw"

        fun getBundle(amountToWithdraw: String) = Bundle().apply {
            putString(AMOUNT_EXTRA, amountToWithdraw)
        }

        fun newInstance(bundle: Bundle): WithdrawDestinationFragment =
                WithdrawDestinationFragment().withArguments(bundle)
    }
}