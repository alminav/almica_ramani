package com.almica.ramani.pdfcreator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import com.akash.images_to_pdf_using_jetpack_compose.ui.theme.ImagesToPdfUsingJetpackComposeTheme
import com.almica.ramani.Const

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val routeFolderExtraName =
            intent.getStringExtra(Const.EXTRA_ROUTEFOLDER)
        setContent {
            ImagesToPdfUsingJetpackComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainScreen(
                        routeFolderExtraName = routeFolderExtraName
                    )
                }
            }
        }
    }
}

