package com.almica.ramani

/**
 * Represents the selection of a GeoJSON folder or a back action.
 *
 * @property path The absolute filesystem path to the folder. Empty if [isBack] is true.
 * @property name The display name of the folder. Empty if [isBack] is true.
 * @property isBack True if the selection represents a "back" or "cancel" action.
 */
data class GeojsonFolderSelection(
    val path: String,
    val name: String,
    val isBack: Boolean
)
