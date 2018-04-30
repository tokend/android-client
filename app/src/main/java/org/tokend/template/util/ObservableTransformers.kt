package org.tokend.template.util

import io.reactivex.CompletableTransformer
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ObservableTransformers {
    private val defaultSchedulers = ObservableTransformer<Any, Any> { upstream ->
        upstream.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
    }

    @SuppressWarnings("unchecked")
    fun <T> defaultSchedulers(): ObservableTransformer<T, T> {
        return defaultSchedulers as ObservableTransformer<T, T>
    }

    fun defaultSchedulersCompletable(): CompletableTransformer {
        return CompletableTransformer { upstream ->
            upstream.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
        }
    }
}