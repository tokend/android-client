package org.tokend.template.features.userkey.pin

import android.graphics.Color
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_pin_code.*
import org.tokend.crypto.ecdsa.erase
import org.tokend.template.R
import org.tokend.template.extensions.vibrator

class SetUpPinCodeActivity : PinCodeActivity() {
    private var isFirstEnter = true
    private var enteredPin = charArrayOf()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        super.onCreateAllowed(savedInstanceState)
        switchToFirstEnter()
    }

    override fun initToolbar() {
        super.initToolbar()
        setTitle(R.string.set_up_pin_code_title)
    }

    override fun onUserKeyEntered(key: CharArray) {
        if (isFirstEnter) {
            enteredPin = key
            switchToConfirmationEnter()
        } else {
            if (enteredPin.contentEquals(key)) {
                userKeyPersistence.save(enteredPin)
                super.onUserKeyEntered(key)
            } else {
                showConfirmationError()
                switchToFirstEnter()
                key.erase()
                enteredPin.erase()
            }
        }
    }

    private fun switchToFirstEnter() {
        isFirstEnter = true
        pin_code_edit_text.text?.clear()
        pin_code_label_text_view.text = getString(R.string.enter_new_pin_code)
    }

    private fun switchToConfirmationEnter() {
        isFirstEnter = false
        pin_code_edit_text.text?.clear()
        pin_code_label_text_view.text = getString(R.string.confirm_pin_code)
    }

    private fun showConfirmationError() {
        try {
            vibrator.vibrate(longArrayOf(0, 100), -1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Snackbar.make(
                pin_code_edit_text,
                R.string.error_pin_confirmation_mismatch,
                Snackbar.LENGTH_SHORT
        )
                .also {
                    it.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                            .setTextColor(Color.WHITE)
                }
                .show()
    }

    override fun requestFingerprintAuthIfAvailable() {
        hideBiometricsHint()
    }

    override fun onDestroy() {
        super.onDestroy()
        enteredPin.erase()
    }
}