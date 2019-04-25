package org.tokend.template.features.send.logic

import android.content.Context
import android.provider.ContactsContract
import io.reactivex.Single
import org.tokend.template.features.send.recipient.model.Contact
import org.tokend.template.features.send.recipient.model.ContactEmail

class ContactsManager {
    companion object {
        private val CONTACTS_CONTENT_URI = ContactsContract.Contacts.CONTENT_URI
        private val EMAILS_CONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        private const val EMAIL_CONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID
        private const val CONTACT_ID = ContactsContract.Contacts._ID

        private const val NAME = ContactsContract.Contacts.DISPLAY_NAME
        private const val EMAIL = ContactsContract.CommonDataKinds.Email.DATA
        private const val PHOTO = ContactsContract.CommonDataKinds.Photo.PHOTO_URI

        @JvmStatic
        fun getContacts(context: Context): Single<List<Contact>> {

            return Single.create {
                val contacts = arrayListOf<Contact>()
                val emails = arrayListOf<Pair<String, String>>()
                val ids = arrayListOf<String>()

                val cr = context.contentResolver

                val cursor = cr.query(EMAILS_CONTENT_URI, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val id = cursor.getString(cursor.getColumnIndex(EMAIL_CONTACT_ID))
                        val email = cursor.getString(cursor.getColumnIndex(EMAIL))
                        ids.add(id)
                        emails.add(id to email)
                    } while (cursor.moveToNext())
                    cursor.close()
                }

                val contactsCursor = cr.query(CONTACTS_CONTENT_URI, null, null, null, null)
                if (contactsCursor != null && contactsCursor.moveToFirst()) {
                    do {
                        val id = contactsCursor.getString(contactsCursor.getColumnIndex(CONTACT_ID))

                        if (ids.contains(id)) {
                            val photoUri = contactsCursor.getString(contactsCursor.getColumnIndex(PHOTO))
                            val name = contactsCursor.getString(contactsCursor.getColumnIndex(NAME))
                            val emailsById = arrayListOf<ContactEmail>()
                            emails.filter { pair -> pair.first == id }.forEach { pair ->
                                emailsById.add(ContactEmail(pair.first, pair.second))
                            }

                            contacts.add(Contact(id, name, emailsById, photoUri))
                        }
                    } while (contactsCursor.moveToNext())
                    contactsCursor.close()
                }
                contacts.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name, true) })
                it.onSuccess(contacts)
            }
        }
    }
}
