package org.tokend.template.features.localaccount.repository

import io.reactivex.Observable
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.storage.LocalAccountPersistor

class LocalAccountRepository(
        private val storage: LocalAccountPersistor
): SimpleSingleItemRepository<LocalAccount>() {
    override fun getItem(): Observable<LocalAccount> {
        return Observable.defer {
            val existing = storage.load()

            if (existing != null) {
                Observable.just(existing)
            } else {
                Observable.empty()
            }
        }
    }

    fun useAccount(newAccount: LocalAccount) {
        storage.save(newAccount)
        onNewItem(newAccount)
    }
}