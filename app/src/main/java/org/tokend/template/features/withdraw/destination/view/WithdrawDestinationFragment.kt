package org.tokend.template.features.withdraw.destination.view

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_withdraw_destination.*
import org.jetbrains.anko.enabled
import org.tokend.template.R
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.view.util.input.SimpleTextWatcher

class WithdrawDestinationFragment : BaseFragment() {

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)

    private var canContinue: Boolean = false
        set(value) {
            field = value
            continue_button.enabled = value
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
        destination_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                destination_edit_text.error = null
                updateContinueAvailability()
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        QrScannerUtil.getStringFromResult(requestCode, resultCode, data)?.also {
            destination_edit_text.setText(it)
            destination_edit_text.setSelection(destination_edit_text.text?.length ?: 0)
            checkDestination()
            updateContinueAvailability()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val AMOUNT_EXTRA = "amount to withdraw"

        fun newInstance(amountToWithdraw: String): WithdrawDestinationFragment {
            val fragment = WithdrawDestinationFragment()
            fragment.arguments = Bundle().apply {
                putString(AMOUNT_EXTRA, amountToWithdraw)
            }
            return fragment
        }
    }
}