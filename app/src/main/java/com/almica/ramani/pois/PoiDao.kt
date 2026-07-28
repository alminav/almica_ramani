package com.almica.ramani.pois

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.UUID

@Dao
interface PoiDao
{
    @Insert
    fun addPoi(poiEntity: PoiEntity)

    @Insert
    fun addPois(poiEntities: List<@JvmSuppressWildcards PoiEntity>)

    @Query("UPDATE poi SET latitude = :latitude, longitude = :longitude WHERE id = :uuid")
    fun updatePoi(latitude: Double, longitude: Double, uuid: UUID)

    @Query("UPDATE poi SET altitude = :altitude WHERE id = :uuid")
    fun updatePoiAltitude(altitude: Double, uuid: UUID)

    @Query("delete from poi WHERE id = :id")
    fun removePoi(id: UUID)

    @Query("delete from poi")
    fun removeAll()

    @Query("delete from poi WHERE name = :name AND category = :category")
    fun removePoiByName(name: String, category: String)

    @Query("delete from poi WHERE category = :category")
    fun removePoisByCategory(category: String)

    @Query("select * from poi where id=(:id)")
    fun getPoi(id: UUID): LiveData<PoiEntity>

    @Query("select * from poi where id=(:id)")
    fun getPoiSimple(id: UUID): PoiEntity

    @Query("select * from poi where lower(name)=(:name) LIMIT 1")
    fun getPoiSimpleByName(name: String): PoiEntity?

    @Query("select * from poi")
    fun getAll() : LiveData<List<PoiEntity>>

    @Query("select * from poi")
    fun getAllFlow() : kotlinx.coroutines.flow.Flow<List<PoiEntity>>

    @Query("select * from poi order by name")
    fun getAllSimpleWithSort() : List<PoiEntity>
    @Query("select * from poi")
    fun getAllSimple() : List<PoiEntity>


    @Query("select * from poi where name like :search")
    fun search(search : String) : LiveData<List<PoiEntity>>

    @Query("select * from poi where name like :search AND category = :category")
    fun searchWithCategory(category: String, search : String) : LiveData<List<PoiEntity>>

    @Query("select * from poi where category = :category")
    fun getAllWithcategory(category: String) : LiveData<List<PoiEntity>>

    @Query("select * from poi where category = :category")
    fun getFirstWithCategory(category: String) : List<PoiEntity>

    @Query("select * from poi where category = :category and name like :search")
    fun searchWithcategory(category: String, search : String) : List<PoiEntity>

    @Query("select * from poi where latitude >= :southEastLat and latitude <= :northWestLat and longitude >= :northWestLng and longitude <= :southEastLng")
    fun getAllAroundPoiEntitynt(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double) : List<PoiEntity>

    @Query("select * from poi where latitude >= :southEastLat and latitude <= :northWestLat and longitude >= :northWestLng and longitude <= :southEastLng and name like :search")
    fun searchAroundPoiEntitynt(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double, search : String) : List<PoiEntity>

    @Query("select * from poi where latitude >= :southEastLat and latitude <= :northWestLat and longitude >= :northWestLng and longitude <= :southEastLng and category = :category")
    fun getAllAroundPoiEntityntWithcategory(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double, category: String) : List<PoiEntity>

    @Query("select * from poi where latitude >= :southEastLat and latitude <= :northWestLat and longitude >= :northWestLng and longitude <= :southEastLng and name like :search and category = :category")
    fun searchAroundPoiEntityntWithcategory(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double, search : String, category: String) : List<PoiEntity>
}
