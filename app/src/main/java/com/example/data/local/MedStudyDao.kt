package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedStudyDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserStats(stats: UserStatsEntity)

    @Query("SELECT * FROM study_guides ORDER BY timestamp DESC")
    fun getAllGuides(): Flow<List<StudyGuideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuide(guide: StudyGuideEntity)

    @Query("SELECT * FROM exam_stats WHERE id = 1")
    fun getExamStats(): Flow<ExamStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateExamStats(stats: ExamStatsEntity)
}
