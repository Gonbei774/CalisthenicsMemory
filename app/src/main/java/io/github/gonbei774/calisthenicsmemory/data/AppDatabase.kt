// AppDatabase.kt
package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, TrainingRecord::class, ExerciseGroup::class],
    version = 9,  // ← 変更: 8 → 9（Exercise.isFavorite追加）
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun trainingRecordDao(): TrainingRecordDao
    abstract fun exerciseGroupDao(): ExerciseGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bodyweight_trainer_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}