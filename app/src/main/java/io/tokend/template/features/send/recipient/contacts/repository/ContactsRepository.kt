package io.tokend.template.features.send.recipient.contacts.repository

import android.content.Context
import android.provider.ContactsContract
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.features.send.recipient.contacts.model.ContactRecord

class ContactsRepository(
    val context: Context,
    itemsCache: RepositoryCache<ContactRecord>
) : MultipleItemsRepository<ContactRecord>(itemsCache) {

    override fun getItems(): Single<List<ContactRecord>> {
        val contacts = mutableListOf<ContactRecord>()
        val credentialsByIds = mutableMapOf<String, MutableSet<String>>()

        val contentResolver = context.contentResolver

        return Single.defer {
            val credentialsCursor = contentResolver.query(
                EMAILS_CONTENT_URI,
                null, null, null, null
            )

            if (credentialsCursor != null && credentialsCursor.moveToFirst()) {
                do {
                    val id = credentialsCursor.getString(
                        credentialsCursor.getColumnIndex(EMAIL_CONTACT_ID)
                    )
                    val email = credentialsCursor.getString(credentialsCursor.getColumnIndex(EMAIL))
                    credentialsByIds.getOrPut(id, ::mutableSetOf).add(email)
                } while (credentialsCursor.moveToNext())
                credentialsCursor.close()
            }

            val dataCursor = contentResolver.query(
                CONTACTS_CONTENT_URI,
                null, null, null, null
            )
            if (dataCursor != null && dataCursor.moveToFirst()) {
                do {
                    val id = dataCursor.getString(dataCursor.getColumnIndex(CONTACT_ID))

                    if (credentialsByIds.containsKey(id)) {
                        val photoUri = dataCursor.getString(dataCursor.getColumnIndex(PHOTO))
                        val name = dataCursor.getString(dataCursor.getColumnIndex(NAME))

                        contacts.add(
                            ContactRecord(
                                id = id,
                                name = name,
                                photoUri = photoUri,
                                credentials = credentialsByIds.getValue(id).toList()
                            )
                        )
                    }
                } while (dataCursor.moveToNext())
                dataCursor.close()
            }

            Single.just<List<ContactRecord>>(contacts)
        }.subscribeOn(Schedulers.newThread())
    }

    private companion object {
        private val CONTACTS_CONTENT_URI = ContactsContract.Contacts.CONTENT_URI
        private val EMAILS_CONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        private const val EMAIL_CONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID
        private const val CONTACT_ID = ContactsContract.Contacts._ID

        private const val NAME = ContactsContract.Contacts.DISPLAY_NAME
        private const val EMAIL = ContactsContract.CommonDataKinds.Email.DATA
        private const val PHOTO = ContactsContract.CommonDataKinds.Photo.PHOTO_URI
    }
}