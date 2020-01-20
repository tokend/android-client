package org.tokend.template.di

import dagger.Module
import dagger.Provides
import org.tokend.template.db.AppDatabase
import javax.inject.Singleton

@Module
class AppDatabaseModule(private val database: AppDatabase) {
    @Provides
    @Singleton
    fun database(): AppDatabase = database
}