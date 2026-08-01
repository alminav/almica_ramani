package com.almica.ramani

import androidx.compose.runtime.staticCompositionLocalOf
import me.ibrahimsn.library.LiveSharedPreferences

val LocalLiveSharedPreferences = staticCompositionLocalOf<LiveSharedPreferences> {
    error("No LiveSharedPreferences provided")
}
