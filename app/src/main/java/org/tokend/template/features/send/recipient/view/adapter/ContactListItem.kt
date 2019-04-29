package org.tokend.template.features.send.recipient.view.adapter

import org.tokend.template.features.send.recipient.model.Contact
import org.tokend.template.features.send.recipient.model.ContactEmail

class ContactListItem(
        val id: String,
        val name: String,
        val emails: List<ContactEmail>,
        val photoUri: String?,
        var isExpanded: Boolean
) {
    constructor(source: Contact): this(
            id = source.id,
            name = source.name,
            emails = source.emails,
            photoUri = source.photoUri,
            isExpanded = false
    )
}