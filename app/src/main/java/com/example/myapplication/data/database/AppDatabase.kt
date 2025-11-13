package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.entities.*
import com.example.myapplication.data.dao.*

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
        TelegramMessageEntity::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
