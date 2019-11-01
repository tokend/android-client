package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.features.userkey.logic.UserKeyProvider
import org.tokend.template.util.cipher.DataCipher
import java.util.*

class ImportLocalAccountFromMnemonicUseCase(
        private val mnemonicPhrase: String,
        private val mnemonicCode: MnemonicCode,
        private val cipher: DataCipher,
        userKeyProvider: UserKeyProvider,
        localAccountRepository: LocalAccountRepository
) : ImportLocalAccountUseCase(userKeyProvider, localAccountRepository) {
    private lateinit var entropy: ByteArray
    private lateinit var userKey: CharArray

    override fun getLocalAccount(): Single<LocalAccount> {
        return getEntropy()
                .doOnSuccess { entropy ->
                    this.entropy = entropy
                }
                .flatMap {
                    getUserKey()
                }
                .doOnSuccess { userKey ->
                    this.userKey = userKey
                }
                .flatMap {
                    getEncryptedLocalAccount()
                }
    }

    private fun getEntropy(): Single<ByteArray> {
        return {
            mnemonicCode.toEntropy(
                    mnemonicPhrase
                            .trim()
                            .toLowerCase(Locale.ENGLISH)
                            .replace(Regex.fromLiteral("[\\n+|\\s+]"), " ")
                            .split(' ')
            )
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun getEncryptedLocalAccount(): Single<LocalAccount> {
        return {
            LocalAccount.fromEntropy(entropy, cipher, userKey)
        }.toSingle().subscribeOn(Schedulers.computation())
    }
}