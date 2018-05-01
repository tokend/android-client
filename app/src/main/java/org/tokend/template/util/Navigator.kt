package org.tokend.template.util

import android.app.Activity
import android.support.v4.app.ActivityCompat
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.singleTop
import org.tokend.template.R
import org.tokend.template.activities.*
import org.tokend.template.activities.settings.SettingsActivity

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

    fun toDashboard(activity: Activity) {
        activity.startActivity(activity.intentFor<DashboardActivity>())
        fadeOut(activity)
    }

    fun toWallet(activity: Activity) {
        activity.startActivity(activity.intentFor<WalletActivity>())
        fadeOut(activity)
    }

    fun toSettings(activity: Activity) {
        activity.startActivity(activity.intentFor<SettingsActivity>())
        fadeOut(activity)
    }
}