package io.tokend.template.util

import android.content.Context
import android.net.ConnectivityManager

/**
 * Helps to determine current internet connection state
 */
class ConnectionStateUtil(
    private val context: Context
) {
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected ?: false
    }
}