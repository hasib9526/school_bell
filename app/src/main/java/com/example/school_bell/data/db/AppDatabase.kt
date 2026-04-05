package com.example.school_bell.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.school_bell.data.db.dao.AzanTimeDao
import com.example.school_bell.data.db.dao.BellScheduleDao
import com.example.school_bell.data.db.entities.AzanTime
import com.example.school_bell.data.db.entities.BellSchedule
import com.example.school_bell.data.db.entities.RoutineType

class RoutineTypeConverter {
    @TypeConverter
    fun fromRoutineType(value: RoutineType): String = value.name

    @TypeConverter
    fun toRoutineType(value: String): RoutineType = RoutineType.valueOf(value)
}

@Database(
    entities = [BellSchedule::class, AzanTime::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoutineTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bellScheduleDao(): BellScheduleDao
    abstract fun azanTimeDao(): AzanTimeDao

    companion object {
        private const val DATABASE_NAME = "school_bell_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
