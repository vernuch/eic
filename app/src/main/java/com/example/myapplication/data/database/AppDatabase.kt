package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.entities.*

@Database(
    entities = [
        TeacherEntity::class,
        SubjectEntity::class,
        ScheduleEntity::class,
        TaskEntity::class,
        TaskAttachmentEntity::class,
        ExamEntity::class,
        MessageEntity::class,
        SettingsEntity::class,
        IntegrationEntity::class,
        ReplacementEntity::class,
        FileEntity::class,
        TelegramMessageEntity::class,
        StudentInfoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun teacherDao(): TeacherDao
    abstract fun subjectDao(): SubjectDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun taskDao(): TaskDao
    abstract fun taskAttachmentDao(): TaskAttachmentDao
    abstract fun examDao(): ExamDao
    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun integrationDao(): IntegrationDao
    abstract fun replacementDao(): ReplacementDao
    abstract fun fileDao(): FileDao
    abstract fun telegramDao(): TelegramDao
    abstract fun studentInfoDao(): StudentInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
