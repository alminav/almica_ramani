package com.almica.ramani

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.compose.AndroidFragment
import com.almica.ramani.ui.theme.RamaniTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefComposeScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    PrefComposeScreenContent(
        modifier = modifier,
        onBack = onBack
    ) { paddingValues ->
        PrefComposeContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefComposeScreenContent(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_home)
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.preferences))
                }
            )
        },
        content = content
    )
}

@Composable
fun PrefComposeContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AndroidFragment<PrefFragment> {}
    }
}

@Preview(showBackground = true)
@Composable
fun PrefComposeScreenPreview() {
    RamaniTheme {
        PrefComposeScreenContent(onBack = {}) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                Text(text = "Preferences Content Placeholder")
            }
        }
    }
}
