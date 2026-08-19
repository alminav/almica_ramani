package com.almica.ramani

import android.graphics.Color
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.graphhopper.routing.util.EncodingManager
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.Locale

class Const {
    companion object {
        val DP64 = 64.dp
        val DP52 = 52.dp
        val DP48 = 48.dp
        val DP32 = 32.dp
        val DP40 = 40.dp
        val DP42 = 42.dp

        val ELEVATION_CHART_PEEK_HEIGHT = 300.dp
        val GRADIENT_CHART_PEEK_HEIGHT = 200.dp
        val TOGGLE_BUTTONS_PEEK_HEIGHT_LARGE = 100.dp
        val TOGGLE_BUTTONS_PEEK_HEIGHT_SMALL = 60.dp
        val HIDDEN_PEEK_HEIGHT = 0.dp
        val LOCATIONS_CHART_PEEK_HEIGHT = 240.dp

        /**
         * In MapLibre Android, the rendering sequence of style layers is determined by their order in the style JSON,
         * which is typically rendered from the bottom up (first layer in the list is rendered first, last layer last).
         * Layers added later appear on top of layers added earlier.
         * --> place countries and planet at bottom of json
         */
        const val EMPTY_STYLE_FILENAME = "empty_style.json"
        const val ONLY_BACKGROUND_FILENAME = "only_background.json"
        const val MAPTILER_REMOTE_STYLE_FILENAME = "osm_bright_gl_style.json"
        const val MVT_OFFLINE_STYLE_FILENAME = "basic_offline_with_planet.json"
        const val PLANET_MVT_FILENAME = "planet.mbtiles"
        const val COUNTRIES_MVT_FILENAME = "countries.mbtiles"
        const val COUNTRIES_STYLE_FILENAME = "country_ready_style.json"
        const val PLANET_STYLE_FILENAME = "planet_ready_style.json"
        const val GEOJSON_OFFLINE_STYLE_FILENAME = "geojson_offline.json"
        const val RASTER_DEM_SOURCE: String = "RasterDemSource"
        const val RASTER_DEM_LAYER: String = "hillshade-layer-id"
        const val PLACE_LATITUDE: String = "place.latitude"
        const val PLACE_LONGITUDE: String = "place.longitude"
        const val PLACE_NAME: String = "place.name"
        const val PLANET: String = "Planet"
        const val TILE_PREFIX = "tile_"
        const val MVT_PREFIX = "mvt_"
        const val ORG_MAPLIBRE: String = "org.maplibre"
        const val COMPOSITE: String = "composite"
        const val MAPBOX: String = "mapbox"
        const val UNDERLINE: String = "_"
        const val HASHTAG: String = "#"
        const val MAPBOX_ACCESS_TOKEN: String = "pk.eyJ1IjoiYWxtaWNhIiwiYSI6ImNsdmdyNTNidDBiMGEyam55Mml1bmhleGEifQ.9926OzKJdhvz9d8FCjZxSQ"
        val wellKnownTileServer = WellKnownTileServer.Mapbox // WellKnownTileServer.MapLibre does not work
        const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
        const val styleVectorUri = "https://demotiles.maplibre.org/style.json"
//        const val NO_VALID_RESULT: String = "No valid result"
//        const val GEOJSON_LINELAYER: String = "GeoJson.LineLayer"
//        const val GEOJSON_SYMBOLLAYER: String = "GeoJson.SxmbolLayer"
//        const val PREF_GEOJSON_FILEPATH: String = "pref.geojson.filepath"
//        const val PREF_GEOJSON_MAPS_SUBFOLDER: String = "pref.geojson.maps.subfolder"
        const val GEOJSON_MAP_FOLDER: String = "geojson_maps"
        //const val GEOJSON_MAP_INDEX: String = "map_index.geojson"
        //const val HILLSHADES_LAYERID : String = "hillshades-layer"
        //const val HILLSHADES_SOURCEID : String = "hillshades"
        //const val HILLSHADES_URL : String = "https://api.maptiler.com/tiles/hillshade/tiles.json?key="
        //const val CONTOURLINES_URL : String = "https://demotiles.maplibre.org/terrain-tiles"

        // local geojson file, converted from QGis, source bbbike pmtiles
        //const val GEOJSON_FOLDER_NAME: String = "geojsonTile_1083_673_11"//"geojsonTile_2165_1345_12"
        const val GEOJSON_ROOT_FOLDER: String = "geojson"
        // geojsonTile_1083_673_11: The program freezes with buildings layer, without ok
        // geojsonTile_2165_1345_12: can be used with buildings layer

        const val ORS_TAG: String = "ors"
        const val MBGL_METADATA_REGION_NAME = "name"
        const val MBGL_REGION_ = "MBGL_REGION_"
        const val GH_FOLDER = "gh"
        const val GH_FOLDERNAME = "gh.name"
        const val GH_FILENAME = "gh.filename"
        const val RESULT_GEOJSON_FOLDERNAME = "result.geojson.name"
        const val RESULT_GEOJSON_FILENAME = "result.geojson.filename"
        const val SETRESULT_IMPORT_HGT = "setresult.import.hgt"
        const val SETRESULT_IMPORT_GEOJSON = "setresult.import.geojson"
        const val DEFAULT_LOCOMOTION: String = "1.1"
        const val TIME_PATTERN_LONG: String = "yyMMdd_HHmmss"
        const val PREF_GH_FILEPATH: String = "pref.gh.filepath"
        const val PREF_GEOJSON_FILEPATH: String = "pref.geojson.filepath"
        const val GH_TAG: String = "gh"
        const val GMS_TAG: String = "gms"
        const val HGT_FOLDER_NAME = "hgt"
        const val HGT_EXT = ".hgt"
        const val ZIP_EXT: String = ".zip"
        const val UNKNOWN = "unknown"
        const val PREF_MBTILES_FILEPATH_SET: String = "pref.mbtiles.filepath.set"
        const val PREF_CYCLEWAY_OVERLAYS_FILEPATH_SET: String = "pref.cycleway.overlays.filepath.set"
        const val PREF_MVT_FILEPATH: String = "pref.mvt.filepath"
        //const val PREF_MVT_STYLEURL: String = "pref.mvt.styleurl"
        const val PREF_HILLSHADE_VISIBILITY: String = "pref.hillshade.visibility"
        const val PREF_PLANET_VISIBILITY: String = "pref.planet.visibility"
        const val PLANET_LAYER_TAG = "planet"
        const val COUNTRIES_LAYER_TAG = "countries"
        const val JOURNAL: String = "journal"
        //const val SERVICE_STATUS = "service.status"
        //const val EARTH_RADIUS = 6378137.0
        //const val EXTRA_DIRECT_DOWNLOAD_URL: String = "extra.direct.download.url"
        const val EXTRA_FILETYPE: String = "extra.filetype"
        //const val EXTRA_FILENAME: String = "extra.filename"
        const val EXTRA_MVTNAME: String = "extra.mvtname"
        const val EXTRA_ROUTEFOLDER: String = "extra.routefolder"
        const val EXTRA_LATITUDE: String = "extra.latitude"
        const val EXTRA_LONGITUDE: String = "extra.longitude"
        const val EXTRA_ZOOM: String = "extra.zoom"
        const val EXTRA_KMLSTRING: String = "extra.kmlstring"
        const val EXTRA_CLOUD_STYLE: String = "extra.cloud.style"
        const val EXTRA_MVT_MAP_PATH: String = "extra.mvt.map.path"
        const val EXTRA_ACTIVITY: String = "extra.activity"
        const val EXTRA_RESTART: String = "extra.restart"
        const val EXTRA_LATLNG: String = "extra.latlng"
        const val EXTRA_ROUTE_DIALOG_MODE: String = "extra.route.dialog.mode"
        //const val EXTRA_DRIVE_URL: String = "extra.drive.url"
        const val PREF_LATITUDE: String = "pref.latitude"
        const val PREF_LONGITUDE: String = "pref.longitude"
        const val PREF_KEEP_SCREEN_ON: String = "pref.keep.screen.on"
        //const val PREF_LOCATION_ENABLED: String = "pref.location.enabled"
        const val PREF_USE_CYCLEWAYS_OVERLAY: String = "pref.use.cycleway.overlay"
        const val PREF_USE_STEPCOUNTER: String = "pref.use.stepcounter"
        const val PREF_ROUTES_REGION_FILTER: String = "pref.routes.region.filter"
        const val PREF_MAPTYPE_KEY: String = "pref.maptype.key"
        const val PREF_RENDER_MODE: String = "pref.render.mode"
        const val RENDER_MODE_FREE = "0"
        const val RENDER_MODE_TRACKING = "1"
        const val RENDER_MODE_COMPASS = "2"
        const val PREF_CAMERA_MODE: String = "pref.camera.mode"
        //const val PREF_RENDER_MODE: String = "pref.render.mode"
        //const val PREF_MAP_ZOOM: String = "pref.map.zoom"
        //const val PREF_NORTH_UP: String = "pref.north.up"
        const val PREF_GMS_NORTH_UP: String = "pref.gms.north.up"
        const val KM_TO_MILES: Double = 0.621371192
        const val ALTITUDE_CORRECTION: Int = -46
        const val TRACKFOLDER: String = "tracks"
        const val ROUTEFOLDER: String = "routes"
        const val MBTILES_FOLDER: String = "mbtiles"
        const val CYCLEWAY_FOLDER: String = "cycleways"
        const val MVT_FOLDER: String = "mvt"
        const val GEOJSON_QGIS_STYLE_FILENAME = "geojson_local_file_qgis.json"
        //const val CYCLE_LAYER_PREFIX: String = "cycle."
        const val DB_JOURNAL_SUFFIX: String = "-journal"
        const val GPX_EXT: String = ".gpx"
        const val KML_EXT: String = ".kml"
        const val JPG_EXT: String = ".jpg"
        const val PNG_EXT: String = ".png"
        const val GEOJSON_EXT = ".geojson"
        const val GEOJSON_PREFIX = "geojsonTile_"
        const val MBTILES_EXT: String = ".mbtiles"
        const val TXT_EXT: String = ".txt"
        const val PDF_EXT = ".pdf"
        const val THUMBNAILS = "thumbnails"
        const val EXIF_MAX_SIZE = 64 * 1024
        val DF: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        const val LATLNG_GRID_SOURCE = "latlng_grid_source"
        const val LATLNG_GRID_LAYER = "latlng_grid_layer"
        const val TIME_PATTERN_LONG_YEAR: String = "yyyyMMdd_HHmm"
        const val TIME_PATTERN_LONG_YEAR_DE: String = "dd/MM/yyyy HH:mm"
        const val UC_VERTICAL_DIVIDER: String = "\u007C"
        const val UC_DISTANCE_ARROW: String = "\u21D4"
        const val UC_ELE_ARROW: String = "\u25B2"
        const val UC_RIGHT_ARROW: String = "\u25B6"
        const val UC_LEFT_ARROW: String = "\u25C0"
        const val UC_UP_ARROW: String = "\u25B2"
        const val UC_DOWN_ARROW: String = "\u25BC"
        const val UC_SPEED: String = "\u267D"
        const val UC_REGION: String ="\u25A8"
        const val UC_MENU: String ="\u2630"
        const val UC_POSITION: String ="\u2316"
        const val TAG_ROUTE_DAO = "RouteDao"
        const val PREF_ROUTEFOLDER_FILEPATH: String = "pref.routefolder.path"
        const val HOME: String = "home"
        const val UC_CLOSE: String = "\u2716"
        const val UC_CHECKMARK: String = "\u2713"
        const val PROPERTY_COLOR_RGBA = "property.color.rgba"
        const val PROPERTY_TYPE = "property.type" // 0=file, 1=dao, 2=gh, 3=ors
        const val ROUTE_TYPE_FILE = 0
        const val ROUTE_TYPE_DAO = 1
        const val ROUTE_TYPE_GH = 2
        const val ROUTE_TYPE_ORS = 3
        const val ROUTE_TYPE_JSON = 4
        const val ROUTE_TYPE_GPS = 5
        const val ROUTE_TYPE_GEOJSON_BORDER = 5
        const val ROUTE_TYPE_LOCATIONS = 6
        const val ROUTE_TYPE_OFFREGION_BORDER = 7
        const val PROPERTY_NAME = "property.name"
        const val PROPERTY_UUID = "property.uuid"
        const val PROPERTY_REGION = "property.region"
        const val PROPERTY_KMLSTRING = "property.kmlstring"
        const val PROPERTY_LAT = "property.lat"
        const val PROPERTY_LON = "property.lon"
        const val PROPERTY_ALT = "property.alt"
        const val PROPERTY_CATEGORY = "property.category"
        const val PROPERTY_DISTANCE_M = "property.distance"
        const val PROPERTY_SPEED = "property.speed"
        const val PROPERTY_TIME = "property.time"
        // TileMaker
        const val FM_NOTIFICATION_ID = 333
        const val CHANNEL_ID = "location"
        const val MBTILES = "mbtiles"
        const val PHONEMAPS = "PhoneMaps"
        const val OPENTOPO : String = "OpenTopo"
        const val CYCLEMAPS = "Cyclemaps"
        const val THUNDERFOREST = "Thunderforest"
        const val OUTDOOR = "Outdoor"

        const val MBTILES_PHONEMAPS = "mbtiles_phonemaps"
        const val MBTILES_OPENTOPO = "mbtiles_opentopo"
        const val MBTILES_CYCLEMAPS = "mbtiles_cyclemaps"
        const val MBTILES_THUNDERFOREST = "mbtiles_thunderforest"
        const val URL_THUNDERFOREST =
            "https://a.tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey=5e2a4558d7a64cbe93fbe6740b019177"
        const val URL_OPENSTREETMAP_CYCLOSM =
            "https://dev.a.tile.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png"
        var URLS_OPENSTREETMAP_CYCLOSM = arrayOf(
            "https://dev.a.tile.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
            "https://dev.b.tile.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
            "https://dev.c.tile.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png"
        )
        var URLS_THUNDERFOREST_OUTDOORS = arrayOf(
            "https://a.tile.thunderforest.com/outdoors/{z}/{x}/{y}.png?apikey=5e2a4558d7a64cbe93fbe6740b019177",
            "https://b.tile.thunderforest.com/outdoors/{z}/{x}/{y}.png?apikey=5e2a4558d7a64cbe93fbe6740b019177",
            "https://c.tile.thunderforest.com/outdoors/{z}/{x}/{y}.png?apikey=5e2a4558d7a64cbe93fbe6740b019177"
        )
        const val THUNDERFOREST_OUTDOORS_PART = "tile.thunderforest.com/outdoors"

        const val URL_OPENTOPO = "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"
        const val OPENTOPO_PART = "tile.opentopomap.org"
        var URLS_OPENTOPO = arrayOf(
            "https://a.tile.opentopomap.org/{z}/{x}/{y}.png",
            "https://b.tile.opentopomap.org/{z}/{x}/{y}.png",
            "https://c.tile.opentopomap.org/{z}/{x}/{y}.png"
        )
        const val URL_PHONEMAPS = "https://webtiles.timepress.cz/open/hike_256/{z}/{x}/{y}.png"
        var URL_OUTDOOR =
            "https://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web/default/WEBMERCATOR/{z}/{y}/{x}.png"

        val ROUTE_COLORS = arrayOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            Color.YELLOW,
            "#FFBB33".toColorInt(),
            "#AA66CC".toColorInt(),
            Color.BLACK
        )
        const val LINEPATTERN_DASH: String = "linepattern.dash"
        const val LINEPATTERN_DEFAULT: String = "linepattern.default"
        const val LINEPATTERN_ARROW: String = "linepattern.arrow"
        const val LINEPATTERN_CIRCLE: String = "linepattern.circle"
        const val GRADIENT_UP_UC: String = "\u2197"
        const val GRADIENT_DOWN_UC: String = "\u2198"
        const val UC_CIRCLE_RED = "\u2B55"
        const val UC_FILLED_CIRCLE = "\u2B24"
        const val UC_ARROW_UP = "\u25B2"
        const val UC_THREEDOTS: String = "\u2026"
        const val GRADIENT_IMAGE_KEY_PART1 = "gradient.image."
        const val UC_DOWNWARDS_ARROW = "\u2193"
        const val UC_DROPDOWN_ARROW = "\u2304"
        const val UC_DROPUP_ARROW = "\u2303"
        const val UC_UPWARDS_ARROW = "\u2191"
        const val UC_UPWARDS_ARROW_FROM_BAR = "\u21A5"
        const val UC_DOWNWARDS_ARROW_FROM_BAR = "\u21A7"
        const val LAST_LOCATION_LAT = "last.location.lat"
        const val LAST_LOCATION_LON = "last.location.lon"
        const val LAST_LOCATION_ALT = "last.location.alt"
        const val LAST_LOCATION_SPEED = "last.location.speed"
        const val LAST_LOCATION_BEARING = "last.location.bearing"
        //const val LAST_LOCATION_TIME = "last.location.time"
        const val LOCATIONS = "_locations_"
        const val PREF_MAP_TRACKING: String = "pref.map.tracking"
        const val TAG_START_ROUTE = "start-route"
        const val TAG_START_NAVIGATION = "start-navigation"
        const val TAG_STOP_NAVIGATION = "stop-navigation"
        const val TAG_START = "Start"
        const val TAG_START_TIME = "Start.Time"
        const val TAG_FINISH = "Finish"
        const val M_TO_FT: Double = 3.28084
        const val GERMANY = "germany"
        val MEERWEG13 = LatLng(52.325, 10.371)
        val PEINE = LatLng(52.319549, 10.234483)

        @Retention(AnnotationRetention.SOURCE)
        @StringDef(
            VehicleEncoding.FOOT_ENCODING,
            VehicleEncoding.BIKE_ENCODING,
            VehicleEncoding.CAR_ENCODING
        )
        annotation class VehicleEncoding {
            companion object {
                const val FOOT_ENCODING: String = EncodingManager.FOOT
                const val BIKE_ENCODING: String = EncodingManager.BIKE
                const val CAR_ENCODING: String = EncodingManager.CAR
                const val AIRPLANE_ENCODING: String = EncodingManager.AIRPLANE
            }
        }

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(Weighting.SHORT, Weighting.FAST)
        annotation class Weighting {
            companion object {
                const val FAST: Int = 0
                const val SHORT: Int = 1
            }
        }
        @StringDef(WeightingEncoding.FAST_ENCODING, WeightingEncoding.SHORT_ENCODING)
        annotation class WeightingEncoding {
            companion object {
                const val FAST_ENCODING: String = "fastest"
                const val SHORT_ENCODING: String = "shortest"
            }
        }
    }
    enum class MapType {
        Vector,
        Raster,
        GeoJson
    }
}