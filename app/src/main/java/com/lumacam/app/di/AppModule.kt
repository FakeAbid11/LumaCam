package com.lumacam.app.di

import android.content.Context
import androidx.room.Room
import com.lumacam.app.data.CaptureDao
import com.lumacam.app.data.CloudAiKeyStore
import com.lumacam.app.data.LumaDatabase
import com.lumacam.app.data.SettingsRepository
import com.lumacam.feature.ai.CompositionAnalyzer
import com.lumacam.feature.ai.MockCompositionAnalyzer
import com.lumacam.feature.ai.cloud.CloudAiProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLumaDatabase(@ApplicationContext context: Context): LumaDatabase =
        Room.databaseBuilder(context, LumaDatabase::class.java, "luma.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCaptureDao(db: LumaDatabase): CaptureDao = db.captureDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    // Prompt 5: mock AI source. Prompt 6/7 swaps this binding for the real analyzer.
    @Provides
    @Singleton
    fun provideCompositionAnalyzer(): CompositionAnalyzer = MockCompositionAnalyzer()

    // Cloud AI (PRD §4 Tier 4). Provided in isolation here; wired into the "✨"
    // analysis selection later (Prompt 10 — Smart AI Engine).
    @Provides
    @Singleton
    fun provideCloudAiKeyStore(@ApplicationContext context: Context): CloudAiKeyStore =
        CloudAiKeyStore(context)

    @Provides
    @Singleton
    fun provideCloudAiProviderFactory(): CloudAiProviderFactory = CloudAiProviderFactory()
}
