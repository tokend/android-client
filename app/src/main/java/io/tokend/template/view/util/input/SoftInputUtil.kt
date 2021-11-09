package io.tokend.template.view.util.input

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Contains utilities for soft keyboard interaction
 */
object SoftInputUtil {
    /**
     * Shows soft keyboard on given view
     *
     * @param view the currently focused view, which would like to receive
     * soft keyboard input
     */
    fun showSoftInputOnView(view: View?) {
        view ?: return

        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val wasAcceptingText = imm.isAcceptingText
        val hasChanged = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

        if (!wasAcceptingText && !hasChanged) {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        }
    }

    /**
     * Hides soft keyboard on currently focused view of the activity
     */
    fun hideSoftInput(activity: Activity) {
        hideSoftInput(activity.currentFocus)
    }

    /**
     * Hides soft keyboard on given view
     *
     * @param view currently focused view
     */
    fun hideSoftInput(view: View?) {
        view ?: return
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}