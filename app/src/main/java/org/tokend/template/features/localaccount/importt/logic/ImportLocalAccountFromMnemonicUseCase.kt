package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.template.util.cipher.DataCipher
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.features.userkey.logic.UserKeyProvider
import java.util.*

class ImportLocalAccountFromMnemonicUseCase(
        private val mnemonicPhrase: String,
        private val mnemonicCode: MnemonicCode,
        private val cipher: DataCipher,
        userKeyProvider: UserKeyProvider,
        localAccountRepository: LocalAccountRepository
) : ImportLocalAccountUseCase(userKeyProvider, localAccountRepository) {
    override fun getLocalAccount(): Single<LocalAccount> {
        return {
            val entropy = mnemonicCode.toEntropy(
                    mnemonicPhrase
                            .trim()
                            .toLowerCase(Locale.ENGLISH)
                            .replace(Regex.fromLiteral("[\\n+|\\s+]"), " ")
                            .split(' ')
            )

            LocalAccount.fromEntropy(entropy, cipher, userKey)
        }.toSingle().subscribeOn(Schedulers.computation())
    }
}