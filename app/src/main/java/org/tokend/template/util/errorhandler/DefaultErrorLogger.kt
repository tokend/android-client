package org.tokend.template.util.errorhandler

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.tokend.template.BuildConfig
import retrofit2.HttpException
import retrofit2.Response

class DefaultErrorLogger : ErrorLogger {
    override fun log(error: Throwable) {
        var e = error

        if (error is HttpException) {
            val rawResponse = error.response().raw()
            val request = rawResponse.request()
            e = HttpException(Response.error<Any>(
                    error.response().errorBody(),
                    rawResponse
                            .newBuilder()
                            .message(rawResponse.message() + " (${request.method()} ${request.url()})")
                            .build()
            ))
            e.stackTrace = emptyArray()
        }

        if (BuildConfig.ENABLE_ANALYTICS) {
            Firebase.analytics.logEvent(EVENT_TYPE, Bundle().apply {
                putString(MESSAGE, e.message)
                putString(STACK_TRACE, e.stackTrace.toString())
            })
        }
        e.printStackTrace()
    }

    companion object {
        private const val EVENT_TYPE = "Crash"
        private const val MESSAGE = "Message"
        private const val STACK_TRACE = "Stack Trace"
    }
}