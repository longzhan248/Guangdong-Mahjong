package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import com.example.model.MahjongMeld
import com.example.model.MahjongTile
import com.example.model.TileType
import com.example.ui.components.TileView
import com.example.ui.components.MahjongJadeGreen
import com.example.viewmodel.GameState
import com.example.viewmodel.MahjongPlayer
import com.example.viewmodel.MahjongViewModel
import com.example.viewmodel.UserActions
import com.example.viewmodel.RoundResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Rich colors for the classic Mahjong parlor theme
val GreenTableDark = Color(0xFF022C22)
val GreenTableLight = Color(0xFF064E3B)
val MahjongGold = Color(0xFFFBBF24)
val MahjongWoodBorder = Color(0xFF78350F)
val EmeraldLight = Color(0xFF34D399)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahjongScreen(viewModel: MahjongViewModel) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activePlayerIndex.collectAsStateWithLifecycle()
    val dealerIdx by viewModel.dealerIndex.collectAsStateWithLifecycle()
    val lastDiscard by viewModel.lastDiscard.collectAsStateWithLifecycle()
    val discarderIdx by viewModel.discarderIndex.collectAsStateWithLifecycle()
    val actions by viewModel.actionsAvailable.collectAsStateWithLifecycle()
    val selectedTile by viewModel.selectedTile.collectAsStateWithLifecycle()
    val speedMs by viewModel.playSpeedMs.collectAsStateWithLifecycle()
    val dice1 by viewModel.dice1.collectAsStateWithLifecycle()
    val dice2 by viewModel.dice2.collectAsStateWithLifecycle()
    val diceRolling by viewModel.diceRolling.collectAsStateWithLifecycle()
    val currentWind by viewModel.currentWind.collectAsStateWithLifecycle()
    val roundNum by viewModel.handRoundNumber.collectAsStateWithLifecycle()
    val roundOutcome by viewModel.roundResult.collectAsStateWithLifecycle()
    val logs by viewModel.gameLogs.collectAsStateWithLifecycle()
    val records by viewModel.gameRecords.collectAsStateWithLifecycle()
    val jokerMode by viewModel.jokerMode.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        if (SoundManager.isBgmEnabled) {
            SoundManager.isBgmEnabled = true
        }
        onDispose {}
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (gameState) {
                GameState.MENU -> MainMenuScreen(
                    records = records,
                    jokerMode = jokerMode,
                    onJokerModeChange = { viewModel.setJokerMode(it) },
                    onStartGame = { SoundManager.playClick(); viewModel.startNewGame() },
                    onOpenHelp = { SoundManager.playClick(); viewModel.enterHelp() },
                    onResetCabinet = { SoundManager.playClick(); viewModel.resetEntireCabinet() }
                )
                GameState.PLAYING, GameState.REVEAL_WIN -> GamePlayScreen(
                    players = players,
                    jokerMode = jokerMode,
                    deckCount = deck.size,
                    activeIdx = activeIdx,
                    dealerIdx = dealerIdx,
                    lastDiscard = lastDiscard,
                    discarderIdx = discarderIdx,
                    actions = actions,
                    selectedTile = selectedTile,
                    speedMs = speedMs,
                    dice1 = dice1,
                    dice2 = dice2,
                    diceRolling = diceRolling,
                    currentWind = currentWind,
                    roundNum = roundNum,
                    logs = logs,
                    roundOutcome = roundOutcome,
                    onTileClick = { viewModel.selectUserTile(it) },
                    onDrawnTileClick = { viewModel.selectUserDrawnTile() },
                    onDiscardClick = { viewModel.executeUserDiscard(it) },
                    onZimoClick = { viewModel.executeUserZimo() },
                    onSelfGangClick = { viewModel.executeUserSelfGang() },
                    onPassActionClick = { viewModel.executeUserPassAction() },
                    onPengClaimClick = { viewModel.executeUserPengClaim() },
                    onGangClaimClick = { viewModel.executeUserGangClaim() },
                    onDiscardHuClick = { viewModel.executeUserDiscardHu() },
                    onPassClaimClick = { viewModel.executeUserPassClaim() },
                    onSpeedChange = { SoundManager.playClick(); viewModel.setPlaySpeed(it) },
                    onNextRoundClick = { SoundManager.playClick(); viewModel.startNextHand() },
                    onExitToMenu = { SoundManager.playClick(); viewModel.exitHelp() }
                )
                GameState.HELP -> HelpRulesScreen(onBack = { SoundManager.playClick(); viewModel.exitHelp() })
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    records: List<com.example.data.GameRecord>,
    jokerMode: MahjongViewModel.JokerMode,
    onJokerModeChange: (MahjongViewModel.JokerMode) -> Unit,
    onStartGame: () -> Unit,
    onOpenHelp: () -> Unit,
    onResetCabinet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF042F1A), Color(0xFF021E12))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Gorgeous Calligraphy-Style Title Card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0F766E), Color(0xFF064E3B))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(2.dp, MahjongGold, RoundedCornerShape(24.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "广东单机麻将",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MahjongGold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "经典推倒胡 · 碰杠自摸 · 娱乐休闲",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Wild Card Settings Row (Modern Card Selector)
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
                .border(1.dp, Color(0x33FBBF24), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x15FFFFFF)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🀄 玩法选择：万能牌 (鬼牌) 属性",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MahjongGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple(MahjongViewModel.JokerMode.NONE, "经典玩法", "无万能卡"),
                        Triple(MahjongViewModel.JokerMode.HONG_ZHONG, "红中当鬼", "中是万能牌"),
                        Triple(MahjongViewModel.JokerMode.BAI_BAN, "白板当鬼", "白是万能牌")
                    )
                    modes.forEach { (modeOption, title, subtitle) ->
                        val isSelected = jokerMode == modeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF10B981) else Color(0x15FFFFFF))
                                .clickable {
                                    SoundManager.playClick()
                                    onJokerModeChange(modeOption)
                                }
                                .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Core Actions
        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("开始对局", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = onOpenHelp,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("玩法说明", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Historic Stats Summary
        if (records.isNotEmpty()) {
            val winsCount = records.count { it.winnerName == "你" }
            val totalGames = records.size
            val winRate = if (totalGames > 0) (winsCount * 100 / totalGames) else 0
            val userChips = records.firstOrNull()?.finalBalancePlayer0 ?: 1000

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .border(1.dp, Color(0x33FBBF24), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("当前筹码", fontSize = 12.sp, color = Color.LightGray)
                        Text("$userChips", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MahjongGold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("参战局数", fontSize = 12.sp, color = Color.LightGray)
                        Text("$totalGames", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("胜率", fontSize = 12.sp, color = Color.LightGray)
                        Text("$winRate%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Records list
        Text(
            text = "对局回执历程",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Start
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.95f)
                .background(Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无参赛记录，快去开一局！", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(records) { record ->
                    val isPlayerWin = record.winnerName == "你"
                    val itemBg = if (isPlayerWin) Color(0xFF0F5236) else Color(0xFF1E293B)
                    val dateFormatted = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .border(1.dp, if (isPlayerWin) Color(0x4434D399) else Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.winnerEmoji,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "胜者: ${record.winnerName} (${record.handName})",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$dateFormatted | ${record.winType}",
                                fontSize = 11.sp,
                                color = Color.LightGray
                             )
                        }
                        val userChange = record.chipChangePlayer0
                        val chipText = if (userChange >= 0) "+$userChange" else "$userChange"
                        val chipColor = if (userChange >= 0) Color(0xFF34D399) else Color(0xFFF87171)
                        Text(
                            text = chipText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = chipColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (records.isNotEmpty()) {
            Text(
                text = "重置大厅与底注筹码",
                fontSize = 12.sp,
                color = Color(0xFFEF4444).copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onResetCabinet() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun GamePlayScreen(
    players: List<MahjongPlayer>,
    jokerMode: MahjongViewModel.JokerMode,
    deckCount: Int,
    activeIdx: Int,
    dealerIdx: Int,
    lastDiscard: MahjongTile?,
    discarderIdx: Int?,
    actions: UserActions,
    selectedTile: MahjongTile?,
    speedMs: Long,
    dice1: Int,
    dice2: Int,
    diceRolling: Boolean,
    currentWind: String,
    roundNum: Int,
    logs: List<String>,
    roundOutcome: RoundResult?,
    onTileClick: (MahjongTile) -> Unit,
    onDrawnTileClick: () -> Unit,
    onDiscardClick: (MahjongTile) -> Unit,
    onZimoClick: () -> Unit,
    onSelfGangClick: () -> Unit,
    onPassActionClick: () -> Unit,
    onPengClaimClick: () -> Unit,
    onGangClaimClick: () -> Unit,
    onDiscardHuClick: () -> Unit,
    onPassClaimClick: () -> Unit,
    onSpeedChange: (Long) -> Unit,
    onNextRoundClick: () -> Unit,
    onExitToMenu: () -> Unit
) {
    if (players.size < 4) return

    val isTileWild = { tile: MahjongTile ->
        when (jokerMode) {
            MahjongViewModel.JokerMode.NONE -> false
            MahjongViewModel.JokerMode.HONG_ZHONG -> tile.type == com.example.model.TileType.DRAGON && tile.value == 1
            MahjongViewModel.JokerMode.BAI_BAN -> tile.type == com.example.model.TileType.DRAGON && tile.value == 3
        }
    }

    val playerMe = players[0]
    val playerRight = players[1]
    val playerUp = players[2]
    val playerLeft = players[3]
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF042F1A), Color(0xFF011C10))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Dash - Glassmorphism parlor bar panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x66000000))
                    .border(width = (0.5).dp, color = Color(0x22FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onExitToMenu,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Exit to Menu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "广东麻将 · 推倒胡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MahjongGold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "东风局 $roundNum / 4",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Audio controls and Speed adjustments side-by-side
                var bgmOn by remember { mutableStateOf(SoundManager.isBgmEnabled) }
                var sfxOn by remember { mutableStateOf(SoundManager.isSfxEnabled) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Audio toggles inside a classy pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0x4D000000), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (bgmOn) Color(0x3334D399) else Color.Transparent)
                                .clickable {
                                    bgmOn = !bgmOn
                                    SoundManager.isBgmEnabled = bgmOn
                                    SoundManager.playClick()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(if (bgmOn) "🎵 乐" else "🔇 乐", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sfxOn) Color(0x3334D399) else Color.Transparent)
                                .clickable {
                                    sfxOn = !sfxOn
                                    SoundManager.isSfxEnabled = sfxOn
                                    SoundManager.playClick()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(if (sfxOn) "🔊 音" else "🔇 音", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Speed adjustments inside another pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0x4D000000), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("速度:", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.padding(end = 2.dp))
                        listOf(
                            Pair("慢", 1400L),
                            Pair("中", 900L),
                            Pair("快", 400L)
                        ).forEach { (label, duration) ->
                            val isSel = speedMs == duration
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MahjongGold else Color.Transparent)
                                    .clickable { onSpeedChange(duration) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Green Mahjong Table Arena! Built adaptive with responsive spacing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(6.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GreenTableLight, GreenTableDark)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(6.dp, MahjongWoodBorder, RoundedCornerShape(16.dp))
                    .border(7.5.dp, Color(0xFF3B1E08), RoundedCornerShape(16.dp))
            ) {
                // Outer table glow sheen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                )

                // ----------------------------------------------------
                // Opponent placements: LEFT, RIGHT, TOP
                // ----------------------------------------------------

                // TOP AI (Player 2 - 包租婆兰姐)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OpponentProfile(playerUp, dealerIdx == 2, activeIdx == 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Show back of cards
                        repeat(playerUp.hand.size) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 16.dp)
                                    .background(MahjongJadeGreen, RoundedCornerShape(1.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f))
                            )
                        }
                        if (playerUp.hasDrawnTile != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 16.dp)
                                    .background(MahjongJadeGreen, RoundedCornerShape(1.dp))
                                    .border(1.dp, MahjongGold)
                            )
                        }
                    }
                    // Meld reveal
                    MeldListRow(playerUp.declaredMelds, isTileWild)
                }

                // LEFT AI (Player 3 - 雀圣波仔)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .width(76.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OpponentProfile(playerLeft, dealerIdx == 3, activeIdx == 3)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${playerLeft.totalActiveTilesCount}张牌",
                        fontSize = 11.sp,
                        color = Color.White.copy(0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    MeldListRow(playerLeft.declaredMelds, isTileWild)
                }

                // RIGHT AI (Player 1 - 雀友阿明)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .width(76.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OpponentProfile(playerRight, dealerIdx == 1, activeIdx == 1)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${playerRight.totalActiveTilesCount}张牌",
                        fontSize = 11.sp,
                        color = Color.White.copy(0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    MeldListRow(playerRight.declaredMelds, isTileWild)
                }

                // CENTER CONTROLLER PANEL (Remaining cards, Turn Indicator, Dice, Current Wind)
                val centerPanelSize = if (isLandscape) 105.dp else 144.dp
                val discardArenaHeight = if (isLandscape) 115.dp else 190.dp

                Box(
                    modifier = Modifier
                        .size(centerPanelSize)
                        .align(Alignment.Center)
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .background(Color(0xE605150E), RoundedCornerShape(24.dp))
                        .border(1.5.dp, MahjongGold.copy(0.8f), RoundedCornerShape(24.dp))
                        .padding(if (isLandscape) 4.dp else 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "剩 $deckCount 张",
                            fontSize = if (isLandscape) 10.sp else 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MahjongGold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 6.dp)
                        ) {
                            Text(
                                "东",
                                fontSize = if (isLandscape) 10.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeIdx == 1) MahjongGold else Color.Gray.copy(0.6f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "北",
                                    fontSize = if (isLandscape) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeIdx == 2) MahjongGold else Color.Gray.copy(0.6f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(if (isLandscape) 30.dp else 40.dp)
                                        .background(Brush.radialGradient(colors = listOf(Color(0xFF0F766E), Color(0xFF042F1A))), CircleShape)
                                        .border(1.5.dp, MahjongGold.copy(0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentWind,
                                        fontSize = if (isLandscape) 14.sp else 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MahjongGold
                                        // Removed line height issues
                                    )
                                }
                                Text(
                                    "南",
                                    fontSize = if (isLandscape) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeIdx == 0) MahjongGold else Color.Gray.copy(0.6f)
                                )
                            }
                            Text(
                                "西",
                                fontSize = if (isLandscape) 10.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeIdx == 3) MahjongGold else Color.Gray.copy(0.6f)
                            )
                        }

                        // Small Dice displays
                        Row(
                            modifier = Modifier.padding(top = if (isLandscape) 1.dp else 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DiceView(number = dice1, rolling = diceRolling)
                            DiceView(number = dice2, rolling = diceRolling)
                        }
                    }
                }

                // ----------------------------------------------------
                // DISCARDED TILES ARENA Layout
                // ----------------------------------------------------
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f)
                        .height(discardArenaHeight)
                ) {
                    // USER Discards (Bottom center-ish)
                    RowOfDiscards(
                        discards = playerMe.discards,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isLandscape) 2.dp else 12.dp),
                        lastDiscard = lastDiscard,
                        isLastOfPlayer = discarderIdx == 0,
                        isTileWild = isTileWild
                    )

                    // TOP AI Discards
                    RowOfDiscards(
                        discards = playerUp.discards,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (isLandscape) 2.dp else 12.dp),
                        lastDiscard = lastDiscard,
                        isLastOfPlayer = discarderIdx == 2,
                        isTileWild = isTileWild
                    )

                    // LEFT AI Discards
                    RowOfDiscards(
                        discards = playerLeft.discards,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = if (isLandscape) 10.dp else 28.dp),
                        lastDiscard = lastDiscard,
                        isLastOfPlayer = discarderIdx == 3,
                        vertical = true,
                        isTileWild = isTileWild
                    )

                    // RIGHT AI Discards
                    RowOfDiscards(
                        discards = playerRight.discards,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = if (isLandscape) 10.dp else 28.dp),
                        lastDiscard = lastDiscard,
                        isLastOfPlayer = discarderIdx == 1,
                        vertical = true,
                        isTileWild = isTileWild
                    )
                }

                // Active Discard Prompt focus indicator overlay (Float pointing)
                if (lastDiscard != null && discarderIdx != null) {
                    Box(
                        modifier = Modifier
                            .align(
                                when (discarderIdx) {
                                    0 -> Alignment.BottomCenter
                                    1 -> Alignment.CenterEnd
                                    2 -> Alignment.TopCenter
                                    else -> Alignment.CenterStart
                                }
                            )
                            .padding(2.dp)
                    ) {
                        // High-contrast glowing banner pointing out what was just thrown
                    }
                }
            }

            // Game Logging dynamic scrolling channel - luxurious dark band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFF01120A))
                    .border(width = (0.5).dp, color = Color(0x1A34D399))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val latestLog = logs.firstOrNull() ?: "准备开打广东麻将！"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x3334D399), RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color(0x6634D399), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("牌局日志", fontSize = 10.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = latestLog,
                        fontSize = 12.sp,
                        color = Color.LightGray.copy(0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ----------------------------------------------------
            // Bottom USER Dashboard & Interface Hand
            // ----------------------------------------------------
            val dashboardBottomPadding = if (isLandscape) 4.dp else 12.dp
            val dashboardTopPadding = if (isLandscape) 2.dp else 6.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF021E12), Color(0xFF042F1A))
                        )
                    )
                    .padding(bottom = dashboardBottomPadding, top = dashboardTopPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Claim Button Bar
                AnimatedVisibility(
                    visible = actions.hasAnyActions,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (actions.canHu) {
                            Button(
                                onClick = {
                                    if (actions.isSelfDraw) onZimoClick() else onDiscardHuClick()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .height(48.dp)
                                    .shadow(6.dp, CircleShape),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.5.dp, Color.White.copy(0.6f))
                            ) {
                                Text(
                                    if (actions.isSelfDraw) "自摸" else "胡牌",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF022C22)
                                )
                            }
                        }

                        if (actions.canGang) {
                            Button(
                                onClick = {
                                    if (actions.isSelfDraw) onSelfGangClick() else onGangClaimClick()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("杠", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (actions.canPeng) {
                            Button(
                                onClick = onPengClaimClick,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("碰", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (actions.canPass) {
                            Button(
                                onClick = {
                                    if (actions.isSelfDraw) onPassActionClick() else onPassClaimClick()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("过", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Prompt user if it is their hand turn
                val playerTurnHint = if (activeIdx == 0) {
                    if (selectedTile != null) "已选中 【${selectedTile.displayName}】，再次点击或点击右侧打出" else "轮到你了，选择手牌并打出"
                } else {
                    val activePlayer = players.getOrNull(activeIdx)
                    if (activePlayer != null) "正在看 [${activePlayer.name}] 操作..." else ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤠 你 (庄家)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "筹码: ${playerMe.chips}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MahjongGold
                        )
                    }

                    Text(
                        text = playerTurnHint,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )

                    // "Discard" trigger
                    if (activeIdx == 0 && selectedTile != null) {
                        Button(
                            onClick = { onDiscardClick(selectedTile) },
                            colors = ButtonDefaults.buttonColors(containerColor = MahjongGold),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("打出", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // The User Tiles Row (Adaptive layout) with beautiful bottom gradient sheen
                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val totalItemsCount = playerMe.hand.size + (playerMe.declaredMelds.size * 3) + (if (playerMe.hasDrawnTile != null) 1 else 0)
                val useMediumSize = isLandscape || totalItemsCount > 11
                val tileSpacedBy = if (useMediumSize) 2.dp else 3.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 1. Revealed Melds
                    MeldListMe(playerMe.declaredMelds, isTileWild)

                    if (playerMe.declaredMelds.isNotEmpty() && playerMe.hand.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(if (useMediumSize) 4.dp else 8.dp))
                    }

                    // 2. Private Hand Tiles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(tileSpacedBy),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        playerMe.hand.forEach { tile ->
                            TileView(
                                tile = tile,
                                isSelected = selectedTile?.id == tile.id,
                                smallSize = false,
                                mediumSize = useMediumSize,
                                isWild = isTileWild(tile),
                                onClick = {
                                    SoundManager.playClick()
                                    onTileClick(tile)
                                }
                            )
                        }
                    }

                    // 3. Spaced newly drawn card
                    if (playerMe.hasDrawnTile != null) {
                        Spacer(modifier = Modifier.width(if (useMediumSize) 6.dp else 10.dp))
                        TileView(
                            tile = playerMe.hasDrawnTile,
                            isSelected = selectedTile?.id == playerMe.hasDrawnTile.id,
                            smallSize = false,
                            mediumSize = useMediumSize,
                            isWild = isTileWild(playerMe.hasDrawnTile),
                            onClick = {
                                SoundManager.playClick()
                                onDrawnTileClick()
                            }
                        )
                    }
                }
            }
        }

        // Win / Tie Results Dialog modal
        if (roundOutcome != null) {
            AlertDialog(
                onDismissRequest = {}, // Cannot bypass results dialog
                confirmButton = {
                    Button(
                        onClick = onNextRoundClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("下一局", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onExitToMenu) {
                        Text("返回大厅", color = Color.Gray)
                    }
                },
                title = {
                    Text(
                        text = if (roundOutcome.winnerIndex == -1) "流局" else "和牌局终",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MahjongGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = roundOutcome.description,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // If there is a winner, showcase their winning hand nicely!
                        if (roundOutcome.winnerIndex != -1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "得胜胡型: ${roundOutcome.handName} | ${roundOutcome.fan} 番",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Winning combo breakdowns
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF292524), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                roundOutcome.details.forEach { det ->
                                    Text("• $det", fontSize = 12.sp, color = Color.LightGray)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("获胜手牌:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                roundOutcome.winningHand.forEach { wt ->
                                    TileView(tile = wt, smallSize = true, isWild = isTileWild(wt))
                                    Spacer(modifier = Modifier.width(1.dp))
                                }
                                if (roundOutcome.winningTile != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+", color = MahjongGold, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TileView(tile = roundOutcome.winningTile, smallSize = true, isSelected = true, isWild = isTileWild(roundOutcome.winningTile))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Score shifts lists
                        Text("筹码计算表格:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(4.dp))
                        players.forEachIndexed { sIdx, p ->
                            val diff = roundOutcome.chipChanges[sIdx]
                            val diffText = if (diff > 0) "+$diff" else if (diff < 0) "$diff" else "0"
                            val diffColor = if (diff > 0) Color(0xFF22C55E) else if (diff < 0) Color(0xFFEF4444) else Color.LightGray

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${p.emoji} ${p.name}", fontSize = 13.sp, color = Color.White)
                                Row {
                                    Text(
                                        text = diffText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = diffColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("余额: ${p.chips}", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                containerColor = Color(0xFF1C1917),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// Compact helper to render opponent profiles
@Composable
fun OpponentProfile(player: MahjongPlayer, isDealer: Boolean, isActive: Boolean) {
    val ringColor = if (isActive) MahjongGold else if (isDealer) Color(0xFFEA580C) else Color.Transparent
    val activeBorder = if (isActive || isDealer) BorderStroke(1.5.dp, ringColor) else null

    Card(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        border = activeBorder,
        colors = CardDefaults.cardColors(containerColor = Color(0x99292524))
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(player.emoji, fontSize = 24.sp)
                if (isDealer) {
                    Box(
                        modifier = Modifier
                            .offset(x = 10.dp, y = (-10).dp)
                            .size(14.dp)
                            .background(Color(0xFFEA580C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("庄", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                player.name,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 68.dp)
            )
            Text("${player.chips}", fontSize = 10.sp, color = MahjongGold, fontWeight = FontWeight.Bold)
            
            if (player.activeStatus.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(Color(0xDD000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        player.activeStatus,
                        fontSize = 9.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DiceView(number: Int, rolling: Boolean) {
    val modifier = Modifier
        .size(16.dp)
        .background(Color.White, RoundedCornerShape(2.dp))
        .border(0.5.dp, Color.Gray, RoundedCornerShape(2.dp))
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (rolling) {
            Text("?", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Black)
        } else {
            val isRedDot = number == 1 || number == 4
            Text(
                text = number.toString(),
                fontSize = 11.sp,
                color = if (isRedDot) Color.Red else Color.Black,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// Melds layouts for opponents
@Composable
fun MeldListRow(melds: List<MahjongMeld>, isTileWild: (MahjongTile) -> Boolean = { false }) {
    if (melds.isEmpty()) return
    Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        melds.forEach { meld ->
            Row(
                modifier = Modifier
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    .padding(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val tiles = meld.tiles.take(3) // Represent as triplet visually
                tiles.forEach { tile ->
                    TileView(tile = tile, smallSize = true, faceDown = meld is MahjongMeld.Gang && meld.isAn, isWild = isTileWild(tile))
                }
            }
        }
    }
}

// User melds list shown on the left of their hand
@Composable
fun MeldListMe(melds: List<MahjongMeld>, isTileWild: (MahjongTile) -> Boolean = { false }) {
    if (melds.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        melds.forEach { meld ->
            Row(
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .background(Color(0x33000000))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isAn = meld is MahjongMeld.Gang && meld.isAn
                meld.tiles.take(3).forEach { tile ->
                    TileView(tile = tile, smallSize = true, faceDown = isAn, isWild = isTileWild(tile))
                }
            }
        }
    }
}

// Renders rows of tiles discarded into table arena
@Composable
fun RowOfDiscards(
    discards: List<MahjongTile>,
    modifier: Modifier = Modifier,
    lastDiscard: MahjongTile?,
    isLastOfPlayer: Boolean,
    vertical: Boolean = false,
    isTileWild: (MahjongTile) -> Boolean = { false }
) {
    if (discards.isEmpty()) return

    // Layout in a grid: 9 tiles per row in landscape to save height, 6 in portrait
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val chunkSize = if (isLandscape) 9 else 6
    val rows = discards.chunked(chunkSize)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        rows.forEachIndexed { rIdx, list ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                list.forEach { tile ->
                    val isJustDiscarded = isLastOfPlayer && lastDiscard != null && tile.id == lastDiscard.id
                    val borderMod = if (isJustDiscarded) {
                        Modifier.border(1.dp, Color.Yellow, RoundedCornerShape(2.dp))
                    } else {
                        Modifier
                    }

                    TileView(
                        tile = tile,
                        smallSize = true,
                        isWild = isTileWild(tile),
                        modifier = borderMod
                    )
                }
            }
        }
    }
}

@Composable
fun HelpRulesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("广东麻将『推倒胡』玩法规则", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("一、 基础配置", fontSize = 16.sp, color = MahjongGold, fontWeight = FontWeight.Bold)
                Text(
                    "本单机版采用标准 136 张牌：\n" +
                            "• 万、筒、条（各36张，共108张)\n" +
                            "• 各风向牌 (东、南、西、北各4张，共16张)\n" +
                            "• 各箭牌 (红中、发财、白板各4张，共12张)",
                    fontSize = 13.sp, color = Color.LightGray
                )
            }

            item {
                Text("二、 核心玩法 (广东推倒胡)", fontSize = 16.sp, color = MahjongGold, fontWeight = FontWeight.Bold)
                Text(
                    "• 不能『吃牌』(Chow)：只能自摸顺子或暗组顺子，别人打出的牌不能吃。\n" +
                            "• 可以『碰牌』(Peng)：当别人打出与自己手中成双对的牌时，可以执行碰牌，组成明刻。\n" +
                            "• 可以『杠牌』(Gang)：包括明杠（碰后杠，直杠）和暗杠（自己摸齐4张）。暗杠牌翻转显示，保护手牌隐私。\n" +
                            "• 『自摸』(Zimo)最香：自己摸到胡牌，向其他三家分别收取筹码。点炮胡时，仅点炮的倒霉鬼全额独力代扣赔付！",
                    fontSize = 13.sp, color = Color.LightGray
                )
            }

            item {
                Text("三、 胡型番数表 (高额倍率)", fontSize = 16.sp, color = MahjongGold, fontWeight = FontWeight.Bold)
                Text(
                    "• 【平胡 / 鸡胡】 (1 番): 标准和牌，包含顺子与刻子刻印组合。\n" +
                            "• 【碰碰胡】 (2 番): 全由刻子(或者杠子)以及一对将牌组成。\n" +
                            "• 【混一色】 (2 番): 由单一花色+风牌/字牌组合。\n" +
                            "• 【清一色】 (4 番): 手牌全部属于单一万/条/筒花色组成。\n" +
                            "• 【七对子】 (4 番): 没有宣告碰杠，手牌正好组成7对双数成对。\n" +
                            "• 【十三幺】 (10 番): 指南风北箭孤子，1-9万筒条全幺，绝世罕见！\n" +
                            "• 【自摸加成】: 自摸会让原番数乘2倍翻滚计价！\n" +
                            "• 【杠上开花】: 杠牌后摸牌直接胡牌，原番数 + 1番。",
                    fontSize = 13.sp, color = Color.LightGray
                )
            }

            item {
                Text("四、 操作助手", fontSize = 16.sp, color = MahjongGold, fontWeight = FontWeight.Bold)
                Text(
                    "• 摸牌：系统全自动发牌到手部右侧略显空位。\n" +
                            "• 选中：点击手中任何一张牌，它会往上突起，再次点击同一张牌或者点击上面的大金『打出』按钮，即可完成出牌。\n" +
                            "• 如果可以执行碰/杠/胡，中央操作条会自动探出醒目的动作按钮指导局终决策！",
                    fontSize = 13.sp, color = Color.LightGray
                )
            }
        }
    }
}
