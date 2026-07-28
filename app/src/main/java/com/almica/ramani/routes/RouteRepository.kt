package com.almica.ramani.routes

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import java.util.UUID
import java.util.concurrent.ExecutorService

private const val logtag = "RouteRepository"
class RouteRepository private constructor(
    routeDatabase: RouteDatabase,
    private val executor: ExecutorService
) {
        // Database related fields/methods:
    val routeDao = routeDatabase.routeDao()

    fun getAllSimple(): List<RouteEntity> = routeDao.getAllSimple()
    fun getAll(): LiveData<List<RouteEntity>> = routeDao.getAll()
    // Not being used now but could in future versions.
    /**
     * Returns specific location in database.
     */
    fun getRoute(id: UUID): LiveData<RouteEntity> = routeDao.getRoute(id)
    fun getRouteSimple(id: UUID): RouteEntity = routeDao.getRouteSimple(id)

    // Not being used now but could in future versions.
    /**
     * Updates location in database.
     */
    fun updateRoute(kmlString: String, uuid: UUID) {
        executor.execute {
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: $uuid $kmlString")
            routeDao.updateRoute(kmlString, uuid)
        }
    }

    /**
     * Adds routes to the database.
     */
    fun addRoute(routeEntity: RouteEntity, finished: () -> Unit) {
        executor.execute {
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: addRoute ${routeEntity.name} ${routeEntity.region}")
            val result = routeDao.addRoute(routeEntity)
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: addRoute " + "result: $result")
            finished()
        }
    }

    fun removeRoutes(region: String, finished: () -> Unit) {
        Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: $region")
        executor.execute {
            val result = routeDao.removeRoutes(region)
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: $result")
            finished()
        }
    }

    fun replaceRoute(routeEntity: RouteEntity, finished: () -> Unit) {
        Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: replaceRoute ${routeEntity.name} ${routeEntity.region}")
        executor.execute {
            var result = routeDao.removeRoute(routeEntity.name, routeEntity.region)
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: remove result $result")
            result = routeDao.addRoute(routeEntity)
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: add    result: $result")
            finished()
        }
    }
    fun removeRoute(name: String, region: String) {
        Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: removeRoute $name $region")
        executor.execute {
            val result = routeDao.removeRoute(name, region)
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: result $result")
        }
    }

    fun selectRoute(name: String, region: String, finished: (List<RouteEntity>) -> Unit) {
        executor.submit {
            val result = routeDao.selectRoute(name, region)
            finished(result)
        }
    }

    fun findRouteSync(name: String, region: String, finished: (List<RouteEntity>) -> Unit) {
        val result = routeDao.findRoute(name, region)
        finished(result)
    }

    fun findRoute(name: String, region: String, finished: (List<RouteEntity>) -> Unit) {
        executor.submit {
            val result = routeDao.findRoute(name, region)
            finished(result)
        }
    }

    fun removeRoute(id: UUID) {
        executor.execute {
            routeDao.removeRoute(id)
        }
    }

    fun getAllWithregion(region: String): LiveData<List<RouteEntity>> = routeDao.getAllWithregion(region)

    fun getAllWithregionSimple(region: String, finished: (List<RouteEntity>) -> Unit) {
        executor.submit {
            finished(routeDao.getAllWithregionSimple(region))
        }
    }

    fun getAllSimple(finished: (List<RouteEntity>) -> Unit) {
        executor.submit {
            finished(routeDao.getAllSimple())
        }
    }

    fun getAllFiltered(filter: String): LiveData<List<RouteEntity>> {
        return routeDao.search(filter)
    }

    fun getAllWithRegionAndFilter(region: String, filter: String): LiveData<List<RouteEntity>> {
        return routeDao.searchWithRegion(region, filter)
    }

    fun getRouteThumbnail(name: String, finished: (Bitmap?) -> Unit){
        executor.submit {
            finished(routeDao.getRouteThumbnail(name))
        }
    }
    companion object {
        @Volatile private var INSTANCE: RouteRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): RouteRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RouteRepository(RouteDatabase.getInstance(context), executor)
                    .also { INSTANCE = it }
            }
        }
    }
}