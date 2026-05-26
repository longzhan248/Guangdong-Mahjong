package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRecordRepository(private val gameRecordDao: GameRecordDao) {
    val allRecords: Flow<List<GameRecord>> = gameRecordDao.getAllRecords()

    suspend fun insertRecord(record: GameRecord) {
        gameRecordDao.insertRecord(record)
    }

    suspend fun clearAllRecords() {
        gameRecordDao.clearAllRecords()
    }
}
