package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CompanionRepository

class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "companion-db"
    ).fallbackToDestructiveMigration().build()

    val companionRepository by lazy { CompanionRepository(db.companionDao()) }
}
