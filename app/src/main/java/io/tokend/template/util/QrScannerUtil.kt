package io.tokend.template.util

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator
import io.tokend.template.features.qr.ScanQrActivity
import io.tokend.template.util.navigation.ActivityRequest

/**
 * Contains utilities to work with QR scanner
 */
object QrScannerUtil {
    /**
     * Opens QR scanner with the default setup:
     * no beep, no orientation lock, no labels
     */
    fun openScanner(
        activity: Activity,
        prompt: String = "",
    ) = ActivityRequest(IntentIntegrator.REQUEST_CODE, this::getStringFromResult).also {
        IntentIntegrator(activity)
            .defaultSetup()
            .setPrompt(prompt)
            .initiateScan()
    }

    /**
     * Opens QR scanner with the default setup:
     * no beep, no orientation lock, no labels
     */
    fun openScanner(
        fragment: Fragment,
        prompt: String = "",
    ) = ActivityRequest(IntentIntegrator.REQUEST_CODE, this::getStringFromResult).also {
        IntentIntegrator
            .forSupportFragment(fragment)
            .defaultSetup()
            .setPrompt(prompt)
            .initiateScan()
    }

    private fun IntentIntegrator.defaultSetup(): IntentIntegrator {
        return this
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setCaptureActivity(ScanQrActivity::class.java)
    }

    private fun getStringFromResult(intent: Intent?): String? =
        IntentIntegrator.parseActivityResult(
            IntentIntegrator.REQUEST_CODE,
            Activity.RESULT_OK,
            intent
        )
            ?.contents
}