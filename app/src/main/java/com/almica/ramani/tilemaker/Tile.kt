package com.almica.ramani.tilemaker

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

class Tile {
    data class TileCoordinate(val x: Int, val y: Int, val z: Int)
    companion object {
        private fun getXYTile(lat: Double, lon: Double, zoom: Int): TileCoordinate {
            val latRad = Math.toRadians(lat)
            var xtile = floor((lon + 180) / 360 * (1 shl zoom)).toInt()
            var ytile = floor((1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom)).toInt()

            if (xtile < 0) {
                xtile = 0
            }
            if (xtile >= 1 shl zoom) {
                xtile = (1 shl zoom) - 1
            }
            if (ytile < 0) {
                ytile = 0
            }
            if (ytile >= 1 shl zoom) {
                ytile = (1 shl zoom) - 1
            }
            return TileCoordinate(xtile, ytile, zoom)
        }

        @JvmStatic
        fun getTiles(northWest: DoubleArray, southEst: DoubleArray, zooms: IntArray):
                MutableSet<Tile.TileCoordinate> {
            val listTileBox = HashSet<TileCoordinate>()

            for (zoom in zooms[0]..zooms[1]) {
                val minXtile = getXYTile(northWest[0], northWest[1], zoom).x
                val maxXtile = getXYTile(southEst[0], southEst[1], zoom).x

                val minYtile = getXYTile(northWest[0], northWest[1], zoom).y
                val maxYtile = getXYTile(southEst[0], southEst[1], zoom).y

                for (x in minXtile..maxXtile) {
                    for (y in minYtile..maxYtile) {
                        listTileBox.add(TileCoordinate(x, y, zoom))
                    }
                }
            }

            return listTileBox
        }
    }
}