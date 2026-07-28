package com.almica.ramani

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun MainPopupMessage(
    message: String?,
    onDismiss: () -> Unit
) {
    message?.let { msg ->
        Popup(
            properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            alignment = Alignment.Center,
            onDismissRequest = onDismiss
        ) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = msg, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
