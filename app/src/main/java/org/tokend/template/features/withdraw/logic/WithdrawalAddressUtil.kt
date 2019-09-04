package org.tokend.template.features.withdraw.logic

import android.net.Uri
import org.tokend.template.extensions.tryOrNull

class WithdrawalAddressUtil {
    /**
     * @return address from crypto invoice, [null] if invoice can't be parsed.
     *
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki#examples">
     *     Bitcoin invoice examples</a>
     */
    fun extractAddressFromInvoice(invoiceString: String) = tryOrNull {
        Uri.parse(
                invoiceString
                        .replace("://", ":")
                        .replace(":", "://")
        ).host
    }
}