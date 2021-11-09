package io.tokend.template.features.localaccount.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.features.localaccount.model.LocalAccount

class LocalAccountRepository(
    private val localAccountPersistence: ObjectPersistence<LocalAccount>
) : SingleItemRepository<LocalAccountRepository.Item>() {
    sealed class Item {
        object Missing : Item()
        class Present(val localAccount: LocalAccount) : Item()
    }

    val presentAccount: LocalAccount?
        get() = (item as? Item.Present)?.localAccount

    override fun getItem(): Single<Item> {
        return Maybe.defer {
            localAccountPersistence.loadItem().toMaybe()
        }
            .map<Item>(Item::Present)
            .switchIfEmpty(Single.defer { Item.Missing.toSingle() })
            .subscribeOn(Schedulers.newThread())
            .doOnSuccess { isNeverUpdated = false }
    }

    fun useAccount(newAccount: LocalAccount) {
        localAccountPersistence.saveItem(newAccount)
        onNewItem(Item.Present(newAccount))
    }

    fun erase() {
        localAccountPersistence.clear()
        (item as? Item.Present)?.localAccount?.let {
            it.isErased = true
            broadcast()
        }
    }
}