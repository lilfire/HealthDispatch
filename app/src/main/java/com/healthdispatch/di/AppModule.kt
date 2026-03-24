package com.healthdispatch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.healthdispatch.data.cloud.CloudConfig
import com.healthdispatch.data.cloud.SupabaseClient
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.SyncDatabase
import com.healthdispatch.data.local.SyncTokenDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SyncDatabase {
        return Room.databaseBuilder(
            context,
            SyncDatabase::class.java,
            "health_sync.db"
        ).build()
    }

    @Provides
    fun providePendingSyncDao(db: SyncDatabase): PendingSyncDao = db.pendingSyncDao()

    @Provides
    fun provideSyncTokenDao(db: SyncDatabase): SyncTokenDao = db.syncTokenDao()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @Provides
    @Singleton
    fun provideCloudConfig(): CloudConfig {
        // TODO: Read from DataStore preferences (setup wizard writes these)
        return CloudConfig(
            url = "",
            apiKey = ""
        )
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
