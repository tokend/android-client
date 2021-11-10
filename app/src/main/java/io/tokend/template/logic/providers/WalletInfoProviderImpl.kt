package io.tokend.template.logic.providers

import io.tokend.template.logic.credentials.model.WalletInfoRecord

class WalletInfoProviderImpl : WalletInfoProvider {
    private var walletInfo: WalletInfoRecord? = null

    override fun setWalletInfo(walletInfo: WalletInfoRecord?) {
        this.walletInfo = walletInfo
    }

    override fun getWalletInfo(): WalletInfoRecord {
        return walletInfo
            ?: throw NoSuchElementException("There is no wallet info set")
    }

    override fun hasWalletInfo(): Boolean = walletInfo != null
}