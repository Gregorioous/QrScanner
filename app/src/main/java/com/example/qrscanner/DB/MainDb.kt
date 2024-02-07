package com.example.qrscanner.DB

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Product::class],
    version = 1
)
abstract class MainDb : RoomDatabase() {
    abstract val dao: Dao
}