package com.almica.ramani

/**
 * Represents a folder containing route files.
 *
 * @property name The display name of the folder.
 * @property path The absolute filesystem path to the folder.
 * @property fileCount The number of route files currently in the folder.
 */
data class RouteFolder(
    val name: String,
    val path: String,
    val fileCount: Int
)
