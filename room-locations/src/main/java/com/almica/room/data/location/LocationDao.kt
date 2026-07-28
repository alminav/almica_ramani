package com.almica.room.data.location

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.util.*

@Dao
interface LocationDao {
    @Query("SELECT * FROM location where time = :queryTime")
    fun getLocationForTime(queryTime: Long): List<LocationEntity>

    @Query("SELECT * FROM location ORDER BY time ASC LIMIT 1")
    fun getFirstLocation(): List<LocationEntity>

    @Query("SELECT * FROM location ORDER BY time DESC LIMIT 1")
    fun getLastLocation(): List<LocationEntity>

    @Query("SELECT * FROM location ORDER BY time DESC LIMIT 1")
    fun getLastLocationLive(): LiveData<List<LocationEntity>>

    @Query("SELECT * FROM location ORDER BY time DESC")
    fun getLocationsLiveDataDesc(): LiveData<List<LocationEntity>>

    @Query("SELECT * FROM location ORDER BY time ASC")
    fun getLocationsLiveDataAsc(): LiveData<List<LocationEntity>>

    @Query("SELECT * FROM location ORDER BY time ASC")
    fun getLocationsAsc(): List<LocationEntity>

    @Query("SELECT * FROM location ORDER BY time DESC")
    fun getLocationsDesc(): List<LocationEntity>

    @Query("SELECT Count(*) FROM location")
    fun getLocationsCount(): Int

    @Query("SELECT * FROM location WHERE time > :time ORDER BY time ASC")
    fun getLocationsAscFromTime(time: Long): List<LocationEntity>

    @Query("SELECT * FROM location WHERE id=(:id)")
    fun getLocation(id: UUID): LiveData<LocationEntity>

    @Query("SELECT * FROM location WHERE time > :time")
    fun getLocationsLiveDataFromTime(time: Long): LiveData<List<LocationEntity>>

    @Query("DELETE FROM location WHERE time > :time")
    fun removeLocationsFromTime(time: Long)

    @Query("DELETE FROM location WHERE time < :time")
    fun removeLocationsToTime(time: Long)

    @Query("DELETE FROM location")
    fun removeAllLocations()

    @Query("UPDATE location SET distanceM = distanceM - :dist")
    fun updateLocations(dist: Double)

    @Update
    fun updateLocation(locationEntity: LocationEntity)

    @Insert
    fun addLocation(locationEntity: LocationEntity)

    @Insert
    fun addLocations(locationEntities: List<LocationEntity>)
}