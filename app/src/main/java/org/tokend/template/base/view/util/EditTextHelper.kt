package org.tokend.template.base.view.util

import com.rengwuxian.materialedittext.MaterialEditText
import org.jetbrains.anko.textChangedListener
import org.tokend.template.App
import org.tokend.template.R
import ua.com.radiokot.pc.util.text_validators.EmailValidator
import ua.com.radiokot.pc.util.text_validators.PasswordValidator

object EditTextHelper {
    fun initEmailEditText(editText: MaterialEditText) {
        editText.textChangedListener {
            afterTextChanged { s ->
                if (s.isNullOrEmpty() || EmailValidator.isValid(s)) {
                    editText.error = null
                } else {
                    editText.error = editText.context.getString(R.string.error_invalid_email)
                }
            }
        }
    }

    fun initPasswordEditText(editText: MaterialEditText) {
        editText.textChangedListener {
            afterTextChanged { s ->
                if (s.isNullOrEmpty() || PasswordValidator.isValid(s)) {
                    editText.error = null
                } else {
                    editText.error = editText.context.getString(R.string.error_weak_password)
                }
            }
        }
    }
}