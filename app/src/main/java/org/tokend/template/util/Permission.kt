package org.tokend.template.util

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat

/**
 * Handles specified Android runtime permission.
 */
class Permission(private val permission: String,
                 private val requestCode: Int) {

    private var grantedCallback: (() -> Unit)? = null
    private var deniedCallback: (() -> Unit)? = null

    fun check(activity: Activity, action: () -> Unit, deniedAction: () -> Unit) {
        this.grantedCallback = action
        this.deniedCallback = deniedAction
        if (ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }

    fun check(activity: Activity, action: () -> Unit) {
        this.grantedCallback = action
        if (ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }

    fun check(fragment: Fragment, action: () -> Unit, deniedAction: () -> Unit) {
        this.grantedCallback = action
        this.deniedCallback = deniedAction
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            fragment.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    fun check(fragment: Fragment, action: () -> Unit) {
        this.grantedCallback = action
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            fragment.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>,
                               grantResults: IntArray) {
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