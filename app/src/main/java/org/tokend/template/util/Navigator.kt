package org.tokend.template.util

import android.app.Activity
import android.support.v4.app.ActivityCompat
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.singleTop
import org.tokend.template.R
import org.tokend.template.base.activities.MainActivity
import org.tokend.template.base.activities.RecoveryActivity
import org.tokend.template.base.activities.SignInActivity
import org.tokend.template.base.activities.SignUpActivity
import org.tokend.template.base.activities.qr.ShareQrActivity

/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
object Navigator {
    private fun fadeOut(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        activity.finish()
    }

    fun openSignUp(activity: Activity) {
        activity.startActivity(activity.intentFor<SignUpActivity>())
    }

    fun openRecovery(activity: Activity,
                     email: String? = null) {
        activity.startActivity(activity.intentFor<RecoveryActivity>(
                RecoveryActivity.EMAIL_EXTRA to email
        ))
    }

    fun toSignIn(activity: Activity) {
        activity.startActivity(activity.intentFor<SignInActivity>().singleTop())
        activity.finish()
    }

    fun toMainActivity(activity: Activity) {
        activity.startActivity(activity.intentFor<MainActivity>())
        fadeOut(activity)
    }

    fun openQrShare(activity: Activity,
                    title: String,
                    data: String,
                    shareDialogText: String,
                    topText: String? = null) {
        activity.startActivity(activity.intentFor<ShareQrActivity>(
                ShareQrActivity.DATA_EXTRA to data,
                ShareQrActivity.TITLE_EXTRA to title,
                ShareQrActivity.SHARE_DIALOG_TEXT_EXTRA to shareDialogText,
                ShareQrActivity.TOP_TEXT_EXTRA to topText
        ))
    }
}