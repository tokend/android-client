package org.tokend.template.di.providers

import dagger.Module
import dagger.Provides
import org.tokend.template.logic.Session
import javax.inject.Singleton

@Module
class SessionModule(private val session: Session) {
    @Provides
    @Singleton
    fun provideSession(): Session {
        return session
    }
}