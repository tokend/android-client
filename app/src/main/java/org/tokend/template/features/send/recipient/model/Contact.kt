package org.tokend.template.features.send.recipient.model

data class Contact(val id: String,
                   val name: String,
                   val emails: List<ContactEmail>,
                   val photoUri: String?)