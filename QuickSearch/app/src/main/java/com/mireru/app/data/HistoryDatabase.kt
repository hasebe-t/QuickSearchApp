package com.mireru.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mireru.app.model.HistoryItem

@Database(entities = [HistoryItem::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "mireru_history.db"
                ).build().also { INSTANCE = it }
            }
    }
}
