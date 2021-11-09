package io.tokend.template.features.userkey.view

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import io.reactivex.Maybe
import io.reactivex.subjects.MaybeSubject
import io.tokend.template.features.userkey.logic.UserKeyProvider
import java.util.*

class ActivityUserKeyProvider(
    private val activityClass: Class<out UserKeyActivity>,
    private val parentActivity: Activity? = null,
    private val parentFragment: Fragment? = null
) : UserKeyProvider {
    private var resultSubject: MaybeSubject<CharArray>? = null
    private var requestCode = 0

    override fun getUserKey(isRetry: Boolean): Maybe<CharArray> {
        return resultSubject ?: (MaybeSubject.create<CharArray>()
            .also {
                resultSubject = it
                openInputActivity(isRetry)
            })
    }

    private fun getRequestCode(renew: Boolean = false): Int {
        if (renew) {
            requestCode = Random().nextInt() and 0xffff
        }
        return requestCode
    }

    private fun openInputActivity(isRetry: Boolean) {
        if (parentActivity != null) {
            parentActivity.startActivityForResult(
                Intent(parentActivity, activityClass).also {
                    it.putExtra(UserKeyActivity.IS_RETRY_EXTRA, isRetry)
                },
                getRequestCode(true)
            )
        } else {
            parentFragment?.startActivityForResult(
                Intent(parentFragment.activity, activityClass).also {
                    it.putExtra(UserKeyActivity.IS_RETRY_EXTRA, isRetry)
                },
                getRequestCode(true)
            )
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != getRequestCode()) {
            return false
        }

        val key = data?.getCharArrayExtra(UserKeyActivity.USER_KEY_RESULT_EXTRA)

        return if (resultCode == Activity.RESULT_CANCELED || key == null) {
            cancelInput()
            true
        } else {
            postKey(key)
            true
        }
    }

    private fun cancelInput() {
        val backupSubject = resultSubject
        resultSubject = null
        backupSubject?.onComplete()
    }

    private fun postKey(key: CharArray) {
        val backupSubject = resultSubject
        resultSubject = null
        backupSubject?.onSuccess(key)
    }
}