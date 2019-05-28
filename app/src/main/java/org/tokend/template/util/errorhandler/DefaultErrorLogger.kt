package org.tokend.template.util.errorhandler

import com.crashlytics.android.Crashlytics
import org.tokend.template.BuildConfig

class DefaultErrorLogger : ErrorLogger {

    override fun log(error: Throwable) {
        if (BuildConfig.ENABLE_ANALYTICS) Crashlytics.logException(error)
    }
}