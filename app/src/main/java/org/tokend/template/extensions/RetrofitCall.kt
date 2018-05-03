package org.tokend.template.extensions

import io.reactivex.Single
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response

/**
 * Wraps [Call] into [Single].
 */
fun <T> Call<T>.toSingle(): Single<T> {
    return Single.create<T> {
        val call = this
        call.enqueue(object : Callback<T> {
            override fun onFailure(call: Call<T>?, t: Throwable?) {
                if (!it.isDisposed && t != null) {
                    it.tryOnError(t)
                }
            }

            override fun onResponse(call: Call<T>?, response: Response<T>?) {
                if (!it.isDisposed && response != null) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            it.onSuccess(body)
                        }
                    } else {
                        it.tryOnError(HttpException(response))
                    }
                }
            }
        })
    }
}