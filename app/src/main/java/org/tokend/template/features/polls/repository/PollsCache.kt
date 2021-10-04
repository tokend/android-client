package org.tokend.template.features.polls.repository

import org.tokend.template.data.storage.repository.MemoryOnlyRepositoryCache
import org.tokend.template.features.polls.model.PollRecord

class PollsCache : MemoryOnlyRepositoryCache<PollRecord>() {
    override fun sortItems() {
        mItems.sortByDescending { it.id.toLongOrNull() }
    }
}