package com.almica.ramani.geojsonMaps

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber
import java.io.File

private const val logtag = "GeojsonMapDatabase"
@Database(entities = [GeojsonMapEntity::class], version = 3)
@TypeConverters(GeojsonMapTypeConverters::class)
abstract class GeojsonMapDatabase : RoomDatabase() {
    abstract fun geojsonMapDao(): GeojsonMapDao

    companion object {
        const val DB_NAME = "geojson-map-database.db"

        @Volatile private var INSTANCE: GeojsonMapDatabase? = null

        fun getInstance(context: Context): GeojsonMapDatabase {
            //Timber.i( "DB_NAME: $DB_NAME")
            val geojsonMapDatabase = INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                //INSTANCE ?: buildDatabaseFromAssets(context).also { INSTANCE = it }
            }
            //Timber.i("geojsonMapDatabase: $geojsonMapDatabase")
            return geojsonMapDatabase
        }

        fun newInstance(context: Context, file: File): GeojsonMapDatabase {
            //Timber.i( "DB_NAME: $DB_NAME")
            val geojsonMapDatabase = buildDatabaseFromFile(context, file)
                //INSTANCE ?: buildDatabaseFromAssets(context).also { INSTANCE = it }
            //Timber.i("geojsonMapDatabase: $geojsonMapDatabase")
            return geojsonMapDatabase
        }

        private fun buildDatabase(context: Context): GeojsonMapDatabase {
            Timber.i( "DB_NAME: $DB_NAME")
            return Room.databaseBuilder(
                context,
                GeojsonMapDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        private fun buildDatabaseFromAssets(context: Context): GeojsonMapDatabase {
            Timber.i( "DB_NAME: $DB_NAME")
            return Room.databaseBuilder(
                context,
                GeojsonMapDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .createFromAsset("databases/geojson-map-database.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
        }

        // Migration path definition from version 1 to version 2.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Timber.i( "" +
                        "db.isDatabaseIntegrityOk ${db.isDatabaseIntegrityOk}")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        /**
         * Google doc
         * Hinweis: In-Memory Room-Datenbanken unterstützen das Vorabfüllen der Datenbank nicht. mit createFromAsset() oder createFromFile().
         * room database difference between inMemoryDatabaseBuilder and databaseBuilder
         * inMemoryDatabaseBuilder(): The database will be created in system memory, If you kill the app (Killing your process), database will be removed and data will not be persisted. This can be used while Testing.
         * databaseBuilder() : The database will be created in /data/data/com.your.app and will be persisted. This you will be using it in production.
         *
         * 31dez25
         * stackOverflow: createFromFile( ) doesn't populate database - Room
         */
        private fun buildDatabaseFromFile(context: Context, file: File): GeojsonMapDatabase {
            Timber.i( "buildDatabaseFromFile file: ${file.path}")
            val databaseBuilder = Room.databaseBuilder(
                context,
                GeojsonMapDatabase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                //.createFromAsset("databases/geojson-map-database.db")
                .setJournalMode(JournalMode.TRUNCATE)
                .createFromFile(file)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Timber.i( "" +
                                "db.isDatabaseIntegrityOk ${db.isDatabaseIntegrityOk}")
                    }
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        Timber.i( "db.isDatabaseIntegrityOk: ${db.isDatabaseIntegrityOk}")
                        Timber.i("path: ${db.path}")
                    }
                })
                .build()
            return databaseBuilder
        }
    }
}