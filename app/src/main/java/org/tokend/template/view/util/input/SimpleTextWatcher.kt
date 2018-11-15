package org.tokend.template.view.util.input

import android.text.TextWatcher

/**
 * Simple abstract TextWatcher which allows to implement only [afterTextChanged].
 */
abstract class SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}