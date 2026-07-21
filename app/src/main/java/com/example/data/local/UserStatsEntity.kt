package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 45,
    val level: Int = 1,
    val streakDays: Int = 1,
    val rankTitle: String = "Iniciando Histopatología",
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
