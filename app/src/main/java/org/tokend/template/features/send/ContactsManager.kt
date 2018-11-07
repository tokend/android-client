package org.tokend.template.features.send

import android.content.Context
import android.provider.ContactsContract

class ContactsManager {

    companion object {

        private val CONTACTS_CONTENT_URI = ContactsContract.Contacts.CONTENT_URI
        private val EMAILS_CONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        private const val ID = ContactsContract.Contacts._ID

        private const val EMAIL_CONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID
        private const val NAME = ContactsContract.Contacts.DISPLAY_NAME
        private const val EMAIL = ContactsContract.CommonDataKinds.Email.DATA
        private const val PHOTO = ContactsContract.CommonDataKinds.Photo.PHOTO_URI

        @JvmStatic
        fun getContacts(context: Context): List<Contact> {

                val contacts = arrayListOf<Contact>()
                val cr = context.contentResolver
                val cursor = cr.query(CONTACTS_CONTENT_URI, null, null, null, null)

                if(cursor != null && cursor.moveToFirst()) {
                    do {
                        val id = cursor.getString(cursor.getColumnIndex(ID))
                        val name = cursor.getString(cursor.getColumnIndex(NAME))

                        val emails = arrayListOf<String>()
                        val emailsCursor = cr.query(EMAILS_CONTENT_URI, null, "$EMAIL_CONTACT_ID = ?",
                                arrayOf(id), null)

                        if(emailsCursor != null && emailsCursor.moveToFirst()) {
                            do {
                                val email = emailsCursor.getString(emailsCursor.getColumnIndex(EMAIL))
                                if(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    emails.add(email)
                                }
                            } while (emailsCursor.moveToNext())
                            emailsCursor.close()
                        }

                        val photoUri = cursor.getString(cursor.getColumnIndex(PHOTO))

                        if(emails.isNotEmpty()) {
                            contacts.add(Contact(id, name, emails, photoUri))
                        }

                    } while (cursor.moveToNext())
                    cursor.close()
                }
            return contacts
        }
    }
}
