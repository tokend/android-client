package org.tokend.template.util

import android.content.Context
import android.support.annotation.StringRes
import android.widget.Toast

class ToastManager(
        private val context: Context
) {
    fun short(text: String?) {
        text?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    fun short(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun long(text: String?) {
        text?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    fun long(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}