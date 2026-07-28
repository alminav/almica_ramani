package com.almica.ramani.utils

import android.annotation.SuppressLint
import com.almica.ramani.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.almica.ramani.Const
import com.almica.ramani.Helpers.Companion.convertTimeToLong
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTimeDialog(
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )
    val datePickerState = rememberDatePickerState()
    datePickerState.selectedDateMillis = System.currentTimeMillis()
    val scrollState = rememberScrollState()
    DatePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = {
            val dateMilliSeconds = convertTimeToLong(timePickerState.hour,
                timePickerState.minute, datePickerState.getSelectedDate())
            dateMilliSeconds?.let {
                @SuppressLint("SimpleDateFormat") val timeFormat =
                    SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
                val timeTag = java.lang.String.format(
                    Locale.getDefault(), "%s", timeFormat.format(it)
                )
                Timber.i("timeTag: $timeTag")
                onConfirm(it, timeTag)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .background(Color.LightGray)
        ) {
            DatePicker(
                state = datePickerState,
            )
            TimePicker(
                state = timePickerState,
            )
        }
    }
}

@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
/*
        title = {
            OutlinedButton(onClick = { onConfirm() }) {
                Text(stringResource(R.string.from_beginning))
            }
        },
 */
        text = { content() }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun StartTimeDialogPreview() {
    RamaniTheme {
        StartTimeDialog(
            onConfirm = { _, _ -> },
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePagerDialog(
    showDialog: Boolean,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )
    val datePickerState = rememberDatePickerState()
    datePickerState.selectedDateMillis = System.currentTimeMillis()
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Define number of pages for ViewPager
                    val pageCount = 2
                    val pagerState = rememberPagerState(pageCount = { pageCount })

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        // Content for each page
                        when(page) {
                            0 -> {
                                DatePicker(
                                    state = datePickerState,
                                )
                            }
                            1 -> {
                                TimePicker(modifier = Modifier.align(alignment = Alignment.CenterHorizontally).fillMaxWidth(),
                                    state = timePickerState,
                                )
                            }
                        }
                    }
                    Row(
                        Modifier
                            .wrapContentHeight()
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(16.dp)
                            )
                        }
                    }

                    Row {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(16.dp)
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                val dateMilliSeconds = convertTimeToLong(
                                    timePickerState.hour,
                                    timePickerState.minute, datePickerState.getSelectedDate()
                                )
                                dateMilliSeconds?.let {
                                    @SuppressLint("SimpleDateFormat") val timeFormat =
                                        SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
                                    val timeTag = java.lang.String.format(
                                        Locale.getDefault(), "%s", timeFormat.format(it)
                                    )
                                    Timber.i("timeTag: $timeTag")
                                    onConfirm(it, timeTag)
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(16.dp)
                        ) {
                            Text("Ok")
                        }
                    }
                }
            }
        }
    }
}
