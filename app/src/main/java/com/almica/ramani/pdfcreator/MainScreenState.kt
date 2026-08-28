package com.almica.ramani.pdfcreator

import android.net.Uri
import java.io.File

data class MainScreenState(
    val imageUris : List<Uri> = emptyList(),
    val geojsonFile: File? = null,
    val isLoading : Boolean = false,
    val success : Boolean? = null
)
