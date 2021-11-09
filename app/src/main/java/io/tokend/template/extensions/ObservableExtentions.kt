package io.tokend.template.extensions

import android.text.InputFilter
import android.widget.EditText
import io.reactivex.Observable
import io.tokend.template.view.util.input.TextWatcherObservable

fun EditText.inputChanges(inputFilter: InputFilter? = null): Observable<String> =
    TextWatcherObservable(this, inputFilter)