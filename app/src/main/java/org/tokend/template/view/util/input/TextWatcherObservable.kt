package org.tokend.template.view.util.input

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.EditText
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

/**
 * Emits text change events of given [EditText]
 *
 * @param editText [EditText] to observe
 * @param inputFilter [InputFilter] to set on [editText]
 */
class TextWatcherObservable(private val editText: EditText,
                            private val inputFilter: InputFilter? = null) : Observable<String>() {

    override fun subscribeActual(observer: Observer<in String>) {
        val listener = Listener(editText, observer)
        observer.onSubscribe(listener)
        editText.addTextChangedListener(listener)
        inputFilter?.let {
            editText.filters = arrayOf(it)
        }
    }

    class Listener(private val editText: EditText, private val observer: Observer<in String>) : TextWatcher, MainThreadDisposable() {

        override fun afterTextChanged(p0: Editable?) {}

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {
            if (!isDisposed) observer.onNext(text.toString())
        }

        override fun onDispose() {
            editText.removeTextChangedListener(this)
        }
    }
}