package org.tokend.template.view

import android.content.Context
import androidx.annotation.StringRes
import android.widget.Toast

/**
 * Simplifies interaction with [ToastManager]
 */
class ToastManager(
        private val context: Context
) {
    /**
     * Shows a toast with [Toast.LENGTH_SHORT] length and given text
     */
    fun short(text: String?) {
        text?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a toast with [Toast.LENGTH_SHORT] length and given text
     */
    fun short(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows a toast with [Toast.LENGTH_LONG] length and given text
     */
    fun long(text: String?) {
        text?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shows a toast with [Toast.LENGTH_LONG] length and given text
     */
    fun long(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}