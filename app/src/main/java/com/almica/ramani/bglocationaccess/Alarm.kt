package com.almica.ramani.bglocationaccess

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Alarm(

    @PrimaryKey(true)
    var id:Long,
    val title: String,
    val message: String,
    val scheduleAt:Long
)
