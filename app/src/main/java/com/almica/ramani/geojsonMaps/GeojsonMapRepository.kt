package com.almica.ramani.geojsonMaps

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import com.almica.ramani.utils.isNotNull
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ExecutorService

private const val logtag = "GeojsonMapRepository"
class GeojsonMapRepository private constructor(
    geojsonMapDatabase: GeojsonMapDatabase,
    private val executor: ExecutorService
) {
    // Database related fields/methods:
    val geojsonMapDao = geojsonMapDatabase.geojsonMapDao()

    fun getAll(): LiveData<List<GeojsonMapEntity>> = geojsonMapDao.getAll()
    fun getAllSimpleWithSort(finished: (List<GeojsonMapEntity>) -> Unit) {
        executor.execute { finished(geojsonMapDao.getAllSimpleWithSort()) }
    }
    fun getAllSimple(withSort: Boolean, finished: (List<GeojsonMapEntity>) -> Unit) {
        executor.execute {
            //Timber.i("withSort: $withSort")
            if (withSort)
                finished(geojsonMapDao.getAllSimpleWithSort())
            else
                finished(geojsonMapDao.getAllSimple()) }
    }

    fun getAllSimpleSync(withSort: Boolean): List<GeojsonMapEntity> {
        return if (withSort)
            geojsonMapDao.getAllSimpleWithSort()
        else
            geojsonMapDao.getAllSimple()
    }

    fun getAllSimpleEnabled(finished: (List<GeojsonMapEntity>) -> Unit) {
//        executor.execute {
            val enabledMaps = geojsonMapDao.getAllSimpleEnabled()
/*
            enabledMaps.forEach { geojsonMapEntity ->
                Timber.i( "${geojsonMapEntity.name}")
            }
 */
            finished(enabledMaps)
//        }
    }

    fun getAllSimple(zoom: Int, finished: (List<GeojsonMapEntity>) -> Unit) {
        executor.execute {
            finished(geojsonMapDao.getAllSimple(zoom))
        }
    }

    fun getAllSimple(region: String?, finished: (List<GeojsonMapEntity>) -> Unit) {
        if (region != null) {
            executor.execute {
                finished(geojsonMapDao.getAllSimple(region))
            }
        } else {
            executor.execute {
                finished(geojsonMapDao.getAllSimple())
            }
        }
    }

    fun getGeojsonMap(id: UUID): LiveData<GeojsonMapEntity> = geojsonMapDao.getGeojsonMap(id)
    fun getGeojsonMapSimpleById(id: UUID): GeojsonMapEntity = geojsonMapDao.getGeojsonMapSimple(id)
    fun getGeojsonMapSimpleByName(name: String): GeojsonMapEntity? {
        val result = geojsonMapDao.getGeojsonMapSimpleByName(name)
        return result
    }
    fun getGeojsonThumbnail(id: UUID): Bitmap? = geojsonMapDao.getGeojsonThumbnail(id)
    // Not being used now but could in future versions.
    /**
     * Updates location in database.
     */
//    fun updateGeojsonMap(geojsonMapEntity: GeojsonMapEntity) {
//        executor.execute {
//            geojsonMapDao.updateGeojsonMap(geojsonMapEntity)
//        }
//    }

    /**
     * Adds geojsonMap to the database.
     */
    fun insertGeojsonMap(geojsonMapEntity: GeojsonMapEntity, finished: (Boolean) -> Unit) {
        executor.execute {
            try {
                val result = geojsonMapDao.insertGeojsonMap(geojsonMapEntity)
                Timber.i( "ready: ${geojsonMapEntity.name}")
                finished(result.isNotNull())
            } catch(exception: Exception) {
                Timber.e("$exception")
                finished(false)
            }
        }
    }

    /**
     * Adds list of geojsonMaps to the database.
     */
    fun insertGeojsonMaps(myGeojsonMapEntities: List<GeojsonMapEntity>) {
        executor.execute {
            geojsonMapDao.insertGeojsonMaps(myGeojsonMapEntities)
        }
    }

    fun updateGeojsonMap(bitmap: Bitmap, id: UUID, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.updateGeojsonMap(bitmap, id)
            finished()
        }
    }

    fun updateGeojsonMapByName(bitmap: Bitmap, name: String, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.updateGeojsonMapByName(bitmap, name)
            finished()
        }
    }

    fun updateGeojsonMapData(data: ByteArray, name: String, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.updateGeojsonMapData(data, name, System.currentTimeMillis())
            finished()
        }
    }

    fun updateGeojsonMapStatus(status: Boolean, name: String, finished:() -> Unit) {
//        executor.execute {
            geojsonMapDao.updateGeojsonMapStatus(status, name)
            finished()
//        }
    }

    fun updateGeojsonMapStatus(status: Boolean, name: String, region: String, finished:() -> Unit) {
//        executor.execute {
        geojsonMapDao.updateGeojsonMapStatus(status, region, name)
        finished()
//        }
    }

    fun toggleGeojsonMapStatus(name: String, finished: () -> Unit) {
        executor.execute {
            geojsonMapDao.toggleGeojsonMapStatus(name)
            finished()
        }
    }
    fun getGeojsonMapStatusSync(name: String): Boolean {
        val b = geojsonMapDao.getGeojsonMapStatus(name)
        Timber.i( "$name $b")
        return b
    }

    fun getGeojsonMapStatus(name: String, finished: (Boolean) -> Unit) {
        executor.execute {
            val b = geojsonMapDao.getGeojsonMapStatus(name)
            Timber.i( "$name $b")
            finished(b)
        }
    }

    fun removeAll(finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.removeAll()
            finished()
        }
    }

    fun removeGeojsonMap(id: UUID, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.removeGeojsonMap(id)
            finished()
        }
    }

    fun removeGeojsonMapByName(name: String, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.removeGeojsonMapByName(name)
            finished()
        }
    }

    fun removeGeojsonMapsByRegion(region: String, finished: () -> Unit) {
        executor.execute {
            geojsonMapDao.removeGeojsonMapsByRegion(region)
            finished()
        }
    }

    fun removeGeojsonMapByXYZ(x: Int, y: Int, z: Int, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.removeGeojsonMapByXYZ(x, y, z)
            finished()
        }
    }

    fun removeGeojsonMapsByZoom(zoom: Int, finished:() -> Unit) {
        executor.execute {
            geojsonMapDao.removeGeojsonMapsByZoom(zoom)
            finished()
        }
    }

    companion object {
        @Volatile private var INSTANCE: GeojsonMapRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): GeojsonMapRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GeojsonMapRepository(GeojsonMapDatabase.getInstance(context), executor)
                    .also { INSTANCE = it }
            }
        }
    }
}