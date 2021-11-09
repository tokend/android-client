package io.tokend.template.features.polls.repository

import io.tokend.template.data.storage.repository.MemoryOnlyRepositoryCache
import io.tokend.template.features.polls.model.PollRecord

class PollsCache : MemoryOnlyRepositoryCache<PollRecord>() {
    override fun sortItems() {
        mItems.sortByDescending { it.id.toLongOrNull() }
    }
}