package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MahjongTile
import com.example.model.TileType

// Premium Color Palette for Authentic Mahjong feel
val MahjongBoneWhite = Color(0xFFFBFBF9)
val MahjongJadeGreen = Color(0xFF14532D) // Back part of Mahjong card
val MahjongBorderBeige = Color(0xFFE2E2D5)
val MahjongTextRed = Color(0xFFDC2626)
val MahjongTextGreen = Color(0xFF16A34A)
val MahjongTextBlue = Color(0xFF1D4ED8)
val MahjongTextBlack = Color(0xFF1F2937)

@Composable
fun TileView(
    tile: MahjongTile,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    smallSize: Boolean = false,
    mediumSize: Boolean = false,
    faceDown: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val width = if (smallSize) 28.dp else if (mediumSize) 33.dp else 42.dp
    val height = if (smallSize) 38.dp else if (mediumSize) 47.dp else 58.dp
    val activeOffset = if (isSelected && !smallSize) (-10).dp else 0.dp

    val baseModifier = modifier
        .offset(y = activeOffset)
        .size(width, height)
        .shadow(
            elevation = if (isSelected) 8.dp else 3.dp,
            shape = RoundedCornerShape(4.dp)
        )
        .clip(RoundedCornerShape(4.dp))

    val finalModifier = if (onClick != null && !faceDown) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    if (faceDown) {
        // Display the Back side of the tile (the Classic Jade Green/Blue solid plastic layer)
        Box(
            modifier = finalModifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF166534), // Rich light jade green
                            Color(0xFF14532D)  // Deep forest jade green
                        )
                    )
                )
                .border(1.dp, Color(0xFF0F2F1D), RoundedCornerShape(4.dp))
        ) {
            // Little elegant divider to represent the layered profile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x33FFFFFF))
                    .align(Alignment.TopCenter)
            )
        }
    } else {
        // Display Front side of the card (Ivory bone face with emerald backing showing through the border)
        Box(
            modifier = finalModifier
                .background(MahjongBoneWhite)
                // Bottom-side border represents the green plastic backing peeking from bottom/sides
                .border(
                    width = 1.dp,
                    color = MahjongBorderBeige,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // Highlight bar on top face to mimic glossy screen sheen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.8f))
            )

            // Inner styling depending on Mahjong tile details
            val textWord = when (tile.type) {
                TileType.WAN -> when (tile.value) {
                    1 -> "一"
                    2 -> "二"
                    3 -> "三"
                    4 -> "四"
                    5 -> "五"
                    6 -> "六"
                    7 -> "七"
                    8 -> "八"
                    9 -> "九"
                    else -> tile.value.toString()
                }
                TileType.TONG -> when (tile.value) {
                    1 -> "①"
                    2 -> "②"
                    3 -> "③"
                    4 -> "④"
                    5 -> "⑤"
                    6 -> "⑥"
                    7 -> "⑦"
                    8 -> "⑧"
                    9 -> "⑨"
                    else -> tile.value.toString()
                }
                TileType.TIAO -> when (tile.value) {
                    1 -> "‖"
                    2 -> "‖‖"
                    else -> tile.value.toString()
                }
                TileType.WIND -> when (tile.value) {
                    1 -> "东"
                    2 -> "南"
                    3 -> "西"
                    4 -> "北"
                    else -> ""
                }
                TileType.DRAGON -> when (tile.value) {
                    1 -> "中"
                    2 -> "發"
                    3 -> "白"
                    else -> ""
                }
            }

            val itemColor = when (tile.type) {
                TileType.WIND -> MahjongTextBlack
                TileType.DRAGON -> when (tile.value) {
                    1 -> MahjongTextRed
                    2 -> MahjongTextGreen
                    else -> MahjongTextBlue // White dragon bordered representation
                }
                TileType.WAN -> MahjongTextBlack
                TileType.TONG -> when (tile.value) {
                    1, 5, 9 -> MahjongTextRed
                    2, 4, 8 -> MahjongTextGreen
                    else -> MahjongTextBlue
                }
                TileType.TIAO -> MahjongTextGreen
            }

            if (tile.type == TileType.WAN) {
                // Character Tiles (一万 - 九万)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = textWord,
                        fontSize = if (smallSize) 10.sp else if (mediumSize) 12.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MahjongTextBlue,
                        textAlign = TextAlign.Center,
                        lineHeight = if (smallSize) 10.sp else if (mediumSize) 12.sp else 15.sp
                    )
                    Text(
                        text = "万",
                        fontSize = if (smallSize) 9.sp else if (mediumSize) 11.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MahjongTextRed,
                        textAlign = TextAlign.Center,
                        lineHeight = if (smallSize) 9.sp else if (mediumSize) 11.sp else 14.sp
                    )
                }
            } else if (tile.type == TileType.DRAGON && tile.value == 3) {
                // White Dragon (白板): Elegant square border representation!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (smallSize) 3.dp else if (mediumSize) 4.dp else 5.dp)
                        .border(
                            width = if (smallSize) 1.5.dp else if (mediumSize) 2.0.dp else 2.5.dp,
                            color = MahjongTextBlue,
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    // Small inner details of Chinese frame
                }
            } else if (tile.type == TileType.TIAO) {
                // Tiao Tiles (条子)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tile.value.toString(),
                        fontSize = if (smallSize) 11.sp else if (mediumSize) 13.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MahjongTextGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "条",
                        fontSize = if (smallSize) 8.sp else if (mediumSize) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MahjongTextBlack,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Wind (风) / Dragon (红中/发财) / Dots (筒子)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = textWord,
                        fontSize = if (smallSize) 14.sp else if (mediumSize) 18.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = itemColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // If selected, overlay a soft tint
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x221D4ED8))
                )
            }
        }
    }
}
