package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserStatsEntity::class, StudyGuideEntity::class, ExamStatsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MedStudyDatabase : RoomDatabase() {
    abstract fun medStudyDao(): MedStudyDao

    companion object {
        @Volatile
        private var INSTANCE: MedStudyDatabase? = null

        fun getDatabase(context: Context): MedStudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedStudyDatabase::class.java,
                    "medstudy_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
