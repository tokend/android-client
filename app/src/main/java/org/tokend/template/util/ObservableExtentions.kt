package org.tokend.template.util

import android.text.InputFilter
import android.widget.EditText
import io.reactivex.Observable

fun EditText.inputChanges(inputFilter: InputFilter? = null): Observable<String> = TextWatcherObservable(this, inputFilter)