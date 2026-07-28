package com.almica.ramani

import android.graphics.Bitmap

data class RasterMapItemModel(
    val name: String,
    val path: String,
    var thumbnail: Bitmap?,
    val lastModifiedDate: String,
    val mapType: String,
    var selected: Boolean
)

enum class RasterMapsAction {
    Delete,
    Share,
    Confirm
}

data class MbTilesSnackbarData(
    val title: String?,
    val action: MbTilesSnackbarAction?,
    val actionText: String?,
    val actionData: String?
)

enum class MbTilesSnackbarAction {
    Nothing
}
