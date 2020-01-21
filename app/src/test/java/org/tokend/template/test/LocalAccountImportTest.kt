package org.tokend.template.test

import io.reactivex.Maybe
import org.junit.Assert
import org.junit.Test
import org.tokend.template.data.repository.base.MemoryOnlyObjectPersistence
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.features.localaccount.importt.logic.ImportLocalAccountFromMnemonicUseCase
import org.tokend.template.features.localaccount.importt.logic.ImportLocalAccountFromSecretSeedUseCase
import org.tokend.template.features.localaccount.mnemonic.logic.EnglishMnemonicWords
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.repository.LocalAccountRepository
import org.tokend.template.features.userkey.logic.UserKeyProvider
import org.tokend.template.util.cipher.Aes256GcmDataCipher

class LocalAccountImportTest {
    private fun getDummyStorage() = MemoryOnlyObjectPersistence<LocalAccount>()

    private fun getDummyUserKeyProvider(): UserKeyProvider {
        return object : UserKeyProvider {
            override fun getUserKey(isRetry: Boolean): Maybe<CharArray> {
                return Maybe.just("0000".toCharArray())
            }
        }
    }

    @Test
    fun fromMnemonicPhrase() {
        val mnemonic = "tonight fat have keen intact happy social powder tired shaft length cram"
        val code = MnemonicCode(EnglishMnemonicWords.LIST)
        val expectedAccountId = "GD3DTFJZMEZ5WOMCDNGNR4EQSOCFGLYWVS3FD3H3C424LRZRB4NNBQPR"

        val storage = getDummyStorage()
        val repository = LocalAccountRepository(storage)

        val account = ImportLocalAccountFromMnemonicUseCase(
                mnemonic,
                code,
                Aes256GcmDataCipher(),
                getDummyUserKeyProvider(),
                repository
        )
                .perform()
                .blockingGet()

        Assert.assertEquals(expectedAccountId, account.accountId)
        Assert.assertNotNull("Account must have an entropy if it's imported from mnemonic",
                account.entropy)
        checkRepositoryAndStorage(storage, repository, expectedAccountId)

    }

    @Test
    fun fromSecretSeed() {
        val seed = "SAMJKTZVW5UOHCDK5INYJNORF2HRKYI72M5XSZCBYAHQHR34FFR4Z6G4".toCharArray()
        val expectedAccountId = "GBA4EX43M25UPV4WIE6RRMQOFTWXZZRIPFAI5VPY6Z2ZVVXVWZ6NEOOB"

        val storage = getDummyStorage()
        val repository = LocalAccountRepository(storage)

        val account = ImportLocalAccountFromSecretSeedUseCase(
                seed,
                Aes256GcmDataCipher(),
                getDummyUserKeyProvider(),
                repository
        )
                .perform()
                .blockingGet()

        Assert.assertEquals(expectedAccountId, account.accountId)
        Assert.assertNull("Account must not have an entropy if it's imported from seed",
                account.entropy)
        checkRepositoryAndStorage(storage, repository, expectedAccountId)

    }

    private fun checkRepositoryAndStorage(storage: ObjectPersistence<LocalAccount>,
                                          repository: LocalAccountRepository,
                                          expectedAccountId: String) {
        val localAccount = repository.item

        Assert.assertNotNull("Local account repository must contain an account after import", localAccount)
        localAccount!!

        Assert.assertEquals(expectedAccountId, localAccount.accountId)

        val storedAccount = storage.loadItem()

        Assert.assertNotNull("Storage must contain an account after import", storedAccount)
        storedAccount!!

        Assert.assertEquals(expectedAccountId, storedAccount.accountId)
    }
}