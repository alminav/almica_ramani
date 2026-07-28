package com.almica.ramani

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

data class MainSnackbarData(val msg: String, val actionText: String?, val action: MainSnackbarSelection?, val data: Any?)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainMoboSnack(
    mainSnackbarData: MainSnackbarData,
    finished: (action: MainSnackbarSelection) -> Unit
) {
    ModalBottomSheet(onDismissRequest = { finished(MainSnackbarSelection.Nothing) }) {
        MainMoboSnackContent(mainSnackbarData = mainSnackbarData) {action ->
            finished(action)
        }
    }
}

@Composable
internal fun MainMoboSnackContent(
    mainSnackbarData: MainSnackbarData,
    finished: (action: MainSnackbarSelection) -> Unit = {}
) {
    Timber.i("mainSnackbarData: ${mainSnackbarData.msg}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp, start = 3.dp, end = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mainSnackbarData.msg,
                Modifier
                    .weight(0.8f)
                    .padding(top = 8.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Blue
            )
            mainSnackbarData.actionText?.let { text ->
                TextButton(onClick = {
                    Timber.i("${mainSnackbarData.action?.name}")
                    mainSnackbarData.action?.let { finished(it) }
                }, modifier = Modifier.weight(0.2f)) {
                    Text(
                        text = text,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Blue
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainMoboSnackPreview() {
    RamaniTheme {
        MainMoboSnackContent(
            mainSnackbarData = MainSnackbarData(
                msg = "This is a sample snackbar message",
                actionText = "OK",
                action = MainSnackbarSelection.Nothing,
                data = null
            )
        )
    }
}
