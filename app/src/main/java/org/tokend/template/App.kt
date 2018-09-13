package org.tokend.template

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.multidex.MultiDexApplication
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.CookieCache
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import io.fabric.sdk.android.Fabric
import io.reactivex.subjects.BehaviorSubject
import org.tokend.template.base.logic.di.*
import org.tokend.template.base.logic.model.UrlConfig
import org.tokend.template.base.logic.persistance.UrlConfigPersistor
import org.tokend.template.util.Navigator
import java.util.*

class App : MultiDexApplication() {
    companion object {
        private const val GO_TO_BACKGROUND_TIMEOUT = 2000
        private const val IMAGE_CACHE_SIZE_MB = 8L
        private const val LOG_TAG = "TokenD App"

        /**
         * Emits value when app goes to the background or comes to the foreground.
         * [true] means that the app is currently in the background.
         */
        val backgroundStateSubject = BehaviorSubject.createDefault(false)
    }

    private var isInForeground = false
    private val goToBackgroundTimer = Timer()
    private var goToBackgroundTask: TimerTask? = null

    private lateinit var cookiePersistor: CookiePersistor
    private lateinit var cookieCache: CookieCache

    lateinit var stateComponent: AppStateComponent

    private val areGooglePlayServicesAvailable: Boolean
        get() {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            return resultCode == ConnectionResult.SUCCESS
        }

    override fun onCreate() {
        super.onCreate()

        try {
            if (areGooglePlayServicesAvailable) {
                ProviderInstaller.installIfNeeded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

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

        initCookies()
        initStateComponent()
        initPicasso()
        initCrashlytics()
    }

    private fun initCrashlytics() {
        val crashlytics = Crashlytics.Builder()
                .core(
                        CrashlyticsCore.Builder()
                                .disabled(!BuildConfig.ENABLE_ANALYTICS)
                                .build()
                )
                .build()
        Fabric.with(this, crashlytics)
    }

    private fun initPicasso() {
        val picasso = Picasso.Builder(this)
                .downloader(OkHttp3Downloader(cacheDir,
                        IMAGE_CACHE_SIZE_MB * 1024 * 1024))
                .build()
        Picasso.setSingletonInstance(picasso)
    }

    // region State
    private fun initCookies() {
        cookiePersistor = SharedPrefsCookiePersistor(this)
        cookieCache = SetCookieCache()
    }

    private fun getCredentialsPreferences(): SharedPreferences {
        return getSharedPreferences("CredentialsPersistence",
                Context.MODE_PRIVATE)
    }

    private fun getNetworkPreferences(): SharedPreferences {
        return getSharedPreferences("NetworkPersistence",
                Context.MODE_PRIVATE)
    }

    private fun initStateComponent() {
        val cookieJar = PersistentCookieJar(cookieCache, cookiePersistor)

        val defaultUrlConfig = UrlConfig(BuildConfig.API_URL, BuildConfig.STORAGE_URL,
                BuildConfig.KYC_URL, BuildConfig.TERMS_URL)

        stateComponent = DaggerAppStateComponent.builder()
                .urlConfigProviderModule(UrlConfigProviderModule(
                        if (BuildConfig.IS_NETWORK_SPECIFIED_BY_USER)
                            UrlConfigPersistor(getNetworkPreferences()).loadConfig()
                                    ?: defaultUrlConfig
                        else
                            defaultUrlConfig
                ))
                .apiProviderModule(ApiProviderModule(cookieJar))
                .persistenceModule(PersistenceModule(
                        getCredentialsPreferences(),
                        getNetworkPreferences()
                ))
                .utilModule(UtilModule(this))
                .build()
    }

    private fun clearState() {
        initStateComponent()
    }

    private fun clearCookies() {
        cookiePersistor.clear()
        cookieCache.clear()
    }

    @SuppressLint("ApplySharedPref")
    fun signOut(activity: Activity?) {
        getCredentialsPreferences().edit().clear().commit()
        clearCookies()
        clearState()

        Navigator.toSignIn(this)

        activity?.let {
            it.setResult(Activity.RESULT_CANCELED, null)
            ActivityCompat.finishAffinity(it)
        }
    }
    // endregion

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