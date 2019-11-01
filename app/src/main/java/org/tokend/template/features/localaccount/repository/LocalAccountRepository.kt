package org.tokend.template.features.localaccount.repository

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
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
        }.subscribeOn(Schedulers.newThread())
    }

    fun useAccount(newAccount: LocalAccount) {
        storage.save(newAccount)
        onNewItem(newAccount)
    }

    fun erase() {
        storage.clear()
        item?.let {
            it.isErased = true
            broadcast()
        }
    }
}