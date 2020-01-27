package org.tokend.template.data.repository.base

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Contains common repository logic. Is a parent of all repositories.
 */
abstract class Repository {
    /**
     * Emits repository errors.
     */
    val errorsSubject: PublishSubject<Throwable> =
            PublishSubject.create<Throwable>()

    protected open val mLoadingSubject: BehaviorSubject<Boolean> =
            BehaviorSubject.createDefault(false)

    /**
     * Emits repository loading states.
     * @see Repository.isLoading
     */
    val loadingSubject: Observable<Boolean> by lazy {
        var postDebounceValue: Boolean? = null
        mLoadingSubject
                .debounce(20, TimeUnit.MILLISECONDS)
                .filter { it != postDebounceValue }
                .doOnNext { postDebounceValue = it }
    }

    /**
     * Indicates whether repository is loading something now.
     */
    var isLoading: Boolean = false
        protected set(value) {
            if (field != value) {
                field = value
                mLoadingSubject.onNext(value)
            }
            field = value
        }

    /**
     * Indicates whether data is in actual state.
     */
    var isFresh = false
        protected set
    /**
     * Indicates whether repository has no data because it was never updated.
     */
    var isNeverUpdated = true
        protected set

    /**
     * Instantly starts data update.
     */
    abstract fun update(): Completable

    /**
     * Starts data update only on subscription.
     */
    open fun updateDeferred(): Completable {
        return Completable.defer { update() }
    }

    /**
     * Marks data as not fresh
     * @see Repository.isFresh
     */
    open fun invalidate() {
        synchronized(this) {
            isFresh = false
        }
    }

    /**
     * Instantly starts data update if it's not fresh.
     * @see Repository.isFresh
     */
    open fun updateIfNotFresh(): Completable {
        return synchronized(this) {
            if (!isFresh) {
                update()
            } else {
                Completable.complete()
            }
        }
    }

    /**
     * Starts data update it it's not fresh only on subscription.
     * @see Repository.isFresh
     */
    open fun updateIfNotFreshDeferred(): Completable {
        return Completable.defer { updateIfNotFresh() }
    }

    /**
     * Instantly starts data it it was ever updated
     * i.e. if someone was ever interested in this repo's data.
     */
    open fun updateIfEverUpdated(): Completable {
        return if (!isNeverUpdated)
            update()
        else
            Completable.complete()
    }
}