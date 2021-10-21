package org.tokend.template.logic.persistence

import android.content.Context
import android.os.Build
import org.tokend.template.extensions.defaultSharedPreferences

/**
 * Manages background app lock preference.
 */
class BackgroundLockManager(context: Context) {
    private val preferences = context.defaultSharedPreferences

    /**
     * Depends on whether secure storage for password is available.
     *
     * @see CredentialsPersistor
     */
    val canBackgroundLockBeDisabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    /**
     * If enabled then lock screen must ask for auth.
     */
    var isBackgroundLockEnabled
        get() = preferences.getBoolean(PREFERENCE_KEY, ENABLED_BY_DEFAULT)
        set(value) {
            preferences.edit().putBoolean(PREFERENCE_KEY, value).apply()
        }

    companion object {
        private const val ENABLED_BY_DEFAULT = true
        private const val PREFERENCE_KEY = "background_lock"
    }
}