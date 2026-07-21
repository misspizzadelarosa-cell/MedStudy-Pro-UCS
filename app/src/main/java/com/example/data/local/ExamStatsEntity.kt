package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_stats")
data class ExamStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalQuestionsAnswered: Int = 0,
    val correctAnswers: Int = 0,
    val puzzleMatchesCompleted: Int = 0
)
