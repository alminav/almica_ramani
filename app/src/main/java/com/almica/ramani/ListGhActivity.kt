package com.almica.ramani

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

class ListGhActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RamaniTheme {
                ListGhScreen { resultPair ->
                    Timber.i("resultPair: ${resultPair.first} ${resultPair.second}")
                    if (resultPair.first.isNotEmpty() && resultPair.second.isNotEmpty()) {
                        val resultIntent = Intent().apply {
                            putExtra(Const.GH_FOLDERNAME, resultPair.first)
                            putExtra(Const.GH_FILENAME, resultPair.second)
                        }
                        setResult(RESULT_OK, resultIntent)
                    } else {
                        setResult(RESULT_CANCELED)
                    }
                    finish()
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ListGhActivity::class.java)
        }
    }
}