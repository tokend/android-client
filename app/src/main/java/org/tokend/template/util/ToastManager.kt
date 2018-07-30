package org.tokend.template.util

import android.support.annotation.StringRes
import android.widget.Toast
import org.tokend.template.App

object ToastManager {
    fun short(text: String?) {
        text?.let {
            Toast.makeText(App.context, it, Toast.LENGTH_SHORT).show()
        }
    }

    fun short(@StringRes text: Int) {
        Toast.makeText(App.context, text, Toast.LENGTH_SHORT).show()
    }

    fun long(text: String?) {
        text?.let {
            Toast.makeText(App.context, it, Toast.LENGTH_LONG).show()
        }
    }

    fun long(@StringRes text: Int) {
        Toast.makeText(App.context, text, Toast.LENGTH_LONG).show()
    }
}