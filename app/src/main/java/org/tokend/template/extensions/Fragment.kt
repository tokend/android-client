package org.tokend.template.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment

inline fun <reified T : Fragment> Fragment.withArguments(bundle: Bundle) = let {
    arguments = bundle
    it as T
}