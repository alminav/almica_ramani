package com.almica.ramani.routes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.almica.ramani.Const
import timber.log.Timber

@Database(entities = [RouteEntity::class], version = 3)
@TypeConverters(RouteTypeConverters::class)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        val DB_NAME = "route-database.db"

        @Volatile private var INSTANCE: RouteDatabase? = null

        fun getInstance(context: Context): RouteDatabase {
            return INSTANCE ?: synchronized(this) {
                //INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                INSTANCE ?: buildDatabaseFromAssets(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): RouteDatabase {
            Timber.i("${Const.TAG_ROUTE_DAO} $DB_NAME")
            return Room.databaseBuilder(
                context,
                RouteDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }
        // experimental 27dez2024
        private fun buildDatabaseFromAssets(context: Context): RouteDatabase {
            Timber.i("${Const.TAG_ROUTE_DAO} $DB_NAME")
            return Room.databaseBuilder(
                context,
                RouteDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .createFromAsset("databases/route-database.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(routes)")
                val existingColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        existingColumns.add(cursor.getString(nameIndex))
                    }
                }
                cursor.close()

                if (!existingColumns.contains("latitudeStart")) {
                    db.execSQL("ALTER TABLE routes ADD COLUMN latitudeStart REAL NOT NULL DEFAULT 0.0")
                }
                if (!existingColumns.contains("longitudeStart")) {
                    db.execSQL("ALTER TABLE routes ADD COLUMN longitudeStart REAL NOT NULL DEFAULT 0.0")
                }
                if (!existingColumns.contains("latitudeCenter")) {
                    db.execSQL("ALTER TABLE routes ADD COLUMN latitudeCenter REAL NOT NULL DEFAULT 0.0")
                }
                if (!existingColumns.contains("longitudeCenter")) {
                    db.execSQL("ALTER TABLE routes ADD COLUMN longitudeCenter REAL NOT NULL DEFAULT 0.0")
                }
                if (!existingColumns.contains("latitudeStop")) {
                    db.execSQL("ALTER TABLE routes ADD COLUMN latitudeStop REAL NOT NULL DEFAULT 0.0")
                }
                if (!existingColumns.contains("longitudeStop")) {
                    db.execSQL("ALTER TABLE routes ADD COLUMN longitudeStop REAL NOT NULL DEFAULT 0.0")
                }
            }
        }
    }
}