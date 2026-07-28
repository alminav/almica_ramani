package com.almica.ramani

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.almica.ramani.tilemaker.TilemakerPrefComposeScreen
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

class TilemakerPreferenceActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RamaniTheme {
                TilemakerPrefComposeScreen {
                    finish()
                }
            }
        }
    }
}