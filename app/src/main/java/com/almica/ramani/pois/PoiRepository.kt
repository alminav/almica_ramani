package com.almica.ramani.pois

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ExecutorService

private const val logtag = "PoiRepository"
class PoiRepository private constructor(
    poiDatabase: PoiDatabase,
    private val executor: ExecutorService
) {
    // Database related fields/methods:
    val poiDao = poiDatabase.poiDao()

    fun getNearestPoi(
        latitude: Double,
        longitude: Double,
        maxDistanceMeters: Double = Double.MAX_VALUE,
        finished: (PoiEntity?) -> Unit
    ) {
        executor.execute {
            val pois = poiDao.getAllSimple()
            if (pois.isEmpty()) {
                finished(null)
                return@execute
            }
            val target = com.google.android.gms.maps.model.LatLng(latitude, longitude)

            val nearest = pois.map {
                val distance = SphericalUtil.computeDistanceBetween(
                    com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude),
                    target
                )
                Pair(it, distance)
            }.filter { it.second <= maxDistanceMeters }
             .minByOrNull {
                 //Timber.i("getNearestPoi: $it")
                 it.second
             }?.first

            finished(nearest)
        }
    }

    fun getAll(): LiveData<List<PoiEntity>> = poiDao.getAll()
    fun getAllFlow(): Flow<List<PoiEntity>> = poiDao.getAllFlow()
    fun getAllSimpleWithSort(finished: (List<PoiEntity>) -> Unit) {
        executor.execute { finished(poiDao.getAllSimpleWithSort()) }
    }

    fun getAllSimple(withSort: Boolean, finished: (List<PoiEntity>) -> Unit) {
        executor.execute {
            //Timber.i( "withSort: $withSort")
            if (withSort)
                finished(poiDao.getAllSimpleWithSort())
            else
                finished(poiDao.getAllSimple()) }
    }
    fun getAllWithcategory(category: String): LiveData<List<PoiEntity>> = poiDao.getAllWithcategory(category)
    fun getFirstWithCategory(category: String, result: (List<PoiEntity>) -> Unit) {
        executor.execute { result(poiDao.getFirstWithCategory(category)) }
    }
    // Not being used now but could in future versions.
    /**
     * Returns specific location in database.
     */
    fun getPoi(id: UUID): LiveData<PoiEntity> = poiDao.getPoi(id)
    fun getPoiSimpleById(id: UUID): PoiEntity = poiDao.getPoiSimple(id)
    fun getPoiSimpleByName(name: String): PoiEntity? = poiDao.getPoiSimpleByName(name)
    // Not being used now but could in future versions.
    /**
     * Updates location in database.
     */
//    fun updatePoi(poiEntity: PoiEntity) {
//        executor.execute {
//            poiDao.updatePoi(poiEntity)
//        }
//    }

    /**
     * Adds poi to the database.
     */
    fun addPoi(poiEntity: PoiEntity, finished: () -> Unit) {
        executor.execute {
            Timber.i( "PoiRepository.addPoi poiEntity $poiEntity")
            val result = poiDao.addPoi(poiEntity)
            Timber.i( "PoiRepository.addPoi result: $result")
            finished()
        }
    }

    /**
     * Adds list of pois to the database.
     */
    fun addPois(myPoiEntities: List<PoiEntity>) {
        executor.execute {
            poiDao.addPois(myPoiEntities)
        }
    }

    fun updatePoi(latitude: Double, longitude: Double, id: UUID, finished:() -> Unit) {
        executor.execute {
            poiDao.updatePoi(latitude, longitude, id)
            finished()
        }
    }

    fun updatePoiAltitude(altitude: Double, id: UUID, finished:() -> Unit) {
        executor.execute {
            poiDao.updatePoiAltitude(altitude, id)
            finished()
        }
    }

    fun removeAll(finished:() -> Unit) {
        executor.execute {
            poiDao.removeAll()
            finished()
        }
    }

    suspend fun removePoiSuspend(id: UUID) = withContext(Dispatchers.IO) {
        poiDao.removePoi(id)
    }

    suspend fun removeAllSuspend() = withContext(Dispatchers.IO) {
        poiDao.removeAll()
    }

    suspend fun updatePoiAltitudeSuspend(altitude: Double, id: UUID) = withContext(Dispatchers.IO) {
        poiDao.updatePoiAltitude(altitude, id)
    }

    fun removePoi(id: UUID, finished:() -> Unit) {
        executor.execute {
            poiDao.removePoi(id)
            finished()
        }
    }

    fun removePoiByName(name: String, category: String, finished:() -> Unit) {
        executor.execute {
            poiDao.removePoiByName(name, category)
            finished()
        }
    }

    fun removePoisByCategory(category: String, finished:() -> Unit) {
        executor.execute {
            poiDao.removePoisByCategory(category)
            finished()
        }
    }

    fun getAllFiltered(filter: String): LiveData<List<PoiEntity>> {
        return poiDao.search(filter)
    }

    fun getAllWithCategoryAndFilter(category: String, filter: String): LiveData<List<PoiEntity>> {
        return poiDao.searchWithCategory(category, filter)
    }

    companion object {
        @Volatile private var INSTANCE: PoiRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): PoiRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PoiRepository(PoiDatabase.getInstance(context), executor)
                    .also { INSTANCE = it }
            }
        }
    }
}