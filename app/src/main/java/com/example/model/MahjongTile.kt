package com.example.model

enum class TileType {
    WAN,   // 万 (Character)
    TONG,  // 筒 (Dot)
    TIAO,  // 条 (Bamboo)
    WIND,  // 风 (Wind)
    DRAGON // 箭 (Dragon)
}

data class MahjongTile(
    val type: TileType,
    val value: Int,
    val id: Int // Unique ID 0..135 to distinguish separate tiles
) : Comparable<MahjongTile> {

    val displayName: String
        get() = when (type) {
            TileType.WAN -> when (value) {
                1 -> "一万"
                2 -> "二万"
                3 -> "三万"
                4 -> "四万"
                5 -> "五万"
                6 -> "六万"
                7 -> "七万"
                8 -> "八万"
                9 -> "九万"
                else -> "${value}万"
            }
            TileType.TONG -> when (value) {
                1 -> "一筒"
                2 -> "二筒"
                3 -> "三筒"
                4 -> "四筒"
                5 -> "五筒"
                6 -> "六筒"
                7 -> "七筒"
                8 -> "八筒"
                9 -> "九筒"
                else -> "${value}筒"
            }
            TileType.TIAO -> when (value) {
                1 -> "一条"
                2 -> "二条"
                3 -> "三条"
                4 -> "四条"
                5 -> "五条"
                6 -> "六条"
                7 -> "七条"
                8 -> "八条"
                9 -> "九条"
                else -> "${value}条"
            }
            TileType.WIND -> when (value) {
                1 -> "东风"
                2 -> "南风"
                3 -> "西风"
                4 -> "北风"
                else -> "风"
            }
            TileType.DRAGON -> when (value) {
                1 -> "红中"
                2 -> "发财"
                3 -> "白板"
                else -> "箭"
            }
        }

    override fun compareTo(other: MahjongTile): Int {
        if (this.type != other.type) {
            return this.type.ordinal.compareTo(other.type.ordinal)
        }
        return this.value.compareTo(other.value)
    }

    fun isSameTile(other: MahjongTile): Boolean {
        return this.type == other.type && this.value == other.value
    }

    companion object {
        // Generate a standard set of 136 Mahjong tiles
        fun generateDeck(): List<MahjongTile> {
            val deck = mutableListOf<MahjongTile>()
            var id = 0

            // 1-9 Wan (4 each)
            for (v in 1..9) {
                repeat(4) { deck.add(MahjongTile(TileType.WAN, v, id++)) }
            }
            // 1-9 Tong (4 each)
            for (v in 1..9) {
                repeat(4) { deck.add(MahjongTile(TileType.TONG, v, id++)) }
            }
            // 1-9 Tiao (4 each)
            for (v in 1..9) {
                repeat(4) { deck.add(MahjongTile(TileType.TIAO, v, id++)) }
            }
            // Winds: 1=东, 2=南, 3=西, 4=北 (4 each)
            for (v in 1..4) {
                repeat(4) { deck.add(MahjongTile(TileType.WIND, v, id++)) }
            }
            // Dragons: 1=中, 2=发, 3=白 (4 each)
            for (v in 1..3) {
                repeat(4) { deck.add(MahjongTile(TileType.DRAGON, v, id++)) }
            }

            return deck
        }
    }
}

// Representing a meld (碰 杠)
sealed class MahjongMeld {
    abstract val tiles: List<MahjongTile>
    abstract val type: MeldType

    enum class MeldType { PENG, MING_GANG, AN_GANG, BU_GANG }

    data class Peng(override val tiles: List<MahjongTile>) : MahjongMeld() {
        override val type = MeldType.PENG
    }

    data class Gang(override val tiles: List<MahjongTile>, val isAn: Boolean) : MahjongMeld() {
        override val type = if (isAn) MeldType.AN_GANG else MeldType.MING_GANG
    }
}
