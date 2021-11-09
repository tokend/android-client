package io.tokend.template.di.providers

import io.tokend.template.logic.credentials.model.WalletInfoRecord

class WalletInfoProviderFactory {
    fun createWalletInfoProvider(): WalletInfoProvider {
        return WalletInfoProviderImpl()
    }

    fun createWalletInfoProvider(wallet: WalletInfoRecord?): WalletInfoProvider {
        return createWalletInfoProvider().apply { setWalletInfo(wallet) }
    }
}