package org.tokend.template.features.tfa.view.confirmation

import android.content.Context
import android.support.annotation.StyleRes
import android.support.v7.app.AlertDialog
import org.tokend.sdk.api.tfa.model.TfaFactorCreationResult
import org.tokend.template.R

/**
 * Dialog used for created TFA factor confirmation
 *
 * @param confirmationAttributes new factor attributes
 *
 * @see TfaFactorCreationResult
 */
abstract class TfaConfirmationDialog(
        protected val context: Context,
        protected val confirmationAttributes: Map<String, Any>,
        @StyleRes
        protected val style: Int? = null
) {
    protected open fun getStyledDialogBuilder(): AlertDialog.Builder {
        val builder = if (style != null)
            AlertDialog.Builder(context, style)
        else
            AlertDialog.Builder(context)

        return builder
                .setTitle(R.string.tfa_add_dialog_title)
    }

    /**
     * @param confirmationCallback invoke with true on confirmation success,
     * invoke with false otherwise
     *
     */
    abstract fun show(confirmationCallback: (Boolean) -> Unit)
}