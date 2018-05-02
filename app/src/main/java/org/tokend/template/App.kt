package org.tokend.template

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import io.reactivex.subjects.BehaviorSubject
import org.tokend.template.base.logic.AppState
import java.util.*

class App : MultiDexApplication() {
    companion object {
        private const val GO_TO_BACKGROUND_TIMEOUT = 2000
        private const val LOG_TAG = "TokenD App"

        private var _context: Context? = null
        val context: Context
            get() = _context!!

        var state: AppState? = null
            private set

        val areGooglePlayServicesAvailable: Boolean
            get() {
                val googleApiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
                return resultCode == ConnectionResult.SUCCESS
            }

        /**
         * Emits value when app goes to the background or comes to the foreground.
         * [true] means that the app is currently in the background.
         */
        val backgroundStateSubject = BehaviorSubject.createDefault(false)

        fun clearState() {
            state = AppState()
        }
    }

    private var isInForeground = false
    private val goToBackgroundTimer = Timer()
    private var goToBackgroundTask: TimerTask? = null

    override fun onCreate() {
        super.onCreate()

        _context = this

        try {
            if (areGooglePlayServicesAvailable) {
                ProviderInstaller.installIfNeeded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        clearState()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                setIsInForeground(true)
            }

            override fun onActivityPaused(a: Activity) {
                setIsInForeground(false)
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}

            override fun onActivityStarted(a: Activity) {}

            override fun onActivityStopped(a: Activity) {}

            override fun onActivitySaveInstanceState(a: Activity, b: Bundle?) {}

            override fun onActivityDestroyed(a: Activity) {}
        })
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }

    // region Background/Foreground state.
    fun setIsInForeground(isInForeground: Boolean) {
        if (isInForeground) {
            cancelBackgroundCallback()
        }

        if (this.isInForeground == isInForeground) {
            return
        }

        if (isInForeground) {
            this.isInForeground = true
            onAppComesToForeground()
        } else {
            scheduleBackgroundCallback()
        }
    }

    private fun scheduleBackgroundCallback() {
        cancelBackgroundCallback()
        goToBackgroundTask = getGoToBackgroundTask()
        goToBackgroundTimer.schedule(goToBackgroundTask, GO_TO_BACKGROUND_TIMEOUT.toLong())
    }

    private fun getGoToBackgroundTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                this@App.isInForeground = false
                onAppGoesToBackground()
            }
        }
    }

    private fun cancelBackgroundCallback() {
        if (goToBackgroundTask != null) {
            goToBackgroundTask?.cancel()
        }

        goToBackgroundTimer.purge()
    }

    private fun onAppGoesToBackground() {
        Log.d(LOG_TAG, "onAppGoesToBackground()")
        backgroundStateSubject.onNext(true)
    }

    private fun onAppComesToForeground() {
        Log.d(LOG_TAG, "onAppComesToForeground()")
        backgroundStateSubject.onNext(false)
    }
    // endregion
}