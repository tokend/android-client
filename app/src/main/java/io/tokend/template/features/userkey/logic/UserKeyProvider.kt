package io.tokend.template.features.userkey.logic

import io.reactivex.Maybe

interface UserKeyProvider {
    fun getUserKey(isRetry: Boolean): Maybe<CharArray>
}