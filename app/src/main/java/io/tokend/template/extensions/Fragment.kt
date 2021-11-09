package io.tokend.template.extensions

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

inline fun <reified T : Fragment> Fragment.withArguments(bundle: Bundle) = let {
    arguments = bundle
    it as T
}

val Fragment.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(requireContext())