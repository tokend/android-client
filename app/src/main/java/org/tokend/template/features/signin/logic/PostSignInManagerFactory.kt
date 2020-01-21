package org.tokend.template.features.signin.logic

import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.util.ConnectionStateUtil

class PostSignInManagerFactory(
        private val repositoryProvider: RepositoryProvider,
        private val connectionStateUtil: ConnectionStateUtil
) {
    fun get(): PostSignInManager {
        return PostSignInManager(repositoryProvider, connectionStateUtil::isOnline)
    }
}