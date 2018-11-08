package org.tokend.template.features.send.repository

import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.base.logic.repository.base.RepositoryCache
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.features.send.Contact
import org.tokend.template.features.send.ContactsManager

class ContactsRepository(val context: Context) : SimpleMultipleItemsRepository<Contact>() {

    override val itemsCache: RepositoryCache<Contact> = ContactsRepositoryCache()

    override fun getItems(): Single<List<Contact>> {
        return ContactsManager.getContacts(context)
                .subscribeOn(Schedulers.computation())
    }
}