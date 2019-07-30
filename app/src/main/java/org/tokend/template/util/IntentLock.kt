package org.tokend.template.util

import android.content.Context
import android.content.Intent
import java.util.*

object IntentLock {

    var hash: Int = 0
    var time: Long = 0L

    private fun set(hash: Int, time: Long) {
        this.hash = hash
        this.time = time
    }

    fun checkIntent(intent: Intent, context: Context?): Boolean {
        val context = context ?: return false
        val hash = intent.resolveActivity(context.packageManager).hashCode()
        val time = Date().time
        return if (this.hash != hash || time - this.time > 500) {
            set(hash, time)
            true
        } else false
    }
}