package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRecord
import com.example.data.GameRecordRepository
import com.example.engine.MahjongAI
import com.example.engine.MahjongEvaluator
import com.example.model.MahjongMeld
import com.example.model.MahjongTile
import com.example.model.TileType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.ui.SoundManager

enum class GameState {
    MENU,
    PLAYING,
    REVEAL_WIN,
    HELP
}

data class MahjongPlayer(
    val id: Int, // 0 = Player, 1 = AI 1 (Right), 2 = AI 2 (Up), 3 = AI 3 (Left)
    val name: String,
    val emoji: String,
    val chips: Int,
    val hand: List<MahjongTile> = emptyList(),
    val declaredMelds: List<MahjongMeld> = emptyList(),
    val discards: List<MahjongTile> = emptyList(),
    val activeStatus: String = "",
    val hasDrawnTile: MahjongTile? = null // Separately tracked drawn card
) {
    // Total cards counting private hand and those in declared melds (3 per Peng/Gang)
    val totalActiveTilesCount: Int
        get() = hand.size + (if (hasDrawnTile != null) 1 else 0)
}

data class UserActions(
    val canHu: Boolean = false,
    val canPeng: Boolean = false,
    val canGang: Boolean = false,
    val canPass: Boolean = false,
    val targetTile: MahjongTile? = null,
    val isSelfDraw: Boolean = false,
    val isBugang: Boolean = false
) {
    val hasAnyActions: Boolean
        get() = canHu || canPeng || canGang
}

data class RoundResult(
    val winnerIndex: Int, // -1 for Tie
    val winnerName: String,
    val winType: String, // "自摸" or "旁胡" (Dianpao/点炮) or "流局"
    val handName: String,
    val fan: Int,
    val details: List<String>,
    val winningTile: MahjongTile?,
    val winningHand: List<MahjongTile>,
    val chipChanges: List<Int>,
    val description: String
)

class MahjongViewModel(private val repository: GameRecordRepository) : ViewModel() {

    // Main Game State
    private val _gameState = MutableStateFlow(GameState.MENU)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // 4 Players State
    private val _players = MutableStateFlow<List<MahjongPlayer>>(emptyList())
    val players: StateFlow<List<MahjongPlayer>> = _players.asStateFlow()

    // Remaining tile deck
    private val _deck = MutableStateFlow<List<MahjongTile>>(emptyList())
    val deck: StateFlow<List<MahjongTile>> = _deck.asStateFlow()

    // Active player turn index
    private val _activePlayerIndex = MutableStateFlow(0)
    val activePlayerIndex: StateFlow<Int> = _activePlayerIndex.asStateFlow()

    // Dealer index
    private val _dealerIndex = MutableStateFlow(0)
    val dealerIndex: StateFlow<Int> = _dealerIndex.asStateFlow()

    // Last discarded tile
    private val _lastDiscard = MutableStateFlow<MahjongTile?>(null)
    val lastDiscard: StateFlow<MahjongTile?> = _lastDiscard.asStateFlow()

    private val _discarderIndex = MutableStateFlow<Int?>(null)
    val discarderIndex: StateFlow<Int?> = _discarderIndex.asStateFlow()

    // Actions available for the user
    private val _actionsAvailable = MutableStateFlow(UserActions())
    val actionsAvailable: StateFlow<UserActions> = _actionsAvailable.asStateFlow()

    // User's selected tile for discarding
    private val _selectedTile = MutableStateFlow<MahjongTile?>(null)
    val selectedTile: StateFlow<MahjongTile?> = _selectedTile.asStateFlow()

    // Pacing speed of AI (ms)
    private val _playSpeedMs = MutableStateFlow(1000L)
    val playSpeedMs: StateFlow<Long> = _playSpeedMs.asStateFlow()

    enum class JokerMode {
        NONE,       // 无万能牌
        HONG_ZHONG, // 红中 (万能牌)
        BAI_BAN     // 白板 (万能牌)
    }

    private val _jokerMode = MutableStateFlow(JokerMode.NONE)
    val jokerMode: StateFlow<JokerMode> = _jokerMode.asStateFlow()

    fun setJokerMode(mode: JokerMode) {
        _jokerMode.value = mode
        addLog("玩法设置：${when(mode) {
            JokerMode.NONE -> "无万能牌（经典玩法）"
            JokerMode.HONG_ZHONG -> "红中作鬼牌（万能牌玩法）"
            JokerMode.BAI_BAN -> "白板作鬼牌（万能牌玩法）"
        }}")
    }

    fun getWildCardDetails(): Pair<TileType?, Int?> {
        return when (_jokerMode.value) {
            JokerMode.NONE -> Pair(null, null)
            JokerMode.HONG_ZHONG -> Pair(TileType.DRAGON, 1)
            JokerMode.BAI_BAN -> Pair(TileType.DRAGON, 3)
        }
    }

    // Dice indicators
    private val _dice1 = MutableStateFlow(1)
    val dice1: StateFlow<Int> = _dice1.asStateFlow()
    private val _dice2 = MutableStateFlow(1)
    val dice2: StateFlow<Int> = _dice2.asStateFlow()
    private val _diceRolling = MutableStateFlow(false)
    val diceRolling: StateFlow<Boolean> = _diceRolling.asStateFlow()

    // Current wind round details
    private val _currentWind = MutableStateFlow("东风")
    val currentWind: StateFlow<String> = _currentWind.asStateFlow()
    private val _handRoundNumber = MutableStateFlow(1)
    val handRoundNumber: StateFlow<Int> = _handRoundNumber.asStateFlow()

    // Current Round win outcome
    private val _roundResult = MutableStateFlow<RoundResult?>(null)
    val roundResult: StateFlow<RoundResult?> = _roundResult.asStateFlow()

    // Live list of game logs for a scrolling chat channel in-game!
    private val _gameLogs = MutableStateFlow<List<String>>(emptyList())
    val gameLogs: StateFlow<List<String>> = _gameLogs.asStateFlow()

    // Total history of games from Database
    val gameRecords = repository.allRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var aiTurnJob: Job? = null

    // Player name configurations
    private val defaultNames = listOf("你", "雀友阿明", "包租婆兰姐", "雀圣波仔")
    private val defaultEmojis = listOf("🤠", "👦", "👩‍🦱", "🧔")

    init {
        initializeSeating()
    }

    private fun initializeSeating() {
        val initialPlayers = List(4) { idx ->
            MahjongPlayer(
                id = idx,
                name = defaultNames[idx],
                emoji = defaultEmojis[idx],
                chips = 1000
            )
        }
        _players.value = initialPlayers
    }

    fun setPlaySpeed(speedMs: Long) {
        _playSpeedMs.value = speedMs
    }

    fun enterHelp() {
        _gameState.value = GameState.HELP
    }

    fun exitHelp() {
        _gameState.value = GameState.MENU
    }

    // Reset records and chips
    fun resetEntireCabinet() {
        viewModelScope.launch {
            repository.clearAllRecords()
            val resetPlayers = _players.value.map { it.copy(chips = 1000) }
            _players.value = resetPlayers
            _gameState.value = GameState.MENU
        }
    }

    // Start a new game lobby
    fun startNewGame() {
        viewModelScope.launch {
            // Read records to restore chips, or default to 1000 if none
            // Simple approach: if records exist, load chips from latest record, else 1000
            val records = gameRecords.value
            val restoredPlayers = if (records.isNotEmpty()) {
                val last = records.first()
                listOf(
                    MahjongPlayer(0, defaultNames[0], defaultEmojis[0], last.finalBalancePlayer0),
                    MahjongPlayer(1, defaultNames[1], defaultEmojis[1], last.finalBalancePlayer0 + (last.chipChangePlayer1 - last.chipChangePlayer0)), // approximation or keep track
                    MahjongPlayer(2, defaultNames[2], defaultEmojis[2], last.finalBalancePlayer0 + (last.chipChangePlayer2 - last.chipChangePlayer0)),
                    MahjongPlayer(3, defaultNames[3], defaultEmojis[3], last.finalBalancePlayer0 + (last.chipChangePlayer3 - last.chipChangePlayer0))
                ).mapIndexed { idx, player ->
                    // To make it simple, let's keep direct chip balance of user and give AI players standard variations or restore from latest log
                    val profileBalance = when (idx) {
                        0 -> last.finalBalancePlayer0
                        1 -> 1000 + records.sumOf { it.chipChangePlayer1 }
                        2 -> 1000 + records.sumOf { it.chipChangePlayer2 }
                        3 -> 1000 + records.sumOf { it.chipChangePlayer3 }
                        else -> 1000
                    }
                    player.copy(chips = profileBalance)
                }
            } else {
                _players.value.map { it.copy(chips = 1000) }
            }
            
            _players.value = restoredPlayers
            _gameState.value = GameState.PLAYING
            _gameLogs.value = listOf("广东省单机推倒胡开局！")
            
            startNextHand()
        }
    }

    // Deal and start the next round
    fun startNextHand() {
        aiTurnJob?.cancel()
        _roundResult.value = null
        val currentPlayers = _players.value

        // 1. Generate full shuffled deck
        val newDeck = MahjongTile.generateDeck().shuffled().toMutableList()

        // 2. Deal 13 cards to each player, dealer gets 13 initially as well
        val updatedPlayers = currentPlayers.mapIndexed { idx, player ->
            val initialHand = mutableListOf<MahjongTile>()
            repeat(13) {
                if (newDeck.isNotEmpty()) {
                    initialHand.add(newDeck.removeAt(0))
                }
            }
            player.copy(
                hand = initialHand.sorted(),
                hasDrawnTile = null,
                declaredMelds = emptyList(),
                discards = emptyList(),
                activeStatus = ""
            )
        }

        _deck.value = newDeck
        _players.value = updatedPlayers
        _lastDiscard.value = null
        _discarderIndex.value = null
        _selectedTile.value = null
        _actionsAvailable.value = UserActions()

        // Roll Dice Animation
        viewModelScope.launch {
            SoundManager.playDice()
            _diceRolling.value = true
            _dice1.value = (1..6).random()
            _dice2.value = (1..6).random()
            addLog("庄家掷出双骰: ${_dice1.value}点 + ${_dice2.value}点")
            delay(1000)
            _diceRolling.value = false

            // Dealer draws 14th card to begin
            val dealerIdx = _dealerIndex.value
            _activePlayerIndex.value = dealerIdx
            addLog("开始发牌，庄家 [${defaultNames[dealerIdx]}] 第一手摸牌")
            
            drawTileForPlayer(dealerIdx)
        }
    }

    private fun addLog(message: String) {
        val current = _gameLogs.value.toMutableList()
        current.add(0, message) // Insert at top
        _gameLogs.value = current.take(30) // keep last 30
    }

    // Player draws a tile from deck
    private suspend fun drawTileForPlayer(playerIdx: Int) {
        _activePlayerIndex.value = playerIdx
        
        // If deck is depleted, it's a TIE (流局)
        if (_deck.value.isEmpty()) {
            handleTieGame()
            return
        }

        val deckCopy = _deck.value.toMutableList()
        val drawn = deckCopy.removeAt(0)
        _deck.value = deckCopy

        val playersCopy = _players.value.toMutableList()
        val p = playersCopy[playerIdx]
        playersCopy[playerIdx] = p.copy(
            hasDrawnTile = drawn,
            activeStatus = ""
        )
        _players.value = playersCopy

        if (playerIdx == 0) {
            // USER's turn: Evaluate self draw actions (Win/Zimo, Angang, Bugang)
            evaluateUserSelfActions(drawn)
        } else {
            // AI turn
            triggerAITurn(playerIdx, drawn)
        }
    }

    // Evaluate actions user can make on drawing a tile
    private suspend fun evaluateUserSelfActions(drawnTile: MahjongTile) {
        val player = _players.value[0]
        val sortedHand = player.hand

        // 1. Check for Self Draw Win (自摸)
        val wildDetails = getWildCardDetails()
        val huEval = MahjongEvaluator.checkHu(
            handTiles = sortedHand,
            declaredMelds = player.declaredMelds,
            winningTile = drawnTile,
            isSelfDraw = true,
            wildTileType = wildDetails.first,
            wildTileValue = wildDetails.second
        )

        // 2. Check for Angang (4 of a kind inside hand including drawn tile)
        val combinedHand = sortedHand + drawnTile
        val anGangCandidates = combinedHand.groupBy { it.displayName }
            .filter { it.value.size == 4 }
            .keys

        // 3. Check for Bugang (already Peng'ed, and drew the 4th matching tile)
        val buGangCandidates = player.declaredMelds
            .filterIsInstance<MahjongMeld.Peng>()
            .map { it.tiles.first() }
            .filter { it.isSameTile(drawnTile) }

        val canAnGang = anGangCandidates.isNotEmpty()
        val canBuGang = buGangCandidates.isNotEmpty()

        if (huEval.isWin || canAnGang || canBuGang) {
            _actionsAvailable.value = UserActions(
                canHu = huEval.isWin,
                canGang = canAnGang || canBuGang,
                canPass = true,
                targetTile = drawnTile,
                isSelfDraw = true,
                isBugang = canBuGang
            )
            addLog("你摸了 [${drawnTile.displayName}]，可以执行自摸或暗杠动作！")
        } else {
            _actionsAvailable.value = UserActions() // No special actions
        }
    }

    // Perform User self-action: Zimo (自摸)
    fun executeUserZimo() {
        val actions = _actionsAvailable.value
        if (!actions.canHu || actions.targetTile == null) return

        val player = _players.value[0]
        viewModelScope.launch {
            val wildDetails = getWildCardDetails()
            val winEval = MahjongEvaluator.checkHu(
                handTiles = player.hand,
                declaredMelds = player.declaredMelds,
                winningTile = actions.targetTile,
                isSelfDraw = true,
                wildTileType = wildDetails.first,
                wildTileValue = wildDetails.second
            )

            handleWin(0, 0, actions.targetTile, winEval, isSelfDraw = true)
            _actionsAvailable.value = UserActions()
        }
    }

    // Perform User self-action: Gang (暗杠 or 补杠)
    fun executeUserSelfGang() {
        val actions = _actionsAvailable.value
        val drawn = actions.targetTile ?: return
        if (!actions.canGang) return

        val player = _players.value[0]
        val playersCopy = _players.value.toMutableList()

        if (actions.isBugang) {
            // Bugang: Upgrade existing PENG to GANG
            val updatedMelds = player.declaredMelds.map { meld ->
                if (meld is MahjongMeld.Peng && meld.tiles.first().isSameTile(drawn)) {
                    MahjongMeld.Gang(meld.tiles + drawn, isAn = false)
                } else {
                    meld
                }
            }
            playersCopy[0] = player.copy(
                hasDrawnTile = null,
                declaredMelds = updatedMelds
            )
            addLog("你执行了补杠：[${drawn.displayName}]")
        } else {
            // Angang: Remove 4 of kind from hand
            val combinedHand = player.hand + drawn
            val counts = combinedHand.groupBy { it.displayName }
            val gangTypeStr = counts.entries.first { it.value.size == 4 }.key
            
            val updatedHand = combinedHand.filterNot { it.displayName == gangTypeStr }
            val matchTiles = combinedHand.filter { it.displayName == gangTypeStr }

            val newMeld = MahjongMeld.Gang(matchTiles, isAn = true)
            playersCopy[0] = player.copy(
                hand = updatedHand.sorted(),
                hasDrawnTile = null,
                declaredMelds = player.declaredMelds + newMeld
            )
            addLog("你执行了暗杠：[$gangTypeStr]")
        }

        _players.value = playersCopy
        _actionsAvailable.value = UserActions()
        _selectedTile.value = null

        // After a Gang, player must draw another tile from deck
        viewModelScope.launch {
            delay(500)
            drawTileForPlayer(0)
        }
    }

    // User chooses to pass their self-drawn action choices and just discard
    fun executeUserPassAction() {
        val actions = _actionsAvailable.value
        if (!actions.canPass) return
        _actionsAvailable.value = UserActions()
        addLog("你选择跳过了动作，请选择一张牌打出。")
    }

    // User selects a tile in hand
    fun selectUserTile(tile: MahjongTile) {
        if (_activePlayerIndex.value != 0) return // Not your turn

        // If tile was already selected, discard it!
        if (_selectedTile.value == tile) {
            executeUserDiscard(tile)
        } else {
            _selectedTile.value = tile
        }
    }

    // Discard selected drawn card
    fun selectUserDrawnTile() {
        val drawn = _players.value[0].hasDrawnTile ?: return
        if (_activePlayerIndex.value != 0) return

        if (_selectedTile.value == drawn) {
            executeUserDiscard(drawn)
        } else {
            _selectedTile.value = drawn
        }
    }

    // Actually discard the tile
    fun executeUserDiscard(tile: MahjongTile?) {
        if (tile == null) return
        val player = _players.value[0]
        val playersCopy = _players.value.toMutableList()

        val hasDrawn = player.hasDrawnTile
        val finalHand = mutableListOf<MahjongTile>()
        finalHand.addAll(player.hand)
        if (hasDrawn != null) {
            finalHand.add(hasDrawn)
        }

        // Remove the selected tile
        val index = finalHand.indexOfFirst { it.id == tile.id }
        if (index != -1) {
            finalHand.removeAt(index)
        }

        playersCopy[0] = player.copy(
            hand = finalHand.sorted(),
            hasDrawnTile = null,
            discards = player.discards + tile
        )

        _players.value = playersCopy
        _selectedTile.value = null
        _lastDiscard.value = tile
        _discarderIndex.value = 0
        _activePlayerIndex.value = -1 // Transition state

        SoundManager.playDiscard()
        addLog("你打出了 [${tile.displayName}]")

        // Trigger evaluations for other players to Peng / Gang / Hu
        evaluateDiscardClaims(tile, 0)
    }

    // AI Turn actions
    private fun triggerAITurn(aiIdx: Int, drawnTile: MahjongTile) {
        aiTurnJob = viewModelScope.launch {
            val playersCopy = _players.value.toMutableList()
            val aiPlayer = playersCopy[aiIdx]

            // Mark status thinking
            playersCopy[aiIdx] = aiPlayer.copy(activeStatus = "思考摸牌中...")
            _players.value = playersCopy
            delay(_playSpeedMs.value)

            val currentAiPlayer = _players.value[aiIdx]
            val hand = currentAiPlayer.hand

            // 1. Check AI Zimo (自摸)
            val wildDetails = getWildCardDetails()
            val huEval = MahjongEvaluator.checkHu(
                handTiles = hand,
                declaredMelds = currentAiPlayer.declaredMelds,
                winningTile = drawnTile,
                isSelfDraw = true,
                wildTileType = wildDetails.first,
                wildTileValue = wildDetails.second
            )
            if (huEval.isWin) {
                playersCopy[aiIdx] = currentAiPlayer.copy(activeStatus = "自摸胡！")
                _players.value = playersCopy
                delay(800)
                handleWin(aiIdx, aiIdx, drawnTile, huEval, isSelfDraw = true)
                return@launch
            }

            // 2. Check AI self Gang (暗杠 / 补杠)
            val combinedHand = hand + drawnTile
            val anGangGroup = combinedHand.groupBy { it.displayName }.filter { it.value.size == 4 }
            val buGangCandidate = currentAiPlayer.declaredMelds
                .filterIsInstance<MahjongMeld.Peng>()
                .firstOrNull { it.tiles.first().isSameTile(drawnTile) }

            if (anGangGroup.isNotEmpty()) {
                val gangKey = anGangGroup.keys.first()
                val gangTiles = combinedHand.filter { it.displayName == gangKey }
                val updatedHand = combinedHand.filterNot { it.displayName == gangKey }
                
                val newMeld = MahjongMeld.Gang(gangTiles, isAn = true)
                playersCopy[aiIdx] = currentAiPlayer.copy(
                    hand = updatedHand.sorted(),
                    hasDrawnTile = null,
                    declaredMelds = currentAiPlayer.declaredMelds + newMeld,
                    activeStatus = "暗杠!"
                )
                _players.value = playersCopy
                addLog("${currentAiPlayer.name} 申明暗杠: [$gangKey]")
                delay(800)
                drawTileForPlayer(aiIdx) // Draw replacement
                return@launch
            } else if (buGangCandidate != null) {
                val updatedMelds = currentAiPlayer.declaredMelds.map { m ->
                    if (m is MahjongMeld.Peng && m.tiles.first().isSameTile(drawnTile)) {
                        MahjongMeld.Gang(m.tiles + drawnTile, isAn = false)
                    } else {
                        m
                    }
                }
                playersCopy[aiIdx] = currentAiPlayer.copy(
                    hasDrawnTile = null,
                    declaredMelds = updatedMelds,
                    activeStatus = "补杠!"
                )
                _players.value = playersCopy
                addLog("${currentAiPlayer.name} 执行补杠: [${drawnTile.displayName}]")
                delay(800)
                drawTileForPlayer(aiIdx)
                return@launch
            }

            // 3. Normal Discard
            val discardChoice = MahjongAI.selectDiscardTile(
                combinedHand,
                wildTileType = wildDetails.first,
                wildTileValue = wildDetails.second
            )
            val finalHandCopy = combinedHand.toMutableList()
            finalHandCopy.remove(discardChoice)

            playersCopy[aiIdx] = currentAiPlayer.copy(
                hand = finalHandCopy.sorted(),
                hasDrawnTile = null,
                discards = currentAiPlayer.discards + discardChoice,
                activeStatus = ""
            )
            _players.value = playersCopy

            SoundManager.playDiscard()
            _lastDiscard.value = discardChoice
            _discarderIndex.value = aiIdx
            addLog("${currentAiPlayer.name} 打出了 [${discardChoice.displayName}]")

            // Evaluate other claims
            evaluateDiscardClaims(discardChoice, aiIdx)
        }
    }

    // Evaluate who can claim a discarded tile
    private fun evaluateDiscardClaims(tile: MahjongTile, discarderIdx: Int) {
        viewModelScope.launch {
            // Check for HU claims first, which takes highest priority in counter-clockwise order
            for (step in 1..3) {
                val claimerIdx = (discarderIdx + step) % 4
                val claimer = _players.value[claimerIdx]

                val wildDetails = getWildCardDetails()
                val huCheck = MahjongEvaluator.checkHu(
                    handTiles = claimer.hand,
                    declaredMelds = claimer.declaredMelds,
                    winningTile = tile,
                    isSelfDraw = false,
                    wildTileType = wildDetails.first,
                    wildTileValue = wildDetails.second
                )

                if (huCheck.isWin) {
                    if (claimerIdx == 0) {
                        // User can win on this discard!
                        _activePlayerIndex.value = 0
                        _actionsAvailable.value = UserActions(
                            canHu = true,
                            canPass = true,
                            targetTile = tile,
                            isSelfDraw = false
                        )
                        addLog("[${_players.value[discarderIdx].name}] 打出的 [${tile.displayName}]，你可以胡！")
                        return@launch // Suspend for User prompt
                    } else {
                        // AI Wins on discard!
                        addLog("${claimer.name} 截获胡牌！")
                        delay(600)
                        handleWin(claimerIdx, discarderIdx, tile, huCheck, isSelfDraw = false)
                        return@launch
                    }
                }
            }

            // If no one claims HU, evaluate Peng / Gang
            // Only one person can have Peng/Gang because of total 4 cards per identity limit
            for (step in 1..3) {
                val claimerIdx = (discarderIdx + step) % 4
                val claimer = _players.value[claimerIdx]

                val canGang = MahjongAI.shouldGang(claimer.hand, tile, isSelfDraw = false)
                val canPeng = MahjongAI.shouldPeng(claimer.hand, tile)

                if (claimerIdx == 0 && (canGang || canPeng)) {
                    // User has claiming rights
                    _activePlayerIndex.value = 0
                    _actionsAvailable.value = UserActions(
                        canPeng = canPeng,
                        canGang = canGang,
                        canPass = true,
                        targetTile = tile,
                        isSelfDraw = false
                    )
                    addLog("你可以对 [${tile.displayName}] 执行 碰/杠！")
                    return@launch // Suspend for user selection
                } else if (claimerIdx > 0 && (canGang || canPeng)) {
                    // AI takes claim automatically
                    executeAIClaim(claimerIdx, discarderIdx, tile, canGang)
                    return@launch
                }
            }

            // No claims made: turn proceeds to next player to draw
            val nextIdx = (discarderIdx + 1) % 4
            delay(_playSpeedMs.value / 2)
            drawTileForPlayer(nextIdx)
        }
    }

    // User executes a discard win (点炮胡)
    fun executeUserDiscardHu() {
        val actions = _actionsAvailable.value
        if (!actions.canHu || actions.targetTile == null) return
        val tile = actions.targetTile
        val discarder = _discarderIndex.value ?: return

        val player = _players.value[0]
        viewModelScope.launch {
            val wildDetails = getWildCardDetails()
            val winEval = MahjongEvaluator.checkHu(
                handTiles = player.hand,
                declaredMelds = player.declaredMelds,
                winningTile = tile,
                isSelfDraw = false,
                wildTileType = wildDetails.first,
                wildTileValue = wildDetails.second
            )

            handleWin(0, discarder, tile, winEval, isSelfDraw = false)
            _actionsAvailable.value = UserActions()
        }
    }

    // User claims Peng (碰)
    fun executeUserPengClaim() {
        val actions = _actionsAvailable.value
        val tile = actions.targetTile ?: return
        val discarder = _discarderIndex.value ?: return

        val player = _players.value[0]
        val playersCopy = _players.value.toMutableList()

        // Remove 2 matching tiles from hand
        val matchTiles = player.hand.filter { it.isSameTile(tile) }.take(2)
        val remainingHand = player.hand.toMutableList()
        matchTiles.forEach { remainingHand.remove(it) }

        val newMeld = MahjongMeld.Peng(matchTiles + tile)

        // Remote last discard from the discarder's list
        val updatedDiscarder = removeLastDiscard(discarder)
        playersCopy[discarder] = updatedDiscarder

        // Update active player's hand and melds
        playersCopy[0] = player.copy(
            hand = remainingHand.sorted(),
            hasDrawnTile = null,
            declaredMelds = player.declaredMelds + newMeld
        )

        _players.value = playersCopy
        _actionsAvailable.value = UserActions()
        _lastDiscard.value = null
        _discarderIndex.value = null
        _activePlayerIndex.value = 0
        _selectedTile.value = null

        SoundManager.playClaim()
        addLog("你碰了 [${tile.displayName}]")

        // Player must discard immediately
    }

    // User claims Gang (明杠)
    fun executeUserGangClaim() {
        val actions = _actionsAvailable.value
        val tile = actions.targetTile ?: return
        val discarder = _discarderIndex.value ?: return

        val player = _players.value[0]
        val playersCopy = _players.value.toMutableList()

        // Remove 3 matching tiles from hand
        val matchTiles = player.hand.filter { it.isSameTile(tile) }.take(3)
        val remainingHand = player.hand.toMutableList()
        matchTiles.forEach { remainingHand.remove(it) }

        val newMeld = MahjongMeld.Gang(matchTiles + tile, isAn = false)

        // Remove discarder's tile
        val updatedDiscarder = removeLastDiscard(discarder)
        playersCopy[discarder] = updatedDiscarder

        playersCopy[0] = player.copy(
            hand = remainingHand.sorted(),
            hasDrawnTile = null,
            declaredMelds = player.declaredMelds + newMeld
        )

        _players.value = playersCopy
        _actionsAvailable.value = UserActions()
        _lastDiscard.value = null
        _discarderIndex.value = null
        _selectedTile.value = null

        SoundManager.playClaim()
        addLog("你杠了 [${tile.displayName}]")

        viewModelScope.launch {
            delay(500)
            drawTileForPlayer(0) // Draw replacement card
        }
    }

    // User chooses to PASS the claim opportunity (放弃碰/杠/胡)
    fun executeUserPassClaim() {
        val actions = _actionsAvailable.value
        if (!actions.canPass) return
        _actionsAvailable.value = UserActions()
        val discarder = _discarderIndex.value ?: return
        val tile = _lastDiscard.value ?: return

        addLog("你选择过牌")

        // Advance evaluations for other AI after User passed
        viewModelScope.launch {
            delay(300)
            // Continue checks for remaining players in rotation
            var startStep = 1
            // Determine our offset step relative to the discarder
            for (step in 1..3) {
                if ((discarder + step) % 4 == 0) {
                    startStep = step + 1
                    break
                }
            }

            for (step in startStep..3) {
                val claimerIdx = (discarder + step) % 4
                val claimer = _players.value[claimerIdx]

                val canGang = MahjongAI.shouldGang(claimer.hand, tile, isSelfDraw = false)
                val canPeng = MahjongAI.shouldPeng(claimer.hand, tile)

                if (canGang || canPeng) {
                    executeAIClaim(claimerIdx, discarder, tile, canGang)
                    return@launch
                }
            }

            // No secondary AI claims, draw card
            val nextIdx = (discarder + 1) % 4
            drawTileForPlayer(nextIdx)
        }
    }

    private fun removeLastDiscard(playerIdx: Int): MahjongPlayer {
        val p = _players.value[playerIdx]
        val discardsCopy = p.discards.toMutableList()
        if (discardsCopy.isNotEmpty()) {
            discardsCopy.removeAt(discardsCopy.size - 1)
        }
        return p.copy(discards = discardsCopy)
    }

    // AI claims a discarded tile
    private suspend fun executeAIClaim(aiIdx: Int, discarderIdx: Int, tile: MahjongTile, isGang: Boolean) {
        val playersCopy = _players.value.toMutableList()
        val aiPlayer = playersCopy[aiIdx]

        // Prepare the action message status
        val claimMsg = if (isGang) "杠!" else "碰!"
        playersCopy[aiIdx] = aiPlayer.copy(activeStatus = claimMsg)
        _players.value = playersCopy
        SoundManager.playClaim()
        delay(600)

        val currentAiPlayer = _players.value[aiIdx]
        val matchingCount = if (isGang) 3 else 2
        val matchTiles = currentAiPlayer.hand.filter { it.isSameTile(tile) }.take(matchingCount)
        val remainingHand = currentAiPlayer.hand.toMutableList()
        matchTiles.forEach { remainingHand.remove(it) }

        val newMeld = if (isGang) {
            MahjongMeld.Gang(matchTiles + tile, isAn = false)
        } else {
            MahjongMeld.Peng(matchTiles + tile)
        }

        // Apply clean references
        val updatedPlayers = _players.value.toMutableList()
        // 1. Remove tile from previous discarder
        val updatedDiscarder = removeLastDiscard(discarderIdx)
        updatedPlayers[discarderIdx] = updatedDiscarder

        // 2. Insert meld into AI
        updatedPlayers[aiIdx] = currentAiPlayer.copy(
            hand = remainingHand.sorted(),
            hasDrawnTile = null,
            declaredMelds = currentAiPlayer.declaredMelds + newMeld,
            activeStatus = ""
        )

        _players.value = updatedPlayers
        _lastDiscard.value = null
        _discarderIndex.value = null
        addLog("${currentAiPlayer.name} ${claimMsg} [${tile.displayName}]")

        if (isGang) {
            delay(400)
            drawTileForPlayer(aiIdx) // AI draws replacement for Gang
        } else {
            // Trigger normal AI turn to choose discard
            _activePlayerIndex.value = aiIdx
            triggerAITurnAfterClaim(aiIdx, null)
        }
    }

    // Trigger AI normal turn WITHOUT a fresh draw (e.g. after Peng, already has 14 tiles)
    private fun triggerAITurnAfterClaim(aiIdx: Int, dummyDraw: MahjongTile?) {
        aiTurnJob = viewModelScope.launch {
            val playersCopy = _players.value.toMutableList()
            val aiPlayer = playersCopy[aiIdx]
            playersCopy[aiIdx] = aiPlayer.copy(activeStatus = "思考出口中...")
            _players.value = playersCopy
            delay(_playSpeedMs.value)

            val currentAiPlayer = _players.value[aiIdx]
            val wildDetails = getWildCardDetails()
            val discardChoice = MahjongAI.selectDiscardTile(
                currentAiPlayer.hand,
                wildTileType = wildDetails.first,
                wildTileValue = wildDetails.second
            )
            val finalHandCopy = currentAiPlayer.hand.toMutableList()
            finalHandCopy.remove(discardChoice)

            playersCopy[aiIdx] = currentAiPlayer.copy(
                hand = finalHandCopy.sorted(),
                discards = currentAiPlayer.discards + discardChoice,
                activeStatus = ""
            )
            _players.value = playersCopy

            SoundManager.playDiscard()
            _lastDiscard.value = discardChoice
            _discarderIndex.value = aiIdx
            addLog("${currentAiPlayer.name} 打出了 [${discardChoice.displayName}]")

            evaluateDiscardClaims(discardChoice, aiIdx)
        }
    }

    // Handle standard Hu / Game winning outcome
    private fun handleWin(
        winnerIdx: Int,
        responsibleIdx: Int,
        winningTile: MahjongTile,
        winResult: MahjongEvaluator.WinResult,
        isSelfDraw: Boolean
    ) {
        SoundManager.playWin()
        val playersCopy = _players.value.map { it.copy(activeStatus = "") }.toMutableList()
        val winner = playersCopy[winnerIdx]

        // Point/Chip Calculation Rules (Guangdong 'Tui Dao Hu' simplified formulas)
        // 1番 = 10 chips
        val fan = winResult.fan
        val basePoints = fan * 10
        val chipChanges = MutableList(4) { 0 }

        val winTypeStr = if (isSelfDraw) "自摸" else "点炮胡"

        val descriptionStr = if (isSelfDraw) {
            // In Zimo, winner gets basePoints from EVERY opponent
            var totalWinnerGain = 0
            for (idx in 0..3) {
                if (idx != winnerIdx) {
                    val actualPayment = minOf(playersCopy[idx].chips, basePoints)
                    chipChanges[idx] = -actualPayment
                    totalWinnerGain += actualPayment
                }
            }
            chipChanges[winnerIdx] = totalWinnerGain
            "${winner.name} 自摸 [${winningTile.displayName}] Wins! 他向每家收取 ${basePoints} 筹码。"
        } else {
            // In Dianpao, only the player who discarded pays basePoints
            val actualPayment = minOf(playersCopy[responsibleIdx].chips, basePoints)
            chipChanges[responsibleIdx] = -actualPayment
            chipChanges[winnerIdx] = actualPayment
            "${winner.name} 截胡 [${winningTile.displayName}]！ 点炮者: ${playersCopy[responsibleIdx].name}。 他需赔付 ${basePoints} 筹码。"
        }

        // Apply updated chips
        val finalPlayers = playersCopy.mapIndexed { idx, player ->
            player.copy(chips = maxOf(0, player.chips + chipChanges[idx]))
        }
        _players.value = finalPlayers

        // Construct complete result details
        val outcome = RoundResult(
            winnerIndex = winnerIdx,
            winnerName = winner.name,
            winType = winTypeStr,
            handName = winResult.handName,
            fan = fan,
            details = winResult.details,
            winningTile = winningTile,
            winningHand = winner.hand,
            chipChanges = chipChanges,
            description = descriptionStr
        )
        _roundResult.value = outcome
        _gameState.value = GameState.REVEAL_WIN

        addLog("【局终】$descriptionStr")

        // Write this round record to the database
        viewModelScope.launch {
            val record = GameRecord(
                winnerName = winner.name,
                winnerEmoji = winner.emoji,
                winType = winTypeStr,
                handName = winResult.handName,
                fan = fan,
                chipChangePlayer0 = chipChanges[0],
                chipChangePlayer1 = chipChanges[1],
                chipChangePlayer2 = chipChanges[2],
                chipChangePlayer3 = chipChanges[3],
                finalBalancePlayer0 = finalPlayers[0].chips
            )
            repository.insertRecord(record)
        }

        // Transfer Dealer button!
        // If dealer wins, they keep the deal (连庄). If opponent wins, dealer rotates counter-clockwise.
        if (winnerIdx != _dealerIndex.value && !isSelfDraw) {
            _dealerIndex.value = (_dealerIndex.value + 1) % 4
        } else if (winnerIdx != _dealerIndex.value) {
            // Zimo rotates dealer too
            _dealerIndex.value = (_dealerIndex.value + 1) % 4
        }
    }

    // Tie outcome when remaining cards deplete
    private fun handleTieGame() {
        val outcome = RoundResult(
            winnerIndex = -1,
            winnerName = "流局",
            winType = "流局",
            handName = "流局",
            fan = 0,
            details = listOf("荒庄流局，本关互不结算。"),
            winningTile = null,
            winningHand = emptyList(),
            chipChanges = listOf(0, 0, 0, 0),
            description = "牌堆耗尽！无人胡牌，大家握手言和。"
        )
        _roundResult.value = outcome
        _gameState.value = GameState.REVEAL_WIN

        addLog("【局终】荒庄流局，牌堆耗尽！")

        // Dealer rotates to next position on a tie
        _dealerIndex.value = (_dealerIndex.value + 1) % 4
    }

    companion object {
        fun provideFactory(repository: GameRecordRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MahjongViewModel(repository) as T
                }
            }
    }
}
