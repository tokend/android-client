package org.tokend.template.features.localaccount.storage

import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.schedulers.Schedulers
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.features.localaccount.model.LocalAccount

class LocalAccountRepository(
        private val localAccountPersistence: ObjectPersistence<LocalAccount>
) : SingleItemRepository<LocalAccount>() {
    override fun getItem(): Maybe<LocalAccount> {
        return Maybe.defer {
            localAccountPersistence.loadItem().toMaybe()
        }
                .subscribeOn(Schedulers.newThread())
                .doOnComplete { isNeverUpdated = false }
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