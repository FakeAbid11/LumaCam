package com.lumacam.app.di

import android.content.Context
import androidx.room.Room
import com.lumacam.app.data.CaptureDao
import com.lumacam.app.data.CloudAiKeyStore
import com.lumacam.app.data.DeviceBenchmarkStore
import com.lumacam.app.data.DeviceCapabilityProbe
import com.lumacam.app.data.LocalModelDownloader
import com.lumacam.app.data.LocalModelRepository
import com.lumacam.app.data.LocalModelStorage
import com.lumacam.app.data.LumaDatabase
import com.lumacam.app.data.SettingsRepository
import com.lumacam.feature.ai.CompositionAnalyzer
import com.lumacam.feature.ai.vision.LumaVisionAnalyzer
import com.lumacam.feature.ai.vision.LumaVisionCompositionAnalyzer
import com.lumacam.feature.ai.cloud.CloudAiProviderFactory
import com.lumacam.feature.ai.local.DefaultLocalAiProvider
import com.lumacam.feature.ai.local.LocalAiProvider
import com.lumacam.feature.ai.local.PlaceholderLocalInferenceEngine
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

    // Real Luma Vision pipeline (PRD §4 Tier 2). The analyzer is created with the
    // on-device ML Kit + accelerometer stack and kept alive for the app's lifetime;
    // its sensor listener is started here so tilt-based horizon is always current.
    @Provides
    @Singleton
    fun provideCompositionAnalyzer(@ApplicationContext context: Context): CompositionAnalyzer {
        val vision = LumaVisionAnalyzer.create(context).also { it.start() }
        return LumaVisionCompositionAnalyzer(vision)
    }

    // Cloud AI (PRD §4 Tier 4). Provided in isolation here; wired into the "✨"
    // analysis selection later (Prompt 10 — Smart AI Engine).
    @Provides
    @Singleton
    fun provideCloudAiKeyStore(@ApplicationContext context: Context): CloudAiKeyStore =
        CloudAiKeyStore(context)

    @Provides
    @Singleton
    fun provideCloudAiProviderFactory(): CloudAiProviderFactory = CloudAiProviderFactory()

    // Local AI Model (PRD §4 Tier 3). On-device model download/selection/storage.
    // Provided in isolation here; wired into the "✨" analysis selection later
    // (Prompt 10 — Smart AI Engine).
    @Provides
    @Singleton
    fun provideLocalModelStorage(@ApplicationContext context: Context): LocalModelStorage =
        LocalModelStorage(context)

    @Provides
    @Singleton
    fun provideLocalModelRepository(
        @ApplicationContext context: Context,
        storage: LocalModelStorage
    ): LocalModelRepository = LocalModelRepository(context, storage)

    @Provides
    @Singleton
    fun provideLocalModelDownloader(storage: LocalModelStorage): LocalModelDownloader =
        LocalModelDownloader(storage)

    // Uses the placeholder inference engine until a real native GGUF runtime is
    // bundled (a deliberately isolated step — see LocalInferenceEngine docs).
    @Provides
    @Singleton
    fun provideLocalAiProvider(repository: LocalModelRepository): LocalAiProvider =
        DefaultLocalAiProvider(
            engine = PlaceholderLocalInferenceEngine(),
            activeModel = { repository.activeModel() }
        )

    // Device AI Compatibility Benchmark (PRD §4). Reads specs and classifies the
    // device tier; the result is reused by the Local AI Model manager and, later,
    // the Smart AI Engine (Prompt 10).
    @Provides
    @Singleton
    fun provideDeviceCapabilityProbe(@ApplicationContext context: Context): DeviceCapabilityProbe =
        DeviceCapabilityProbe(context)

    @Provides
    @Singleton
    fun provideDeviceBenchmarkStore(@ApplicationContext context: Context): DeviceBenchmarkStore =
        DeviceBenchmarkStore(context)
}
