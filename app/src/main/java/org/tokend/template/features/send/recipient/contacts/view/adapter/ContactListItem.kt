package org.tokend.template.features.send.recipient.contacts.view.adapter

import org.tokend.template.features.send.recipient.contacts.model.ContactRecord

interface ContactListItem

interface CredentialedContactListItem: ContactListItem {
    val credential: String
}

class ContactMainListItem(
        val id: String,
        val name: String,
        override val credential: String,
        val photoUri: String?
) : CredentialedContactListItem {
    constructor(source: ContactRecord) : this(
            id = source.id,
            name = source.name,
            credential = source.credentials.first(),
            photoUri = source.photoUri
    )

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ContactMainListItem && other.id == this.id
    }
}

class ContactExtraCredentialListItem(
        override val credential: String
) : CredentialedContactListItem {
    override fun hashCode(): Int {
        return credential.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ContactExtraCredentialListItem && other.credential == this.credential
    }
}

class ContactSectionTitleListItem(
        val title: String
): ContactListItem {
    override fun hashCode(): Int {
        return title.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ContactSectionTitleListItem && other.title == this.title
    }
}