package org.tokend.template.base.logic.repository.contacts

import android.content.Context
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.template.base.logic.repository.base.RepositoryCache
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.features.send.Contact
import org.tokend.template.features.send.ContactsManager

class ContactsRepository(val context: Context) : SimpleMultipleItemsRepository<Contact>() {

    override val itemsCache: RepositoryCache<Contact> = ContactsRepositoryCache()

    override fun getItems(): Single<List<Contact>> {
        return ContactsManager.getContacts(context)
                .toSingle()
    }
}