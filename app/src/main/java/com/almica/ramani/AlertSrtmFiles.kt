package com.almica.ramani

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.almica.ramani.ui.theme.RamaniTheme
import java.io.File

/**
 * A dialog to select or import SRTM (.hgt) files.
 *
 * @param srtmFile The currently selected SRTM file, if any.
 * @param onFileSelected Callback when a file is clicked for selection.
 * @param onImportRequested Callback when the "Import" button is clicked.
 * @param onDismiss Callback to dismiss the dialog.
 */
@Composable
fun AlertSrtmFiles(
    srtmFile: File?,
    onFileSelected: (File) -> Unit,
    onImportRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Move file I/O out of the main composition body and avoid re-reading on every frame
    val hgtFiles by remember(context) {
        derivedStateOf {
            val hgtFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
            hgtFolder.listFiles()?.sortedBy { it.name } ?: emptyList()
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        ),
        shape = RoundedCornerShape(28.dp),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onImportRequested) {
                Text(
                    text = stringResource(R.string.import_title),
                    style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.uc_close),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.srtm_files),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp)
                    .heightIn(max = 400.dp)
            ) {
                if (hgtFiles.isEmpty()) {
                    Text(
                        text = "No SRTM files found.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(hgtFiles, key = { it.absolutePath }) { file ->
                            val isSelected = srtmFile?.name == file.name
                            TextButton(
                                onClick = { onFileSelected(file) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                            ) {
                                Text(
                                    text = file.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AlertSrtmFilesPreview() {
    RamaniTheme {
        AlertSrtmFiles(
            srtmFile = File("N00E000.hgt"),
            onFileSelected = {},
            onImportRequested = {},
            onDismiss = {}
        )
    }
}
