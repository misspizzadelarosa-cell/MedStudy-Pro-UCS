package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_guides")
data class StudyGuideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String,
    val isCustom: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
