package com.almica.ramani

/**
 * A simple reference to a folder on the filesystem.
 *
 * @property name The display name of the folder.
 * @property path The absolute path to the folder.
 */
data class FolderReference(
    val name: String,
    val path: String
)
