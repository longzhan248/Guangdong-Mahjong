package com.example.engine

import com.example.model.MahjongTile
import com.example.model.MahjongMeld
import com.example.model.TileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MahjongEvaluator {

    // Representation of win breakdown
    data class WinResult(
        val isWin: Boolean,
        val handName: String = "",
        val fan: Int = 0,
        val details: List<String> = emptyList()
    )

    // Check if the current hand + new tile yields a win (Hu)
    suspend fun checkHu(
        handTiles: List<MahjongTile>,
        declaredMelds: List<MahjongMeld>,
        winningTile: MahjongTile,
        isSelfDraw: Boolean,
        isGangShangKaiHua: Boolean = false,
        wildTileType: TileType? = null,
        wildTileValue: Int? = null
    ): WinResult = withContext(Dispatchers.Default) {
        // Form the full combination
        val fullHand = (handTiles + winningTile).sorted()

        // 1. Thirteen Orphans (十三幺) - Must be 14 tiles (no declared melds)
        if (declaredMelds.isEmpty() && isThirteenOrphans(fullHand)) {
            return@withContext WinResult(
                isWin = true,
                handName = "十三幺",
                fan = 10,
                details = listOf("十三幺 (10番)")
            )
        }

        // 2. Seven Pairs (七对) - Must be 14 tiles (no declared melds)
        if (declaredMelds.isEmpty() && isSevenPairs(fullHand, wildTileType, wildTileValue)) {
            val isQingYiSe = isQingYiSe(fullHand, declaredMelds, wildTileType, wildTileValue)
            var fan = 4
            val details = mutableListOf<String>()
            var name = "七对"
            if (isQingYiSe) {
                fan += 4
                details.add("清一色 (+4番)")
                name = "清一色七对"
            } else if (isHunYiSe(fullHand, declaredMelds, wildTileType, wildTileValue)) {
                fan += 2
                details.add("混一色 (+2番)")
                name = "混一色七对"
            }
            details.add("七对 (4番)")
            if (isSelfDraw) {
                fan *= 2
                details.add("自摸 (番数翻倍)")
            }
            if (isGangShangKaiHua) {
                fan += 1
                details.add("杠上开花 (+1番)")
            }
            return@withContext WinResult(true, name, fan, details)
        }

        // 3. Standard Hu
        val standardWin = checkStandardHu(fullHand, wildTileType, wildTileValue)
        if (standardWin) {
            val details = mutableListOf<String>()
            var fan = 1
            var name = "鸡胡"

            val isQing = isQingYiSe(fullHand, declaredMelds, wildTileType, wildTileValue)
            val isHun = isHunYiSe(fullHand, declaredMelds, wildTileType, wildTileValue)
            val isPengPeng = isPengPengHu(fullHand, declaredMelds, wildTileType, wildTileValue)

            if (isQing) {
                fan += 4
                details.add("清一色 (+4番)")
                name = "清一色"
            } else if (isHun) {
                fan += 2
                details.add("混一色 (+2番)")
                name = "混一色"
            }

            if (isPengPeng) {
                fan += 2
                details.add("碰碰胡 (+2番)")
                name = if (isQing) "清一色碰碰胡" else if (isHun) "混一色碰碰胡" else "碰碰胡"
            } else {
                details.add("平胡 (1番)")
            }

            if (isSelfDraw) {
                fan *= 2
                details.add("自摸 (番数翻倍)")
            }
            if (isGangShangKaiHua) {
                fan += 1
                details.add("杠上开花 (+1番)")
            }

            return@withContext WinResult(true, name, fan, details)
        }

        return@withContext WinResult(false)
    }

    // Helper extension to identify if a tile is a Wild/Joker tile
    private fun MahjongTile.isWild(wildType: TileType?, wildVal: Int?): Boolean {
        return wildType != null && wildVal != null && this.type == wildType && this.value == wildVal
    }

    // Checking Seven Pairs (七对) with Wild Cards
    private fun isSevenPairs(tiles: List<MahjongTile>, wildType: TileType?, wildVal: Int?): Boolean {
        if (tiles.size != 14) return false
        val (wilds, normals) = tiles.partition { it.isWild(wildType, wildVal) }
        val wildCount = wilds.size

        val groups = normals.groupBy { it.type.toString() + "_" + it.value }
        val needed = groups.values.sumOf { it.size % 2 }

        return wildCount >= needed && (wildCount - needed) % 2 == 0
    }

    // Checking Thirteen Orphans (十三幺 - Standard implementation without wild substitution)
    private fun isThirteenOrphans(tiles: List<MahjongTile>): Boolean {
        if (tiles.size != 14) return false
        
        val required = listOf(
            Pair(TileType.WAN, 1), Pair(TileType.WAN, 9),
            Pair(TileType.TONG, 1), Pair(TileType.TONG, 9),
            Pair(TileType.TIAO, 1), Pair(TileType.TIAO, 9),
            Pair(TileType.WIND, 1), Pair(TileType.WIND, 2), Pair(TileType.WIND, 3), Pair(TileType.WIND, 4),
            Pair(TileType.DRAGON, 1), Pair(TileType.DRAGON, 2), Pair(TileType.DRAGON, 3)
        )

        val grouped = tiles.groupBy { Pair(it.type, it.value) }
        if (grouped.size != 13) return false

        return required.all { req -> grouped.containsKey(req) && grouped[req]!!.isNotEmpty() }
    }

    // Checking Qing Yi Se (清一色 - Hand consists of ONLY ONE suit, NO Winds or Dragons)
    private fun isQingYiSe(tiles: List<MahjongTile>, melds: List<MahjongMeld>, wildType: TileType?, wildVal: Int?): Boolean {
        val allTiles = (tiles + melds.flatMap { it.tiles }).filter { !it.isWild(wildType, wildVal) }
        if (allTiles.isEmpty()) return true
        val firstMainSuit = allTiles.firstOrNull { it.type in listOf(TileType.WAN, TileType.TONG, TileType.TIAO) }?.type ?: return false
        return allTiles.all { it.type == firstMainSuit }
    }

    // Checking Hun Yi Se (混一色 - Hand consists of ONLY ONE suit AND Winds/Dragons)
    private fun isHunYiSe(tiles: List<MahjongTile>, melds: List<MahjongMeld>, wildType: TileType?, wildVal: Int?): Boolean {
        val allTiles = (tiles + melds.flatMap { it.tiles }).filter { !it.isWild(wildType, wildVal) }
        if (allTiles.isEmpty()) return false
        
        val suits = allTiles.map { it.type }.distinct()
        val numericSuits = suits.filter { it in listOf(TileType.WAN, TileType.TONG, TileType.TIAO) }
        val letterSuits = suits.filter { it in listOf(TileType.WIND, TileType.DRAGON) }
        
        return numericSuits.size == 1 && letterSuits.isNotEmpty()
    }

    // Checking Peng Peng Hu (碰碰胡 - all melds in hand/revealed are triplets + 1 pair)
    private fun isPengPengHu(tiles: List<MahjongTile>, melds: List<MahjongMeld>, wildType: TileType?, wildVal: Int?): Boolean {
        val (wilds, normals) = tiles.partition { it.isWild(wildType, wildVal) }
        val wildCount = wilds.size
        
        val uniqueTiles = normals.map { Pair(it.type, it.value) }.distinct()
        val memo = mutableMapOf<String, Boolean>()
        
        // Try each candidate pair
        for (pairCandidate in uniqueTiles) {
            val candidateCount = normals.count { it.type == pairCandidate.first && it.value == pairCandidate.second }
            if (candidateCount >= 2) {
                val remainingNormal = normals.toMutableList()
                removeOneEquivalent(remainingNormal, pairCandidate)
                removeOneEquivalent(remainingNormal, pairCandidate)
                
                if (canPartitionTripletsOnlyWithWild(remainingNormal.sorted(), wildCount, memo)) {
                    return true
                }
            }
        }
        
        if (wildCount >= 1) {
            for (pairCandidate in uniqueTiles) {
                val remainingNormal = normals.toMutableList()
                removeOneEquivalent(remainingNormal, pairCandidate)
                
                if (canPartitionTripletsOnlyWithWild(remainingNormal.sorted(), wildCount - 1, memo)) {
                    return true
                }
            }
        }
        
        if (wildCount >= 2) {
            if (canPartitionTripletsOnlyWithWild(normals.sorted(), wildCount - 2, memo)) {
                return true
            }
        }
        return false
    }

    private fun getCacheKey(sortedNormals: List<MahjongTile>, wildCount: Int): String {
        val sb = StringBuilder()
        for (tile in sortedNormals) {
            sb.append(tile.type.ordinal).append('_').append(tile.value).append(',')
        }
        sb.append(':').append(wildCount)
        return sb.toString()
    }

    private fun canPartitionTripletsOnlyWithWild(
        sortedNormals: List<MahjongTile>, 
        wildCount: Int,
        memo: MutableMap<String, Boolean>
    ): Boolean {
        if (sortedNormals.isEmpty()) {
            return wildCount % 3 == 0 && wildCount >= 0
        }
        val key = getCacheKey(sortedNormals, wildCount)
        memo[key]?.let { return it }

        val first = sortedNormals[0]
        val firstCount = sortedNormals.count { it.isSameTile(first) }
        
        // Scenario A1: 3 of 'first'
        if (firstCount >= 3) {
            val remaining = sortedNormals.toMutableList()
            repeat(3) { removeOneEquivalent(remaining, first) }
            if (canPartitionTripletsOnlyWithWild(remaining, wildCount, memo)) {
                memo[key] = true
                return true
            }
        }
        // Scenario A2: 2 of 'first' + 1 wild card
        if (firstCount >= 2 && wildCount >= 1) {
            val remaining = sortedNormals.toMutableList()
            repeat(2) { removeOneEquivalent(remaining, first) }
            if (canPartitionTripletsOnlyWithWild(remaining, wildCount - 1, memo)) {
                memo[key] = true
                return true
            }
        }
        // Scenario A3: 1 of 'first' + 2 wild cards
        if (wildCount >= 2) {
            val remaining = sortedNormals.toMutableList()
            removeOneEquivalent(remaining, first)
            if (canPartitionTripletsOnlyWithWild(remaining, wildCount - 2, memo)) {
                memo[key] = true
                return true
            }
        }
        
        memo[key] = false
        return false
    }

    // Standard recursive Hu checker with wild card support
    private fun checkStandardHu(tiles: List<MahjongTile>, wildType: TileType?, wildVal: Int?): Boolean {
        val (wilds, normals) = tiles.partition { it.isWild(wildType, wildVal) }
        val wildCount = wilds.size
        
        val uniqueTiles = normals.map { Pair(it.type, it.value) }.distinct()
        val memo = mutableMapOf<String, Boolean>()
        
        // 1. Try pair candidates from normal tiles
        for (pairCandidate in uniqueTiles) {
            val candidateCount = normals.count { it.type == pairCandidate.first && it.value == pairCandidate.second }
            if (candidateCount >= 2) {
                val remainingNormal = normals.toMutableList()
                removeOneEquivalent(remainingNormal, pairCandidate)
                removeOneEquivalent(remainingNormal, pairCandidate)
                
                if (canPartitionIntoMeldsWithWild(remainingNormal.sorted(), wildCount, memo)) {
                    return true
                }
            }
        }
        
        // 2. Try pair candidate as 1 normal tile + 1 wild card
        if (wildCount >= 1) {
            for (pairCandidate in uniqueTiles) {
                val remainingNormal = normals.toMutableList()
                removeOneEquivalent(remainingNormal, pairCandidate)
                
                if (canPartitionIntoMeldsWithWild(remainingNormal.sorted(), wildCount - 1, memo)) {
                    return true
                }
            }
        }
        
        // 3. Try pair as two wild cards
        if (wildCount >= 2) {
            if (canPartitionIntoMeldsWithWild(normals.sorted(), wildCount - 2, memo)) {
                return true
            }
        }
        
        if (normals.isEmpty() && wildCount >= 2) {
            if ((wildCount - 2) % 3 == 0) return true
        }

        return false
    }

    private fun canPartitionIntoMeldsWithWild(
        sortedNormals: List<MahjongTile>, 
        wildCount: Int,
        memo: MutableMap<String, Boolean>
    ): Boolean {
        if (sortedNormals.isEmpty()) {
            return wildCount % 3 == 0 && wildCount >= 0
        }
        val key = getCacheKey(sortedNormals, wildCount)
        memo[key]?.let { return it }
        
        val first = sortedNormals[0]
        val firstCount = sortedNormals.count { it.isSameTile(first) }
        
        // Try Option A: Form a Triplet containing 'first'
        if (firstCount >= 3) {
            val remaining = sortedNormals.toMutableList()
            repeat(3) { removeOneEquivalent(remaining, first) }
            if (canPartitionIntoMeldsWithWild(remaining, wildCount, memo)) {
                memo[key] = true
                return true
            }
        }
        if (firstCount >= 2 && wildCount >= 1) {
            val remaining = sortedNormals.toMutableList()
            repeat(2) { removeOneEquivalent(remaining, first) }
            if (canPartitionIntoMeldsWithWild(remaining, wildCount - 1, memo)) {
                memo[key] = true
                return true
            }
        }
        if (wildCount >= 2) {
            val remaining = sortedNormals.toMutableList()
            removeOneEquivalent(remaining, first)
            if (canPartitionIntoMeldsWithWild(remaining, wildCount - 2, memo)) {
                memo[key] = true
                return true
            }
        }
        
        // Try Option B: Form a Sequence containing 'first' (only WAN, TONG, TIAO)
        if (first.type in listOf(TileType.WAN, TileType.TONG, TileType.TIAO)) {
            val val1 = first.value + 1
            val val2 = first.value + 2
            
            val next1 = first.copy(value = val1)
            val next2 = first.copy(value = val2)
            
            val has1 = sortedNormals.any { it.isSameTile(next1) }
            val has2 = sortedNormals.any { it.isSameTile(next2) }
            
            if (has1 && has2) {
                val remaining = sortedNormals.toMutableList()
                removeOneEquivalent(remaining, first)
                removeOneEquivalent(remaining, next1)
                removeOneEquivalent(remaining, next2)
                if (canPartitionIntoMeldsWithWild(remaining, wildCount, memo)) {
                    memo[key] = true
                    return true
                }
            }
            if (has1 && wildCount >= 1) {
                val remaining = sortedNormals.toMutableList()
                removeOneEquivalent(remaining, first)
                removeOneEquivalent(remaining, next1)
                if (canPartitionIntoMeldsWithWild(remaining, wildCount - 1, memo)) {
                    memo[key] = true
                    return true
                }
            }
            if (has2 && wildCount >= 1) {
                val remaining = sortedNormals.toMutableList()
                removeOneEquivalent(remaining, first)
                removeOneEquivalent(remaining, next2)
                if (canPartitionIntoMeldsWithWild(remaining, wildCount - 1, memo)) {
                    memo[key] = true
                    return true
                }
            }
            if (wildCount >= 2) {
                val remaining = sortedNormals.toMutableList()
                removeOneEquivalent(remaining, first)
                if (canPartitionIntoMeldsWithWild(remaining, wildCount - 2, memo)) {
                    memo[key] = true
                    return true
                }
            }
        }
        
        memo[key] = false
        return false
    }

    private fun removeOneEquivalent(list: MutableList<MahjongTile>, target: MahjongTile) {
        val index = list.indexOfFirst { it.isSameTile(target) }
        if (index != -1) {
            list.removeAt(index)
        }
    }

    private fun removeOneEquivalent(list: MutableList<MahjongTile>, target: Pair<TileType, Int>) {
        val index = list.indexOfFirst { it.type == target.first && it.value == target.second }
        if (index != -1) {
            list.removeAt(index)
        }
    }
}
