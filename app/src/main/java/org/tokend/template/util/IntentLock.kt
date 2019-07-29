package org.tokend.template.util

class IntentLock(
        var hash: Int = 0,
        var time: Long = 0L
) {
    fun set(hash: Int, time: Long) {
        this.hash = hash
        this.time = time
    }

    companion object {
        val instance by lazy {
            IntentLock()
        }
    }
}