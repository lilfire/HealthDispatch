package com.healthdispatch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.healthdispatch.BuildConfig
import com.healthdispatch.data.auth.AuthClient
import com.healthdispatch.data.auth.AuthRepository
import com.healthdispatch.data.auth.SupabaseAuthClient
import com.healthdispatch.data.auth.SupabaseAuthRepository
import com.healthdispatch.data.cloud.AuthSessionProvider
import com.healthdispatch.data.cloud.PostgrestClientWrapper
import com.healthdispatch.data.cloud.SupabaseAuthSessionProvider
import com.healthdispatch.data.cloud.SupabasePostgrestClientWrapper
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.SyncDatabase
import com.healthdispatch.data.local.SyncTokenDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
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
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        httpClient: HttpClient,
        dataStore: DataStore<Preferences>,
        json: Json
    ): AuthRepository {
        return SupabaseAuthRepository(
            httpClient = httpClient,
            dataStore = dataStore,
            json = json,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseApiKey = BuildConfig.SUPABASE_ANON_KEY
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAuthSessionProvider(impl: SupabaseAuthSessionProvider): AuthSessionProvider

    @Binds
    @Singleton
    abstract fun bindPostgrestClientWrapper(impl: SupabasePostgrestClientWrapper): PostgrestClientWrapper

    @Binds
    @Singleton
    abstract fun bindAuthClient(impl: SupabaseAuthClient): AuthClient
}
