package org.tokend.template.view.util.input

import com.rengwuxian.materialedittext.MaterialEditText
import org.jetbrains.anko.textChangedListener
import org.tokend.template.R
import org.tokend.template.util.validator.EmailValidator
import org.tokend.template.util.validator.PasswordValidator

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