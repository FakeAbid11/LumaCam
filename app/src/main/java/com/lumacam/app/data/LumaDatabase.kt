package com.lumacam.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CaptureRecord::class], version = 1, exportSchema = false)
abstract class LumaDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
}
