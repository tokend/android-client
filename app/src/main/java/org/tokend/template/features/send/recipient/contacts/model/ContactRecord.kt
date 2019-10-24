package org.tokend.template.features.send.recipient.contacts.model

class ContactRecord(
        val id: String,
        val name: String,
        val credentials: List<String>,
        val photoUri: String?
) {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ContactRecord && other.id == this.id
    }
}