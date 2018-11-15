package org.tokend.template.features.send.model

data class Contact(val id: String,
                   val name: String,
                   val emails: List<ContactEmail>,
                   val photo_uri: String?)