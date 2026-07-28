package com.almica.ramani.geojsonMaps

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.UUID

@Dao
interface GeojsonMapDao
{
    @Query("select * from geojsonMaps where z = :zoom")
    fun searchWithZoom(zoom: Int) : List<GeojsonMapEntity>
    @Query("UPDATE geojsonMaps SET bitmap = :bitmap WHERE id = :uuid")
    fun updateGeojsonMap(bitmap: Bitmap, uuid: UUID)
    @Query("UPDATE geojsonMaps SET bitmap = :bitmap WHERE name = :name")
    fun updateGeojsonMapByName(bitmap: Bitmap, name: String)
    @Query("UPDATE geojsonMaps SET enabled = :status WHERE name = :name")
    fun updateGeojsonMapStatus(status: Boolean, name: String)
    @Query("UPDATE geojsonMaps SET enabled = :status, path = :region WHERE name = :name")
    fun updateGeojsonMapStatus(status: Boolean, region: String, name: String)

    @Query("UPDATE geojsonMaps SET data = :data, lastModifiedTime = :time WHERE name = :name")
    fun updateGeojsonMapData(data: ByteArray, name: String, time: Long)

    //UPDATE `users` SET `authorised` = NOT `authorised` WHERE id = 2
    @Query("UPDATE geojsonMaps SET enabled = NOT enabled WHERE name = :name")
    fun toggleGeojsonMapStatus(name: String)
    @Query("select enabled from geojsonMaps WHERE name = :name")
    fun getGeojsonMapStatus(name: String) : Boolean

    @Insert
    fun insertGeojsonMap(geojsonMapEntity: GeojsonMapEntity)
    @Insert
    fun insertGeojsonMaps(myGeojsonMapEntities: List<GeojsonMapEntity>)

    @Query("delete from geojsonMaps WHERE id = :id")
    fun removeGeojsonMap(id: UUID)

    @Query("delete from geojsonMaps")
    fun removeAll()

    @Query("delete from geojsonMaps WHERE name = :name")
    fun removeGeojsonMapByName(name: String)
    @Query("delete from geojsonMaps WHERE x = :x AND y = :y AND z = :z")
    fun removeGeojsonMapByXYZ(x: Int, y:Int, z:Int)
    @Query("delete from geojsonMaps WHERE z = :zoom")
    fun removeGeojsonMapsByZoom(zoom: Int)
    @Query("delete from geojsonMaps WHERE path = :region")
    fun removeGeojsonMapsByRegion(region: String)

    @Query("select * from geojsonMaps where id=(:id)")
    fun getGeojsonMap(id: UUID): LiveData<GeojsonMapEntity>

    @Query("select * from geojsonMaps where id=(:id)")
    fun getGeojsonMapSimple(id: UUID): GeojsonMapEntity

    @Query("select * from geojsonMaps where name=(:name) LIMIT 1")
    fun getGeojsonMapSimpleByName(name: String): GeojsonMapEntity?

    @Query("select * from geojsonMaps")
    fun getAll() : LiveData<List<GeojsonMapEntity>>

    //@Query("select name, north, south, west, east, bitmap, enabled from geojsonMaps order by name")
    @Query("select * from geojsonMaps order by name")
    fun getAllSimpleWithSort() : List<GeojsonMapEntity>
    //@Query("select name, north, south, west, east, bitmap, enabled from geojsonMaps")
    @Query("select * from geojsonMaps")
    fun getAllSimple() : List<GeojsonMapEntity>
    @Query("select * from geojsonMaps where z = :zoom")
    fun getAllSimple(zoom: Int) : List<GeojsonMapEntity>
    @Query("select * from geojsonMaps where path = :region")
    fun getAllSimple(region: String) : List<GeojsonMapEntity>

    @Query("select * from geojsonMaps where enabled = 1")
    fun getAllSimpleEnabled() : List<GeojsonMapEntity>
    @Query("select bitmap from geojsonMaps where id=(:id)")
    fun getGeojsonThumbnail(id: UUID): Bitmap?
}
