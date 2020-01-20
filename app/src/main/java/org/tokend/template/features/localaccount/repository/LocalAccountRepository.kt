package org.tokend.template.features.localaccount.repository

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.features.localaccount.model.LocalAccount

class LocalAccountRepository(
        private val localAccountPersistence: ObjectPersistence<LocalAccount>
): SingleItemRepository<LocalAccount>() {
    override fun getItem(): Single<LocalAccount> {
        return Single.defer {
            val existing = localAccountPersistence.loadItem()

            if (existing != null) {
                Single.just(existing)
            } else {
                Single.error(IllegalStateException("No local account found"))
            }
        }.subscribeOn(Schedulers.newThread())
    }

    fun useAccount(newAccount: LocalAccount) {
        localAccountPersistence.saveItem(newAccount)
        onNewItem(newAccount)
    }

    fun erase() {
        localAccountPersistence.clear()
        item?.let {
            it.isErased = true
            broadcast()
        }
    }
}