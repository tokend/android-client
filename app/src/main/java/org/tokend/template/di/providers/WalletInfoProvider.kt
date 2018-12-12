package org.tokend.template.di.providers

import org.tokend.sdk.keyserver.models.WalletInfo

interface WalletInfoProvider {
    fun setWalletInfo(walletInfo: WalletInfo?)
    fun getWalletInfo(): WalletInfo?
}