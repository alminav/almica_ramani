package com.almica.ramani.googlemaps

import androidx.compose.runtime.Composable
import com.almica.ramani.utils.WheelItem
import com.almica.ramani.utils.WheelSelectionDialog

@Composable
fun MaptypeWheel(
    options: List<MapTypeOption>,
    finished: () -> Unit,
    select: (WheelItem) -> Unit,
    initialSelection: Int = 1
) {
    val wheelItems = options.mapIndexed { index, option ->
        WheelItem(index, option.entry, option.imageRes)
    }
    WheelSelectionDialog(
        onDismissRequest = { finished() },
        onItemSelected = { wheelItem -> select(wheelItem)  },
        items = wheelItems,
        initialSelection = wheelItems.getOrNull(initialSelection)
    )

}