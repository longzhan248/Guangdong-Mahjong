package com.example.engine

import com.example.model.MahjongTile
import com.example.model.MahjongMeld
import com.example.model.TileType

object MahjongAI {

    // Decide what tile to discard from hand (14 tiles)
    fun selectDiscardTile(
        hand: List<MahjongTile>,
        wildTileType: TileType? = null,
        wildTileValue: Int? = null
    ): MahjongTile {
        if (hand.isEmpty()) throw IllegalArgumentException("Hand cannot be empty")
        if (hand.size == 1) return hand[0]

        // 1. Group tiles to find frequencies
        val tileCounts = hand.groupBy { Pair(it.type, it.value) }

        // Find tiles that are isolated
        // Let's rate each tile's "loneliness score" (higher score = more isolated = better candidate to discard)
        val ratedTiles = hand.map { tile ->
            var score = 0 // Baseline score

            // Check if this is a Wild/Joker tile
            if (wildTileType != null && wildTileValue != null && tile.type == wildTileType && tile.value == wildTileValue) {
                // Wild card is extremely valuable, dramatically decrease its discard rating!
                score -= 2000
            }

            val count = tileCounts[Pair(tile.type, tile.value)]?.size ?: 1

            if (count == 4) {
                // Keep for Gang
                score -= 100
            } else if (count == 3) {
                // Completed triplet, do NOT discard
                score -= 80
            } else if (count == 2) {
                // Pair, good for Eyes, lower discard priority
                score -= 30
            }

            // Wind and Dragon evaluation
            if (tile.type == TileType.WIND || tile.type == TileType.DRAGON) {
                if (count == 1) {
                    // Single Winds & Dragons are highly isolated because they cannot form sequences!
                    score += 50
                }
            } else {
                // Character, Bamboo, Dot - Check sequence connection
                val adjacent1 = hand.any { it.type == tile.type && it.value == tile.value - 1 }
                val adjacent2 = hand.any { it.type == tile.type && it.value == tile.value + 1 }
                val gap1 = hand.any { it.type == tile.type && it.value == tile.value - 2 }
                val gap2 = hand.any { it.type == tile.type && it.value == tile.value + 2 }

                if (!adjacent1 && !adjacent2 && !gap1 && !gap2) {
                    // Completely isolated number tile
                    score += 25
                } else if (adjacent1 || adjacent2) {
                    // Has direct neighbor, good for sequence
                    score -= 15
                } else if (gap1 || gap2) {
                    // Has gap neighbor (e.g. 3 and 5), okay for sequence
                    score -= 5
                }
            }

            // Add slight randomness so the AI doesn't always perform identically
            val randomOffset = (0..5).random()
            Pair(tile, score + randomOffset)
        }

        // Sort descending by loneliness score
        val sortedByScore = ratedTiles.sortedByDescending { it.second }
        return sortedByScore.first().first
    }

    // Decide if AI should Peng a tile discarded by another player
    fun shouldPeng(hand: List<MahjongTile>, discardedTile: MahjongTile): Boolean {
        // AI will Peng if they have exactly 2 of this tile in hand
        val matchingCount = hand.count { it.isSameTile(discardedTile) }
        return matchingCount == 2
    }

    // Decide if AI should Gang on a tile discarded by another player, or self-drawn
    fun shouldGang(hand: List<MahjongTile>, targetTile: MahjongTile, isSelfDraw: Boolean): Boolean {
        val matchingCount = hand.count { it.isSameTile(targetTile) }
        return if (isSelfDraw) {
            // Self-draw Gang can be standard Angang (4 in hand) or Bugang (already Peng'ed, draw the 4th, checked in VM)
            matchingCount == 4
        } else {
            // Minggang on opponent discard (3 in hand)
            matchingCount == 3
        }
    }
}
