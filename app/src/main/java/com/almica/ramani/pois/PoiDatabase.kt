package com.almica.ramani.pois

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

private const val logtag = "PoiDatabase"
@Database(entities = [PoiEntity::class], version = 2)
@TypeConverters(PoiTypeConverters::class)
abstract class PoiDatabase : RoomDatabase() {
    abstract fun poiDao(): PoiDao

    companion object {
        val DB_NAME = "poi-database.db"

        @Volatile private var INSTANCE: PoiDatabase? = null

        fun getInstance(context: Context): PoiDatabase {
            //Timber.i("DB_NAME: $DB_NAME")
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                //INSTANCE ?: buildDatabaseFromAssets(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PoiDatabase {
            Timber.i("DB_NAME: $DB_NAME")
            return Room.databaseBuilder(
                context,
                PoiDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        private fun buildDatabaseFromAssets(context: Context): PoiDatabase {
            Timber.i("DB_NAME: $DB_NAME")
            return Room.databaseBuilder(
                context,
                PoiDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .createFromAsset("databases/poi-database.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
    }
}