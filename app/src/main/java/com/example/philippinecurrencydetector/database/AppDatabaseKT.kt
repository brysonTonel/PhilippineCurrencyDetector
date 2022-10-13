package com.example.philippinecurrencydetector.database

import android.content.Context
import android.os.Environment
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.philippinecurrencydetector.database.dao.CurrencyDao
import com.example.philippinecurrencydetector.database.model.CurrencyModel


@Database(entities = [CurrencyModel::class], version = 1)

abstract class AppDatabaseKT : RoomDatabase() {

    abstract fun currencyDao(): CurrencyDao?

    companion object {
        private val DATABASE_PATH = Environment.getExternalStorageDirectory().absolutePath
        private const val DATA_PATH = "/data/" + "currency"

        @Volatile
        private var INSTANCE: AppDatabaseKT? = null

        @JvmStatic
        fun getInstance(context: Context): AppDatabaseKT = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }


        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            AppDatabaseKT::class.java,
            DATABASE_PATH + DATA_PATH + "data.db"
        ).fallbackToDestructiveMigration()

            .build()
    }
}