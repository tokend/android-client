package org.tokend.template.features.withdraw.logic

import android.net.Uri

class WithdrawalAddressUtil {
    /**
     * @return address from crypto invoice, [null] if invoice can't be parsed.
     *
     * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki#examples">
     *     Bitcoin invoice examples</a>
     */
    fun extractAddressFromInvoice(invoiceString: String): String? {
        return try {
            Uri.parse(
                    invoiceString
                            .replace("://", ":")
                            .replace(":", "://")
            ).host
        } catch (_: Exception) {
            null
        }
    }
}