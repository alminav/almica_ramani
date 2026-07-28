package com.almica.ramani

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.compose.AndroidFragment
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.BackPressHandler
import timber.log.Timber

private const val logtag = "SatStatusComposeScreen"
@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SatStatusComposeScreen(finish: () -> Unit) {
    Timber.i( "start 1")
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    BackPressHandler {
        Timber.i(" Back Press intercepted")
        finish()
    }
    SatStatusComposeScreenContent(finish = finish) {
        AndroidFragment<com.almica.gpssatstatus.SatsFragment>(
            modifier = Modifier.padding(
                top = (1.25*marginTopDp).dp,
                bottom = marginTopDp.dp
            )
        ) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatStatusComposeScreenContent(
    finish: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        finish()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back home"
                    )
                }
            }, title = {
                Text(text = stringResource(R.string.sat_status), fontSize = 14.sp)
            }, actions = {})
    }, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun SatStatusComposeScreenPreview() {
    RamaniTheme {
        SatStatusComposeScreenContent(finish = {}) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Satellite Status Content Placeholder")
            }
        }
    }
}
