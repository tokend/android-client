package org.tokend.template.util

import io.reactivex.CompletableTransformer
import io.reactivex.ObservableTransformer
import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Holds observable transformers
 */
object ObservableTransformers {
    private val defaultSchedulers = ObservableTransformer<Any, Any> { upstream ->
        upstream.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Sets default schedulers for transformed Observable:
     * [Schedulers.newThread] for subscribe and [AndroidSchedulers.mainThread] for observe
     */
    @SuppressWarnings("unchecked")
    fun <T> defaultSchedulers(): ObservableTransformer<T, T> {
        return defaultSchedulers as ObservableTransformer<T, T>
    }

    /**
     * Sets default schedulers for transformed Completable:
     * [Schedulers.newThread] for subscribe and [AndroidSchedulers.mainThread] for observe
     */
    fun defaultSchedulersCompletable(): CompletableTransformer {
        return CompletableTransformer { upstream ->
            upstream.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
        }
    }

    /**
     * Sets default schedulers for transformed Single:
     * [Schedulers.newThread] for subscribe and [AndroidSchedulers.mainThread] for observe
     */
    fun <T> defaultSchedulersSingle(): SingleTransformer<T, T> {
        return SingleTransformer { upstream ->
            upstream.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
        }
    }
}