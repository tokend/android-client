package io.tokend.template.features.signup.logic

import io.reactivex.Single
import io.tokend.template.features.signin.logic.SignInUseCase
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.session.Session
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.SignerData
import org.tokend.sdk.keyserver.models.WalletCreationResult
import org.tokend.wallet.Account

/**
 * Creates wallet with given credentials and submits it
 *
 * Sets up providers if they are set and the created wallet is verified.
 */
class SignUpUseCase(
    private val login: String,
    private val password: CharArray,
    private val keyServer: KeyServer,
    private val repositoryProvider: RepositoryProvider,
    private val session: Session? = null,
    private val credentialsPersistence: CredentialsPersistence? = null,
    private val walletInfoPersistence: WalletInfoPersistence? = null,
) {
    private lateinit var accounts: List<Account>
    private lateinit var signers: Collection<SignerData>
    private lateinit var walletCreateResult: WalletCreationResult

    fun perform(): Single<WalletCreationResult> {
        return ensureLoginIsFree()
            .flatMap {
                generateAccounts()
            }
            .doOnSuccess { accounts ->
                this.accounts = accounts
            }
            .flatMap {
                getSigners()
            }
            .doOnSuccess { signers ->
                this.signers = signers
            }
            .flatMap {
                createAndSaveWallet()
            }
            .doOnSuccess { walletCreateResult ->
                this.walletCreateResult = walletCreateResult
                if (walletCreateResult.isVerified) {
                    setUpProvidersIfPossible()
                }
            }
    }

    private fun ensureLoginIsFree(): Single<Boolean> {
        return keyServer
            .getLoginParams(login)
            .toSingle()
            .map { false }
            .onErrorResumeNext { error ->
                if (error is InvalidCredentialsException)
                    Single.just(true)
                else
                    Single.error(error)
            }
            .flatMap { isFree ->
                if (!isFree)
                    Single.error(EmailAlreadyTakenException())
                else
                    Single.just(isFree)
            }
    }

    private fun generateAccounts(): Single<List<Account>> {
        return WalletAccountsUtil.getAccountsForNewWallet()
    }

    private fun getSigners(): Single<Collection<SignerData>> {
        return WalletAccountsUtil.getSignersForNewWallet(
            orderedAccountIds = accounts.map(Account::accountId),
            keyValueRepository = repositoryProvider.keyValueEntries
        )
    }

    private fun createAndSaveWallet(): Single<WalletCreationResult> {
        return keyServer.createAndSaveWallet(
            email = login,
            password = password,
            accounts = accounts,
            signers = signers
        ).toSingle()
    }

    private fun setUpProvidersIfPossible() {
        if (session != null) {
            SignInUseCase.updateProviders(
                walletInfo = WalletInfoRecord(walletCreateResult),
                session = session,
                password = password,
                accounts = accounts,
                credentialsPersistence = credentialsPersistence,
                walletInfoPersistence = walletInfoPersistence,
                signInMethod = null
            )
        }
    }
}