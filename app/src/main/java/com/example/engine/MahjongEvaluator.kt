package com.example.engine

import com.example.model.MahjongTile
import com.example.model.MahjongMeld
import com.example.model.TileType

object MahjongEvaluator {

    // Representation of win breakdown
    data class WinResult(
        val isWin: Boolean,
        val handName: String = "",
        val fan: Int = 0,
        val details: List<String> = emptyList()
    )

    // Check if the current hand + new tile yields a win (Hu)
    fun checkHu(
        handTiles: List<MahjongTile>,
        declaredMelds: List<MahjongMeld>,
        winningTile: MahjongTile,
        isSelfDraw: Boolean,
        isGangShangKaiHua: Boolean = false
    ): WinResult {
        // Form the full combination
        val fullHand = (handTiles + winningTile).sorted()

        // 1. Thirteen Orphans (十三幺) - Must be 14 tiles (no declared melds)
        if (declaredMelds.isEmpty() && isThirteenOrphans(fullHand)) {
            return WinResult(
                isWin = true,
                handName = "十三幺",
                fan = 10,
                details = listOf("十三幺 (10番)")
            )
        }

        // 2. Seven Pairs (七对) - Must be 14 tiles (no declared melds)
        if (declaredMelds.isEmpty() && isSevenPairs(fullHand)) {
            val isQingYiSe = isQingYiSe(fullHand, declaredMelds)
            var fan = 4
            val details = mutableListOf<String>()
            var name = "七对"
            if (isQingYiSe) {
                fan += 4
                details.add("清一色 (+4番)")
                name = "清一色七对"
            } else if (isHunYiSe(fullHand, declaredMelds)) {
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
            return WinResult(true, name, fan, details)
        }

        // 3. Standard Hu
        val standardWin = checkStandardHu(fullHand)
        if (standardWin) {
            val details = mutableListOf<String>()
            var fan = 1
            var name = "鸡胡"

            val isQing = isQingYiSe(fullHand, declaredMelds)
            val isHun = isHunYiSe(fullHand, declaredMelds)
            val isPengPeng = isPengPengHu(fullHand, declaredMelds)

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

            return WinResult(true, name, fan, details)
        }

        return WinResult(false)
    }

    // Checking Seven Pairs (七对)
    private fun isSevenPairs(tiles: List<MahjongTile>): Boolean {
        if (tiles.size != 14) return false
        val groups = tiles.groupBy { it.type.toString() + "_" + it.value }
        return groups.values.all { it.size == 2 || it.size == 4 }
    }

    // Checking Thirteen Orphans (十三幺 - 1,9 of Wan/Tiao/Tong, East/South/West/North, Red/Green/White, +1 duplicate)
    private fun isThirteenOrphans(tiles: List<MahjongTile>): Boolean {
        if (tiles.size != 14) return false
        
        val required = listOf(
            Pair(TileType.WAN, 1), Pair(TileType.WAN, 9),
            Pair(TileType.TONG, 1), Pair(TileType.TONG, 9),
            Pair(TileType.TIAO, 1), Pair(TileType.TIAO, 9),
            Pair(TileType.WIND, 1), Pair(TileType.WIND, 2), Pair(TileType.WIND, 3), Pair(TileType.WIND, 4),
            Pair(TileType.DRAGON, 1), Pair(TileType.DRAGON, 2), Pair(TileType.DRAGON, 3)
        )

        // Count match with required orphans
        val grouped = tiles.groupBy { Pair(it.type, it.value) }
        if (grouped.size != 13) return false // Must have exactly 13 unique types of orphans

        return required.all { req -> grouped.containsKey(req) && grouped[req]!!.isNotEmpty() }
    }

    // Checking Qing Yi Se (清一色 - Hand consists of ONLY ONE suit (Wan, Tong, or Tiao), NO Winds or Dragons)
    private fun isQingYiSe(tiles: List<MahjongTile>, melds: List<MahjongMeld>): Boolean {
        val allTiles = tiles + melds.flatMap { it.tiles }
        if (allTiles.isEmpty()) return false
        val firstMainSuit = allTiles.firstOrNull { it.type in listOf(TileType.WAN, TileType.TONG, TileType.TIAO) }?.type ?: return false
        return allTiles.all { it.type == firstMainSuit }
    }

    // Checking Hun Yi Se (混一色 - Hand consists of ONLY ONE suit (Wan, Tong or Tiao) AND Winds/Dragons)
    private fun isHunYiSe(tiles: List<MahjongTile>, melds: List<MahjongMeld>): Boolean {
        val allTiles = tiles + melds.flatMap { it.tiles }
        if (allTiles.isEmpty()) return false
        
        val suits = allTiles.map { it.type }.distinct()
        val numericSuits = suits.filter { it in listOf(TileType.WAN, TileType.TONG, TileType.TIAO) }
        val letterSuits = suits.filter { it in listOf(TileType.WIND, TileType.DRAGON) }
        
        return numericSuits.size == 1 && letterSuits.isNotEmpty()
    }

    // Checking Peng Peng Hu (碰碰胡 - all melds in hand/revealed are triplets/quads + 1 pair)
    private fun isPengPengHu(tiles: List<MahjongTile>, melds: List<MahjongMeld>): Boolean {
        // If there is any sequence (顺子) in Hand, it's not Peng Peng Hu!
        // So standard partition into melds should ONLY use triplets.
        // Let's check: can partition using ONLY triplets after removing a pair?
        
        val uniqueTiles = tiles.map { Pair(it.type, it.value) }.distinct()
        for (pairCandidate in uniqueTiles) {
            val candidateCount = tiles.count { it.type == pairCandidate.first && it.value == pairCandidate.second }
            if (candidateCount >= 2) {
                val remaining = tiles.toMutableList()
                removeOneEquivalent(remaining, pairCandidate)
                removeOneEquivalent(remaining, pairCandidate)
                
                // Now check if remaining can be perfectly partitioned into triplets ONLY
                if (canPartitionTripletsOnly(remaining)) {
                    return true
                }
            }
        }
        return false
    }

    private fun canPartitionTripletsOnly(tiles: List<MahjongTile>): Boolean {
        if (tiles.isEmpty()) return true
        val grouped = tiles.groupBy { Pair(it.type, it.value) }
        return grouped.values.all { it.size == 3 || it.size == 4 || it.size == 0 } // A quad can also act as valid triplet + discarded or just 4 of a kind is fine
    }

    // Standard recursive Hu checker
    private fun checkStandardHu(tiles: List<MahjongTile>): Boolean {
        val uniqueTiles = tiles.map { Pair(it.type, it.value) }.distinct()
        
        for (pairCandidate in uniqueTiles) {
            val candidateCount = tiles.count { it.type == pairCandidate.first && it.value == pairCandidate.second }
            if (candidateCount >= 2) {
                val remaining = tiles.toMutableList()
                removeOneEquivalent(remaining, pairCandidate)
                removeOneEquivalent(remaining, pairCandidate)
                
                if (canPartitionIntoMelds(remaining)) {
                    return true
                }
            }
        }
        return false
    }

    private fun canPartitionIntoMelds(tiles: List<MahjongTile>): Boolean {
        if (tiles.isEmpty()) return true
        
        val sorted = tiles.sorted()
        val first = sorted[0]
        
        // 1. Try Triplet (碰)
        val sameCount = sorted.count { it.isSameTile(first) }
        if (sameCount >= 3) {
            val remaining = sorted.toMutableList()
            var removed = 0
            val iterator = remaining.iterator()
            while (iterator.hasNext() && removed < 3) {
                if (iterator.next().isSameTile(first)) {
                    iterator.remove()
                    removed++
                }
            }
            if (canPartitionIntoMelds(remaining)) return true
        }
        
        // 2. Try Sequence (顺子) - only valid for Wan, Tong, Tiao
        if (first.type in listOf(TileType.WAN, TileType.TONG, TileType.TIAO)) {
            val next1 = first.copy(value = first.value + 1)
            val next2 = first.copy(value = first.value + 2)
            
            val hasNext1 = sorted.any { it.isSameTile(next1) }
            val hasNext2 = sorted.any { it.isSameTile(next2) }
            
            if (hasNext1 && hasNext2) {
                val remaining = sorted.toMutableList()
                removeOneEquivalent(remaining, Pair(first.type, first.value))
                removeOneEquivalent(remaining, Pair(next1.type, next1.value))
                removeOneEquivalent(remaining, Pair(next2.type, next2.value))
                
                if (canPartitionIntoMelds(remaining)) return true
            }
        }
        
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
