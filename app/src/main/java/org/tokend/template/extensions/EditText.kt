package org.tokend.template.extensions

import android.support.annotation.StringRes
import android.widget.EditText
import org.tokend.template.util.SoftInputUtil

fun EditText.hasError(): Boolean {
    return error != null
}

fun EditText.setErrorAndFocus(@StringRes errorId: Int) {
    setErrorAndFocus(context.getString(errorId))
}

fun EditText.setErrorAndFocus(error: String) {
    this.error = error
    setSelection(text.length)
    requestFocus()
    SoftInputUtil.showSoftInputOnView(this)
}

fun EditText.onEditorAction(callback: () -> Unit) {
    this.setOnEditorActionListener { _, _, _ ->
        callback()
        true
    }
}