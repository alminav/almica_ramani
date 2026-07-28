package com.almica.ramani.routes

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.UUID

@Dao
interface RouteDao
{
    @Insert
    fun addRoute(routeEntity: RouteEntity)
    @Query("delete from routes WHERE region = :region")
    fun removeRoutes(region: String)

    @Query("delete from routes WHERE name = :name AND region = :region")
    fun removeRoute(name: String, region: String)

    @Query("select * from routes WHERE name = :name AND region = :region")
    fun selectRoute(name: String, region: String): List<RouteEntity>
    @Query("select * from routes WHERE name LIKE :name || '%' AND region = :region")
    fun findRoute(name: String, region: String): List<RouteEntity>

    @Query("delete from routes WHERE id = :id")
    fun removeRoute(id: UUID)

    @Query("select * from routes where id=(:id)")
    fun getRoute(id: UUID): LiveData<RouteEntity>

    @Query("select * from routes where id=(:id)")
    fun getRouteSimple(id: UUID): RouteEntity

    @Query("select * from routes")
    fun getAllSimple() : List<RouteEntity>

    @Query("select * from routes")
    fun getAll() : LiveData<List<RouteEntity>>

    @Query("select * from routes where name like :search")
    fun search(search : String) : LiveData<List<RouteEntity>>

    @Query("select * from routes where region = :region and name like :search")
    fun searchWithRegion(region : String, search : String) : LiveData<List<RouteEntity>>

    @Query("select * from routes where region = :region")
    fun getAllWithregion(region: String): LiveData<List<RouteEntity>>
    @Query("select * from routes where region = :region")
    fun getAllWithregionSimple(region: String): List<RouteEntity>
    @Query("select bitmap from routes where name = :name")
    fun getRouteThumbnail(name: String): Bitmap?

    @Query("UPDATE routes SET kmlString = :kmlString WHERE id = :uuid")
    fun updateRoute(kmlString: String, uuid: UUID)

    /*
        @Query("select * from routes where latitude >= :southEastLat and latitude <= :northWestLat and longitudeStart >= :northWestLng and longitudeStart <= :southEastLng")
        fun getAllAroundRouteEntitynt(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double) : List<RouteEntity>

        @Query("select * from routes where latitude >= :southEastLat and latitude <= :northWestLat and longitudeStart >= :northWestLng and longitudeStart <= :southEastLng and name like :search")
        fun searchAroundRouteEntitynt(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double, search : String) : List<RouteEntity>

        @Query("select * from routes where latitude >= :southEastLat and latitude <= :northWestLat and longitudeStart >= :northWestLng and longitudeStart <= :southEastLng and category = :category")
        fun getAllAroundRouteEntityntWithcategory(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double, category : Int) : List<RouteEntity>

        @Query("select * from routes where latitude >= :southEastLat and latitude <= :northWestLat and longitudeStart >= :northWestLng and longitudeStart <= :southEastLng and name like :search and category = :category")
        fun searchAroundRouteEntityntWithcategory(northWestLat : Double, northWestLng : Double, southEastLat : Double, southEastLng : Double, search : String, category : Int) : List<RouteEntity>

     */
}
