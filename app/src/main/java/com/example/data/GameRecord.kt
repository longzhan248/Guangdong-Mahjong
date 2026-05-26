package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val winnerName: String,
    val winnerEmoji: String,
    val winType: String, // "自摸" or "点炮胡" or "流局"
    val handName: String, // "碰碰胡" etc
    val fan: Int,
    val chipChangePlayer0: Int, // User chips change
    val chipChangePlayer1: Int, // AI 1 chips change
    val chipChangePlayer2: Int, // AI 2 chips change
    val chipChangePlayer3: Int, // AI 3 chips change
    val finalBalancePlayer0: Int // User chips after this match
)
