package org.tokend.template.util

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator

/**
 * Contains utilities to work with QR scanner
 */
object QrScannerUtil {
    /**
     * Opens QR scanner with the default setup:
     * no beep, no orientation lock, no labels
     */
    fun openScanner(activity: Activity) {
        IntentIntegrator(activity)
                .defaultSetup()
                .initiateScan()
    }

    /**
     * Opens QR scanner with the default setup:
     * no beep, no orientation lock, no labels
     */
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

    /**
     * @return QR code content if [result] is QR scanner result, null otherwise
     */
    fun getStringFromResult(requestCode: Int, resultCode: Int, result: Intent?): String? {
        return IntentIntegrator.parseActivityResult(requestCode, resultCode, result)?.contents
    }
}