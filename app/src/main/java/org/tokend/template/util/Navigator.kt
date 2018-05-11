package org.tokend.template.util

import android.app.Activity
import android.content.Context
import android.support.v4.app.ActivityCompat
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import org.jetbrains.anko.singleTop
import org.tokend.template.R
import org.tokend.template.base.activities.ChangePasswordActivity
import org.tokend.template.base.activities.MainActivity
import org.tokend.template.base.activities.RecoveryActivity
import org.tokend.template.base.activities.SignInActivity
import org.tokend.template.base.activities.qr.ShareQrActivity
import org.tokend.template.base.activities.signup.RecoverySeedActivity
import org.tokend.template.base.activities.signup.SignUpActivity

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

    fun toSignIn(context: Context) {
        context.startActivity(context.intentFor<SignInActivity>()
                .newTask())
    }

    fun toSignIn(activity: Activity) {
        activity.startActivity(activity.intentFor<SignInActivity>()
                .singleTop()
                .clearTop())
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

    fun openRecoverySeedSaving(activity: Activity, requestCode: Int, seed: String) {
        activity.startActivityForResult(activity.intentFor<RecoverySeedActivity>(
                RecoverySeedActivity.SEED_EXTRA to seed
        ), requestCode)
    }
    
    fun openPasswordChange(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(activity.intentFor<ChangePasswordActivity>(),
                requestCode)
    }
}