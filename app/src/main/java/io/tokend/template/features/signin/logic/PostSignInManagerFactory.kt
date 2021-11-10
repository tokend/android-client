package io.tokend.template.features.signin.logic

import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.util.ConnectionStateUtil

class PostSignInManagerFactory(
    private val repositoryProvider: RepositoryProvider,
    private val connectionStateUtil: ConnectionStateUtil
) {
    fun get(): PostSignInManager {
        return PostSignInManager(repositoryProvider, connectionStateUtil::isOnline)
    }
}