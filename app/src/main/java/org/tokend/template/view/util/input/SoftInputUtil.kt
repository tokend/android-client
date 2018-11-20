package org.tokend.template.view.util.input

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object SoftInputUtil {
    fun showSoftInputOnView(view: View?) {
        view ?: return

        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideSoftInput(activity: Activity) {
        hideSoftInput(activity.currentFocus, activity)
    }

    fun hideSoftInput(view: View?, context: Context) {
        view ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}