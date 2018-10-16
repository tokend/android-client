package org.tokend.template.extensions

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import org.tokend.sdk.api.base.ApiCallback
import org.tokend.sdk.api.base.ApiRequest
import org.tokend.sdk.api.base.model.ApiResponse

fun <T> ApiRequest<T>.toSingle(): Single<T> {
    return Single.create<T> {
        val request = this

        request.executeAsync(object : ApiCallback<T> {
            override fun onSuccess(response: ApiResponse<T>) {
                if (!it.isDisposed) {
                    it.onSuccess(response.get())
                }
            }

            override fun onError(error: Throwable) {
                if (!it.isDisposed) {
                    it.tryOnError(error)
                }
            }
        })

        it.setDisposable(object : Disposable {
            private var disposed = false
            override fun isDisposed(): Boolean {
                return disposed
            }

            override fun dispose() {
                request.cancel()
                disposed = true
            }
        })
    }
}

fun ApiRequest<Void>.toCompletable(): Completable {
    return Completable.create {
        val request = this

        request.executeAsync(object : ApiCallback<Void> {
            override fun onSuccess(response: ApiResponse<Void>) {
                if (!it.isDisposed) {
                    it.onComplete()
                }
            }

            override fun onError(error: Throwable) {
                if (!it.isDisposed) {
                    it.tryOnError(error)
                }
            }
        })

        it.setDisposable(object : Disposable {
            private var disposed = false
            override fun isDisposed(): Boolean {
                return disposed
            }

            override fun dispose() {
                request.cancel()
                disposed = true
            }
        })
    }
}