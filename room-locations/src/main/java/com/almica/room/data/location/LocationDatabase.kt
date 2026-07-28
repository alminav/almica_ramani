package com.almica.room.data.location

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocationEntity::class], version = 3)
@TypeConverters(LocationTypeConverters::class)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        private val DB_NAME = "location-tracking-database"

        @Volatile private var INSTANCE: LocationDatabase? = null

        fun getInstance(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LocationDatabase {
            return Room.databaseBuilder(
                context,
                LocationDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE location ADD COLUMN latitudeStart REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE location ADD COLUMN longitudeStart REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // If the columns were already added in a "dirty" version 2, 
                // we might need to handle it, but usually, we just ensure they exist.
                // For safety with destructive migration fallback, we can leave this empty 
                // or add the columns if they are missing.
                try {
                    db.execSQL("ALTER TABLE location ADD COLUMN latitudeStart REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) { }
                try {
                    db.execSQL("ALTER TABLE location ADD COLUMN longitudeStart REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) { }
            }
        }
    }
}