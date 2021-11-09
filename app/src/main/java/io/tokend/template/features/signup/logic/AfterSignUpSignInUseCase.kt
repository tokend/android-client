package io.tokend.template.features.signup.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.logic.Session
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence

/**
 * Saves current wallet info from [session] and credentials to [walletInfoPersistence]
 * and [walletInfoPersistence] and performs post sign in.
 */
class AfterSignUpSignInUseCase(
    private val login: String,
    private val password: CharArray,
    private val session: Session,
    private val credentialsPersistence: CredentialsPersistence,
    private val walletInfoPersistence: WalletInfoPersistence,
    private val postSignInActions: (() -> Completable)
) {
    private lateinit var currentWalletInfo: WalletInfoRecord

    fun perform(): Completable {
        return getCurrentCurrentInfo()
            .doOnSuccess { currentWalletInfo ->
                this.currentWalletInfo = currentWalletInfo
            }
            .flatMap {
                performPostSignIn()
            }
            .doOnSuccess {
                updatePersistence()
            }
            .ignoreElement()
    }

    private fun getCurrentCurrentInfo(): Single<WalletInfoRecord> {
        return session.getWalletInfo()
            .toMaybe()
            .switchIfEmpty(Single.error(IllegalStateException("No wallet info found")))
    }

    private fun performPostSignIn(): Single<Boolean> {
        return postSignInActions()
            .toSingleDefault(true)
    }

    private fun updatePersistence() {
        credentialsPersistence.saveCredentials(login, password)
        walletInfoPersistence.saveWalletInfo(currentWalletInfo, password)
    }
}