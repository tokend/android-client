package org.tokend.template.features.localaccount.importt.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import java.util.*

class ImportLocalAccountFromMnemonicUseCase(
        private val mnemonicPhrase: String,
        private val mnemonicCode: MnemonicCode,
        localAccountRepository: LocalAccountRepository
) : ImportLocalAccountUseCase(localAccountRepository) {
    override fun getLocalAccount(): Single<LocalAccount> {
        return {
            val entropy = mnemonicCode.toEntropy(
                    mnemonicPhrase
                            .trim()
                            .toLowerCase(Locale.ENGLISH)
                            .replace(Regex.fromLiteral("[\\n+|\\s+]"), " ")
                            .split(' ')
            )

            LocalAccount.fromEntropy(entropy)
        }.toSingle().subscribeOn(Schedulers.computation())
    }
}