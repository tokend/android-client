package org.tokend.template.logic.persistance

import android.content.SharedPreferences
import org.tokend.template.features.signin.logic.SignInMethod

class SessionInfoStorage(
        private val sharedPreferences: SharedPreferences
) {
    fun saveLastSignInMethod(signInMethod: SignInMethod) {
        sharedPreferences
                .edit()
                .putString(LAST_SIGN_IN_METHOD_KEY, signInMethod.toString())
                .apply()
    }

    fun loadLastSignInMethod(): SignInMethod? {
        return sharedPreferences
                .getString(LAST_SIGN_IN_METHOD_KEY, "")
                .takeIf { it.isNotEmpty() }
                ?.let { SignInMethod.valueOf(it) }
    }

    fun clear() {
        sharedPreferences
                .edit()
                .remove(LAST_SIGN_IN_METHOD_KEY)
                .apply()
    }

    private companion object {
        private const val LAST_SIGN_IN_METHOD_KEY = "last_sign_in_method"
    }
}