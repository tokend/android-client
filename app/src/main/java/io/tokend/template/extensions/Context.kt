package io.tokend.template.extensions

import android.content.*
import android.net.Uri
import android.os.Vibrator
import android.view.LayoutInflater
import androidx.preference.PreferenceManager
import io.tokend.template.R

var Context.clipboardText: CharSequence?
    set(value) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (value != null) {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.app_name),
                    value
                )
            )
        }
    }
    get() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) {
            return null
        }
        if (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) != true) {
            return null
        }
        val primaryClip = clipboard.primaryClip
            ?: return null
        return primaryClip
            .takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
    }

val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.browse(uri: String): Boolean =
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

val Context.vibrator: Vibrator
    get() = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

inline fun <reified T> Context.intentFor() = Intent(this, T::class.java)