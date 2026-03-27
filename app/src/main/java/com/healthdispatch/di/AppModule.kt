package com.healthdispatch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthdispatch.data.auth.AuthRepository
import com.healthdispatch.data.auth.FirebaseAuthRepository
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.SyncDatabase
import com.healthdispatch.data.local.SyncTokenDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        return firestore
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository {
        return FirebaseAuthRepository(firebaseAuth)
    }
}
