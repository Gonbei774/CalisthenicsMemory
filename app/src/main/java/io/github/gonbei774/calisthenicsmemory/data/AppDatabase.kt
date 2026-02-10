// AppDatabase.kt
package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercise::class, TrainingRecord::class, ExerciseGroup::class, TodoTask::class, Program::class, ProgramExercise::class, ProgramLoop::class, IntervalProgram::class, IntervalProgramExercise::class, IntervalRecord::class],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun trainingRecordDao(): TrainingRecordDao
    abstract fun exerciseGroupDao(): ExerciseGroupDao
    abstract fun todoTaskDao(): TodoTaskDao
    abstract fun programDao(): ProgramDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun programLoopDao(): ProgramLoopDao
    abstract fun intervalProgramDao(): IntervalProgramDao
    abstract fun intervalProgramExerciseDao(): IntervalProgramExerciseDao
    abstract fun intervalRecordDao(): IntervalRecordDao

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
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
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

        // マイグレーション 10 → 11: 距離・荷重トラッキング機能を追加
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Exercise テーブルに距離・荷重トラッキングフラグを追加
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN distanceTrackingEnabled INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN weightTrackingEnabled INTEGER NOT NULL DEFAULT 0"
                )

                // TrainingRecord テーブルに距離・荷重値を追加
                database.execSQL(
                    "ALTER TABLE training_records ADD COLUMN distanceCm INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE training_records ADD COLUMN weightG INTEGER"
                )
            }
        }

        // マイグレーション 11 → 12: To Do機能（TodoTaskテーブル追加）
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)
            }
        }

        // マイグレーション 12 → 13: Program機能（プログラム・種目テーブル追加）
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Program テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS programs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        timerMode INTEGER NOT NULL DEFAULT 0,
                        startInterval INTEGER NOT NULL DEFAULT 5
                    )
                """)

                // ProgramExercise テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        sets INTEGER NOT NULL DEFAULT 1,
                        targetValue INTEGER NOT NULL,
                        intervalSeconds INTEGER NOT NULL DEFAULT 60,
                        FOREIGN KEY (programId) REFERENCES programs(id) ON DELETE CASCADE,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)

                // インデックス作成
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_programId ON program_exercises(programId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_exerciseId ON program_exercises(exerciseId)")
            }
        }

        // マイグレーション 13 → 14: timerMode/startIntervalをSharedPreferencesへ移行
        // これらは「ユーザーの好み」であり「プログラムのコンテンツ」ではないため
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLiteではカラム削除が直接できないため、テーブル再作成が必要
                // 1. 新テーブル作成（timerMode, startIntervalなし）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS programs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """)

                // 2. データコピー（id, nameのみ）
                database.execSQL("""
                    INSERT INTO programs_new (id, name)
                    SELECT id, name FROM programs
                """)

                // 3. 旧テーブル削除
                database.execSQL("DROP TABLE programs")

                // 4. 新テーブルをリネーム
                database.execSQL("ALTER TABLE programs_new RENAME TO programs")
            }
        }

        // マイグレーション 14 → 15: ループ機能追加
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. ProgramLoop テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_loops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        rounds INTEGER NOT NULL,
                        restBetweenRounds INTEGER NOT NULL,
                        FOREIGN KEY (programId) REFERENCES programs(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_loops_programId ON program_loops(programId)")

                // 2. ProgramExercise テーブル再作成（ForeignKey追加のため）
                // SQLiteではALTER TABLEでForeignKeyを追加できないため、テーブル再作成が必要
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_exercises_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        sets INTEGER NOT NULL,
                        targetValue INTEGER NOT NULL,
                        intervalSeconds INTEGER NOT NULL,
                        loopId INTEGER DEFAULT NULL,
                        FOREIGN KEY (programId) REFERENCES programs(id) ON DELETE CASCADE,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE,
                        FOREIGN KEY (loopId) REFERENCES program_loops(id) ON DELETE CASCADE
                    )
                """)

                // データコピー
                database.execSQL("""
                    INSERT INTO program_exercises_new (id, programId, exerciseId, sortOrder, sets, targetValue, intervalSeconds, loopId)
                    SELECT id, programId, exerciseId, sortOrder, sets, targetValue, intervalSeconds, NULL
                    FROM program_exercises
                """)

                // 旧テーブル削除
                database.execSQL("DROP TABLE program_exercises")

                // 新テーブルをリネーム
                database.execSQL("ALTER TABLE program_exercises_new RENAME TO program_exercises")

                // インデックス作成
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_programId ON program_exercises(programId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_exerciseId ON program_exercises(exerciseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_loopId ON program_exercises(loopId)")
            }
        }

        // マイグレーション 15 → 16: アシストトラッキング機能を追加
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Exercise テーブルにアシストトラッキングフラグを追加
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN assistanceTrackingEnabled INTEGER NOT NULL DEFAULT 0"
                )

                // TrainingRecord テーブルにアシスト値を追加
                database.execSQL(
                    "ALTER TABLE training_records ADD COLUMN assistanceG INTEGER"
                )
            }
        }

        // マイグレーション 16 → 17: 種目の説明文フィールドを追加
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN description TEXT"
                )
            }
        }

        // マイグレーション 17 → 18: インターバルモード用テーブル追加
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // IntervalProgram テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS interval_programs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        workSeconds INTEGER NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        rounds INTEGER NOT NULL,
                        roundRestSeconds INTEGER NOT NULL
                    )
                """)

                // IntervalProgramExercise テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS interval_program_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY (programId) REFERENCES interval_programs(id) ON DELETE CASCADE,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_interval_program_exercises_programId ON interval_program_exercises(programId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_interval_program_exercises_exerciseId ON interval_program_exercises(exerciseId)")

                // IntervalRecord テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS interval_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programName TEXT NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        workSeconds INTEGER NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        rounds INTEGER NOT NULL,
                        roundRestSeconds INTEGER NOT NULL,
                        completedRounds INTEGER NOT NULL,
                        completedExercisesInLastRound INTEGER NOT NULL,
                        exercisesJson TEXT NOT NULL,
                        comment TEXT
                    )
                """)
            }
        }
    }
}