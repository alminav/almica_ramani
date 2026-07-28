package com.almica.ramani.locations

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import com.almica.room.data.location.LocationDatabase
import com.almica.room.data.location.LocationEntity
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ExecutorService

private const val logtag = "LocationRepository"
class LocationRepository private constructor(
    private val locationDatabase: LocationDatabase,
    //private val locationManager: BackgroundLocationManager,
    private val executor: ExecutorService
) {
    //val myLog = MyLog(true)
    // Database related fields/methods:
    val locationDao = locationDatabase.locationDao()
    /**
     * Returns last location for time
     */
    fun getLocationForTime(time: Long): List<LocationEntity> = locationDao.getLocationForTime(time)
    /**
     * Returns first recorded location
     */
    fun getFirstLocation(): List<LocationEntity> = locationDao.getFirstLocation()
    /**
     * Returns last recorded location
     */
    fun getLastLocation(): List<LocationEntity> = locationDao.getLastLocation()
    /**
     * Returns last recorded location LiveData
     */
    fun getLastLocationLive(): LiveData<List<LocationEntity>> = locationDao.getLastLocationLive()

    /**
     * Returns all recorded locations from database descending.
     */
    fun getLocationsLiveDataDesc(): LiveData<List<LocationEntity>> = locationDao.getLocationsLiveDataDesc()

    /**
     * Returns all recorded locations from database ascending, LiveData
     */
    fun getLocationsLiveDataAsc(): LiveData<List<LocationEntity>> = locationDao.getLocationsLiveDataAsc()

    /**
     * Returns all recorded locations from database ascending
     */
    fun getLocationsAsc(): List<LocationEntity> = locationDao.getLocationsAsc()
    /**
     * Returns all recorded locations from database descending
     */
    fun getLocationsDesc(): List<LocationEntity> = locationDao.getLocationsDesc()

    fun getLocationsCount(): Int = locationDao.getLocationsCount()
    /**
     * Returns all recorded locations after time from database ascending
     */
    fun getLocationsAscFromTime(time: Long) = locationDao.getLocationsAscFromTime(time)

    /**
     * Removes all recorded locations from time upwards.
     */
    fun removeLocationsFromTime(time: Long) = locationDao.removeLocationsFromTime(time)

    /**
     * Removes all recorded locations from time downwards.
     */
    fun removeLocationsToTime(time: Long) = locationDao.removeLocationsToTime(time)

    /**
     * Removes all recorded locations from time downwards.
     */
    fun removeAllLocations() = locationDao.removeAllLocations()

    /**
     * Updates all location in database, adjust distance
     */
    fun updateLocations(dist: Double) {
        executor.execute {
            locationDao.updateLocations(dist)
        }
    }
    // Not being used now but could in future versions.
    /**
     * Returns specific location in database.
     */
    fun getLocation(id: UUID): LiveData<LocationEntity> = locationDao.getLocation(id)

    // Not being used now but could in future versions.
    /**
     * Updates location in database.
     */
    fun updateLocation(locationEntity: LocationEntity) {
        executor.execute {
            locationDao.updateLocation(locationEntity)
        }
    }

    /**
     * Adds location to the database.
     */
    fun addLocation(locationEntity: LocationEntity) {
        executor.execute {
            //Timber.i( "${locationEntity.latitude} ${locationEntity.longitude}")
            locationDao.addLocation(locationEntity)
        }
    }

    /**
     * Adds list of locations to the database.
     */
    fun addLocations(myLocationEntities: List<LocationEntity>) {
        executor.execute {
            locationDao.addLocations(myLocationEntities)
        }
    }

    companion object {
        @Volatile private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(
                    LocationDatabase.getInstance(context),
                    executor)
                    .also { INSTANCE = it }
            }
        }
    }
}