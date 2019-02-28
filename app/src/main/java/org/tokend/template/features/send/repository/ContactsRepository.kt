package org.tokend.template.features.send.repository

import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.features.send.logic.ContactsManager
import org.tokend.template.features.send.model.Contact


class ContactsRepository(val context: Context,
                         itemsCache: RepositoryCache<Contact>
) : SimpleMultipleItemsRepository<Contact>(itemsCache) {

    override fun getItems(): Single<List<Contact>> {
        return ContactsManager.getContacts(context)
                .subscribeOn(Schedulers.computation())
    }
}