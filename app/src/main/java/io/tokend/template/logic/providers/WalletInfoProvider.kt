package io.tokend.template.logic.providers

import io.tokend.template.logic.credentials.model.WalletInfoRecord

interface WalletInfoProvider {
    fun setWalletInfo(walletInfo: WalletInfoRecord?)
    fun getWalletInfo(): WalletInfoRecord
    fun hasWalletInfo(): Boolean
}