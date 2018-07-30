package org.tokend.template.base.view.adapter.base

import android.support.v7.widget.RecyclerView
import android.view.View
import org.jetbrains.anko.onClick

abstract class BaseViewHolder<T>(protected val view: View): RecyclerView.ViewHolder(view) {
    abstract fun bind(item: T)

    open fun bind(item: T, clickListener: SimpleItemClickListener<T>?) {
        bind(item)
        view.onClick { clickListener?.invoke(view, item) }
    }
}