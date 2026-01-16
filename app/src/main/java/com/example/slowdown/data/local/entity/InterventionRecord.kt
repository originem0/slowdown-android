package com.example.slowdown.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intervention_records")
data class InterventionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val interventionType: String,
    val userChoice: String,
    val countdownDuration: Int,
    val actualWaitTime: Int
)
