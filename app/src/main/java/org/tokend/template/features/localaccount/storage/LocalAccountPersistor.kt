package org.tokend.template.features.localaccount.storage

import org.tokend.template.features.localaccount.model.LocalAccount

interface LocalAccountPersistor {
    fun load(): LocalAccount?
    fun save(localAccount: LocalAccount): LocalAccount
}