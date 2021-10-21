package org.tokend.template.extensions

import android.view.View
import android.view.ViewGroup

fun ViewGroup.childrenSequence(): Sequence<View> {
    return ArrayList<Int>(this.childCount).mapIndexed { index, _ ->
        getChildAt(index)
    }.asSequence()
}

inline fun ViewGroup.forEachChildWithIndex(receiver: (Int, View) -> Unit) {
    for (index in 0 until this.childCount) {
        receiver.invoke(index, getChildAt(index))
    }
}

inline fun ViewGroup.forEachChild(receiver: (View) -> Unit) {
    for (index in 0 until this.childCount) {
        receiver.invoke(getChildAt(index))
    }
}