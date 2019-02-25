package org.tokend.template.logic.wallet

import io.reactivex.Completable
import org.tokend.crypto.ecdsa.erase
import org.tokend.rx.extensions.toSingle
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account

/**
 * Holds wallet update logic.
 *
 * @param credentialsPersistor if set credentials will be updated on wallet update complete
 */
class WalletUpdateManager(
        private val systemInfoRepository: SystemInfoRepository,
        private val credentialsPersistor: CredentialsPersistor? = null
) {
    /**
     * Updates keychain data and account signers in order to
     * change password (or recover it).
     * Also updates data in [accountProvider] and [walletInfoProvider] on complete.
     *
     * @param apiProvider must be able to provide API and KeyServer signed by active account signer
     * @param accountProvider must provide active account signer, well be updated on complete
     * @param walletInfoProvider must provide actual wallet info, will be updated on complete
     */
    fun updateWalletWithNewPassword(apiProvider: ApiProvider,
                                    accountProvider: AccountProvider,
                                    walletInfoProvider: WalletInfoProvider,
                                    newAccount: Account,
                                    newPassword: CharArray): Completable {
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))
        val wallet = walletInfoProvider.getWalletInfo()
                ?: return Completable.error(IllegalStateException("Cannot obtain current wallet"))
        val signedKeyServer = apiProvider.getSignedKeyServer()
                ?: return Completable.error(IllegalStateException("Cannot obtain signed KeyServer"))

        return systemInfoRepository
                .getNetworkParams()
                .flatMap { netParams ->
                    signedKeyServer.updateWalletPassword(
                            currentWalletInfo = wallet,
                            currentAccount = account,
                            newAccount = newAccount,
                            newPassword = newPassword,
                            keyValueApi = apiProvider.getApi().v3.keyValue,
                            signersApi = apiProvider.getApi().v3.signers,
                            networkParams = netParams
                    )
                            .toSingle()
                }
                .doOnSuccess { newWalletInfo ->
                    walletInfoProvider.setWalletInfo(newWalletInfo)
                    accountProvider.setAccount(newAccount)

                    // Update in persistent storage.
                    credentialsPersistor?.saveCredentials(newWalletInfo, newPassword)
                    newWalletInfo.secretSeed.erase()
                }
                .ignoreElement()
    }
    // endregion
}