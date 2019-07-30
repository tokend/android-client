package org.tokend.template.util

import android.content.Context
import android.content.Intent
import org.tokend.template.util.IntentLock.THRESHOLD_MS
import java.util.*

/**
 * Prevents same intent second launch in a short time
 *
 * @see THRESHOLD_MS
 */
object IntentLock {
    private const val THRESHOLD_MS = 300

    var hash: Int = 0
    var time: Long = 0L

    private fun set(hash: Int, time: Long) {
        this.hash = hash
        this.time = time
    }

    /**
     * @return true if the intent can be launched and set's a lock for it
     */
    fun checkIntent(intent: Intent, context: Context?): Boolean {
        val context = context ?: return false
        val hash = intent.resolveActivity(context.packageManager).hashCode()
        val time = Date().time
        return if (this.hash != hash || time - this.time > THRESHOLD_MS) {
            set(hash, time)
            true
        } else false
    }
}