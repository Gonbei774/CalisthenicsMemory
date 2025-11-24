// AppDatabase.kt
package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercise::class, TrainingRecord::class, ExerciseGroup::class],
    version = 10,  // ← 変更: 9 → 10（displayOrder, restInterval, repDuration追加）
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
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // マイグレーション 9 → 10: displayOrder, restInterval, repDuration を追加
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. displayOrder フィールドを追加（並び替え機能用）
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0"
                )

                // 2. restInterval フィールドを追加（タイマー設定機能用）
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN restInterval INTEGER"
                )

                // 3. repDuration フィールドを追加（タイマー設定機能用）
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN repDuration INTEGER"
                )

                // 4. displayOrder の初期値を設定
                //    グループ内で sortOrder の昇順に従って 0, 1, 2... を割り当て
                //    グループ外は name の昇順で連番を割り当て

                // グループ内の種目に連番を割り当て
                database.execSQL("""
                    UPDATE exercises
                    SET displayOrder = (
                        SELECT COUNT(*)
                        FROM exercises e2
                        WHERE e2.`group` IS NOT NULL
                        AND e2.`group` = exercises.`group`
                        AND (
                            e2.sortOrder < exercises.sortOrder
                            OR (e2.sortOrder = exercises.sortOrder AND e2.id < exercises.id)
                        )
                    )
                    WHERE exercises.`group` IS NOT NULL
                """)

                // グループ外の種目に連番を割り当て（名前順）
                database.execSQL("""
                    UPDATE exercises
                    SET displayOrder = (
                        SELECT COUNT(*)
                        FROM exercises e2
                        WHERE e2.`group` IS NULL
                        AND e2.name < exercises.name
                    )
                    WHERE exercises.`group` IS NULL
                """)
            }
        }
    }
}