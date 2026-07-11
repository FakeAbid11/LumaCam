package com.lumacam.app.di

import android.content.Context
import androidx.room.Room
import com.lumacam.app.data.CaptureDao
import com.lumacam.app.data.LumaDatabase
import com.lumacam.app.data.SettingsRepository
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
}
