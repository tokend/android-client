package org.tokend.template.extensions

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Wraps [Call] into [Observable] with call cancellation on dispose.
 */
fun <T> Call<T>.toObservable(): Observable<T> {
    return Observable.create<T> {
        val call = this
        call.enqueue(object : Callback<T> {
            override fun onFailure(call: Call<T>?, t: Throwable?) {
                if (!it.isDisposed && t != null) {
                    it.onError(t)
                }
            }

            override fun onResponse(call: Call<T>?, response: Response<T>?) {
                if (!it.isDisposed && response != null) {
                    it.onNext(response.body())
                    it.onComplete()
                }
            }

        })

        var disposed = false
        it.setDisposable(object : Disposable {
            override fun isDisposed(): Boolean {
                return disposed
            }

            override fun dispose() {
                disposed = true
                call.cancel()
            }
        })
    }
}