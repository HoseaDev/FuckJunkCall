package com.hoseadev.fuckjunkcall.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hoseadev.fuckjunkcall.data.model.BlockedCall
import com.hoseadev.fuckjunkcall.data.model.WhitelistNumber

/**
 * 应用数据库
 */
@Database(
    entities = [BlockedCall::class, WhitelistNumber::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockedCallDao(): BlockedCallDao
    abstract fun whitelistDao(): WhitelistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuck_junk_call_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
