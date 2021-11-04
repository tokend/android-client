package org.tokend.template.di.providers

import org.tokend.template.logic.credentials.model.WalletInfoRecord

class WalletInfoProviderImpl : WalletInfoProvider {
    private var walletInfo: WalletInfoRecord? = null

    override fun setWalletInfo(walletInfo: WalletInfoRecord?) {
        this.walletInfo = walletInfo
    }

    override fun getWalletInfo(): WalletInfoRecord? {
        return walletInfo
    }
}