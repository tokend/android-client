package io.tokend.template.extensions

import android.view.View
import android.view.ViewGroup

inline val ViewGroup.children: List<View>
    get() = let { (0 until childCount).map(::getChildAt) }

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