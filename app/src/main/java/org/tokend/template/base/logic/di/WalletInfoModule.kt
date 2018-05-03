package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import org.tokend.sdk.keyserver.models.WalletInfo

@Module
class WalletInfoModule {
    companion object {
        var walletInfo: WalletInfo? = null
    }

    @Provides
    fun provideWalletInfo(): WalletInfo? {
        return walletInfo
    }
}