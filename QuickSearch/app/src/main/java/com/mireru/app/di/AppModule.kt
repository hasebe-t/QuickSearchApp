package com.mireru.app.di

import android.content.Context
import com.mireru.app.data.HistoryDao
import com.mireru.app.data.HistoryDatabase
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
    fun provideDatabase(@ApplicationContext ctx: Context): HistoryDatabase =
        HistoryDatabase.getInstance(ctx)

    @Provides
    @Singleton
    fun provideHistoryDao(db: HistoryDatabase): HistoryDao = db.historyDao()

    // GeminiRepository, SettingsDataStore, OcrHelper, MlKitHelper, ImageCropper は
    // @Singleton + @Inject constructor で自動提供されるため個別登録不要
}
