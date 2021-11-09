package io.tokend.template.extensions

import android.view.View
import android.widget.TextView

var TextView.textOrGone: CharSequence?
    set(value) {
        text = value
        visibility =
            if (value == null)
                View.GONE
            else
                View.VISIBLE
    }
    get() = text