package org.tokend.template.extensions

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import org.tokend.template.R

var Context.clipboardText: CharSequence?
    set(value) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (value != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(
                    getString(R.string.app_name),
                    value
            ))
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