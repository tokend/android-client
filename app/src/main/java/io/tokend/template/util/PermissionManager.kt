package io.tokend.template.util

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Handles specified Android runtime permission.
 */
class PermissionManager(
    private val permission: String,
    private val requestCode: Int
) {

    private var grantedCallback: (() -> Unit)? = null
    private var deniedCallback: (() -> Unit)? = null

    /**
     * Checks for the permission, requests it if needed
     *
     * @param action action to invoke if permission is granted
     * @param deniedAction action to invoke if permission is denied
     */
    fun check(activity: Activity, action: () -> Unit, deniedAction: () -> Unit) {
        this.grantedCallback = action
        this.deniedCallback = deniedAction
        if (ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }

    /**
     * Checks for the permission, requests it if needed
     *
     * @param action action to invoke if permission is granted
     */
    fun check(activity: Activity, action: () -> Unit) {
        this.grantedCallback = action
        if (ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }

    /**
     * Checks for the permission, requests it if needed
     *
     * @param action action to invoke if permission is granted
     * @param deniedAction action to invoke if permission is denied
     */
    fun check(fragment: Fragment, action: () -> Unit, deniedAction: () -> Unit) {
        this.grantedCallback = action
        this.deniedCallback = deniedAction
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            fragment.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    /**
     * Checks for the permission, requests it if needed
     *
     * @param action action to invoke if permission is granted
     */
    fun check(fragment: Fragment, action: () -> Unit) {
        this.grantedCallback = action
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            fragment.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    /**
     * Handles permission grant result,
     * invokes corresponding action passed to the [check] method
     */
    fun handlePermissionResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == this.requestCode) {
            if (isPermissionGranted(grantResults)) {
                grantedCallback?.invoke()
            } else {
                deniedCallback?.invoke()
            }
        }
    }

    private fun isPermissionGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }
}