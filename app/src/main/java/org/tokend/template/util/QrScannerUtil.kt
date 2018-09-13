package org.tokend.template.util

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator

object QrScannerUtil {
    fun openScanner(activity: Activity) {
        IntentIntegrator(activity)
                .defaultSetup()
                .initiateScan()
    }

    fun openScanner(fragment: Fragment) {
        IntentIntegrator.forSupportFragment(fragment)
                .defaultSetup()
                .initiateScan()
    }

    private fun IntentIntegrator.defaultSetup(): IntentIntegrator {
        return this
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .setPrompt("")
    }

    fun getStringFromResult(requestCode: Int, resultCode: Int, result: Intent?): String? {
        return IntentIntegrator.parseActivityResult(requestCode, resultCode, result)?.contents
    }
}