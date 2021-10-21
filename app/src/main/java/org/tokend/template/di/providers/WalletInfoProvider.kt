package org.tokend.template.di.providers

import org.tokend.template.logic.credentials.model.WalletInfoRecord

interface WalletInfoProvider {
    fun setWalletInfo(walletInfo: WalletInfoRecord?)
    fun getWalletInfo(): WalletInfoRecord?
}