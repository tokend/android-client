package org.tokend.template.view.util.input

import android.text.Editable
import android.text.TextWatcher

/**
 * Simple TextWatcher which allows to implement only [afterTextChanged].
 */
open class SimpleTextWatcher(
        private val afterTextChanged: (editable: Editable?) -> Unit,
) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {
        afterTextChanged.invoke(s)
    }
}