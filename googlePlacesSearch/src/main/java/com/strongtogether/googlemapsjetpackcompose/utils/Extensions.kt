package com.strongtogether.googlemapsjetpackcompose.utils

import java.util.Locale

fun Double.format(digits: Int) = "%.${digits}f".format(Locale.ENGLISH, this)
