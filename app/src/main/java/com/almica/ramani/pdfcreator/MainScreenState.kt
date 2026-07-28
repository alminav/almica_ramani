package com.almica.ramani.pdfcreator

import android.graphics.Bitmap
import java.io.File

data class MainScreenState(
    val imageBitmaps : List<Bitmap> = emptyList(),
    val geojsonFile: File? = null,
    val isLoading : Boolean = false,
    val success : Boolean? = null
)
