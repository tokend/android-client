package org.tokend.template.di.providers

import org.tokend.sdk.keyserver.models.WalletInfo

class WalletInfoProviderImpl : WalletInfoProvider {
    private var walletInfo: WalletInfo? = null

    override fun setWalletInfo(walletInfo: WalletInfo?) {
        this.walletInfo = walletInfo
    }

    override fun getWalletInfo(): WalletInfo? {
        return walletInfo
    }
}