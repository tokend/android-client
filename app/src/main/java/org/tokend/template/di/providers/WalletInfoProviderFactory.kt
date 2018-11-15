package org.tokend.template.di.providers

import org.tokend.sdk.keyserver.models.WalletInfo

class WalletInfoProviderFactory {
    fun createWalletInfoProvider(): WalletInfoProvider {
        return WalletInfoProviderImpl()
    }

    fun createWalletInfoProvider(wallet: WalletInfo?): WalletInfoProvider {
        return createWalletInfoProvider().apply { setWalletInfo(wallet) }
    }
}