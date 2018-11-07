package org.tokend.template.base.logic.repository.contacts

import io.reactivex.Single
import org.tokend.template.base.logic.repository.base.RepositoryCache
import org.tokend.template.features.send.Contact

class ContactsRepositoryCache : RepositoryCache<Contact>() {
    override fun isContentSame(first: Contact, second: Contact): Boolean {
        return first.id == second.id
    }

    override fun getAllFromDb(): Single<List<Contact>> = Single.just(emptyList())

    override fun addToDb(items: List<Contact>) { }

    override fun updateInDb(items: List<Contact>) { }

    override fun deleteFromDb(items: List<Contact>) { }

    override fun clearDb() { }
}