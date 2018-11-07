package org.tokend.template.features.send

data class Contact(val id: String,
                   val name: String,
                   val emails: List<String>,
                   val photo_uri: String?)