package com.example.chess.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.api.GeminiChessOpponent
import com.example.chess.api.GeminiChessMove
import com.example.chess.db.ChessDatabase
import com.example.chess.db.ChessMatchRecord
import com.example.chess.db.ChessRepository
import com.example.chess.engine.ChessAI
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessMove
import com.example.chess.engine.PieceSide
import com.example.chess.engine.PieceType
import com.example.chess.engine.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChessRepository
    val gameHistory: StateFlow<List<ChessMatchRecord>>

    init {
        val database = ChessDatabase.getDatabase(application)
        repository = ChessRepository(database.chessMatchDao())
        gameHistory = repository.gameHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Interactive Chess Board
    var chessBoard by mutableStateOf(ChessBoard())
        private set

    // Human Preferences
    var playerSide by mutableStateOf(PieceSide.WHITE)
    var aiLevel by mutableStateOf(2) // 1: Easy, 2: Medium, 3: Hard, 4: Gemini GM
    var isFlipped by mutableStateOf(false) // Whether board is visually rotated for Black at bottom

    // Board Interactive UI States
    var selectedSquare by mutableStateOf<Position?>(null)
    var legalTargets by mutableStateOf<List<Position>>(emptyList())
    var lastMove by mutableStateOf<ChessMove?>(null)

    // AI Processing States
    var isThinking by mutableStateOf(false)
    var geminiThought by mutableStateOf("")
    var geminiCommentary by mutableStateOf("Welcome to the Chess Arena! Choose your difficulty above or toggle Gemini GM mode to match wits against a strategic sage.")
    var showApiKeyWarning by mutableStateOf(false)

    // Current Status Messages
    val currentTurnText: String
        get() = if (chessBoard.isGameOver) {
            when {
                chessBoard.isDraw -> "Game Drawn (${chessBoard.drawReason ?: "Draw"})"
                chessBoard.winner == PieceSide.WHITE -> "Checkmate! WHITE is Victorious!"
                chessBoard.winner == PieceSide.BLACK -> "Checkmate! BLACK is Victorious!"
                else -> "Game Over!"
            }
        } else {
            val status = if (chessBoard.isInCheck(chessBoard.activeSide)) " [IN CHECK]" else ""
            "Active Turn: ${chessBoard.activeSide.name}$status"
        }

    fun selectSquare(pos: Position) {
        if (chessBoard.isGameOver || isThinking) return
        if (chessBoard.activeSide != playerSide) return // Not user's turn

        val piece = chessBoard.getPiece(pos)
        val selected = selectedSquare

        if (selected == null) {
            // First tap: Select piece
            if (piece != null && piece.side == playerSide) {
                selectedSquare = pos
                legalTargets = chessBoard.getLegalMoves(playerSide)
                    .filter { it.from == pos }
                    .map { it.to }
            }
        } else {
            // Second tap: Intended move target
            if (pos == selected) {
                // Deselect
                clearSelection()
            } else if (piece != null && piece.side == playerSide) {
                // Tap another own piece: Swapping selection
                selectedSquare = pos
                legalTargets = chessBoard.getLegalMoves(playerSide)
                    .filter { it.from == pos }
                    .map { it.to }
            } else {
                // Try to make move
                val possibleMove = chessBoard.getLegalMoves(playerSide).find {
                    it.from == selected && it.to == pos
                }

                if (possibleMove != null) {
                    executeUserMove(possibleMove)
                } else {
                    clearSelection()
                }
            }
        }
    }

    private fun clearSelection() {
        selectedSquare = null
        legalTargets = emptyList()
    }

    private fun executeUserMove(move: ChessMove) {
        val success = chessBoard.makeMove(move)
        if (success) {
            lastMove = move
            clearSelection()
            triggerAiIfNeeded()
        }
    }

    private fun triggerAiIfNeeded() {
        if (chessBoard.isGameOver) {
            saveGameRecord()
            return
        }

        if (chessBoard.activeSide != playerSide) {
            isThinking = true
            viewModelScope.launch {
                if (aiLevel == 4) {
                    makeGeminiMove()
                } else {
                    makeLocalAiMove()
                }
            }
        }
    }

    private suspend fun makeLocalAiMove() {
        withContext(Dispatchers.Default) {
            // Artificial tiny delay to make AI calculations feel realistic (e.g. 500ms)
            kotlinx.coroutines.delay(500)
            val move = ChessAI.getBestMove(chessBoard, aiLevel)
            withContext(Dispatchers.Main) {
                if (move != null) {
                    val success = chessBoard.makeMove(move)
                    if (success) {
                        lastMove = move
                    }
                }
                isThinking = false
                if (chessBoard.isGameOver) {
                    saveGameRecord()
                }
            }
        }
    }

    private suspend fun makeGeminiMove() {
        val currentFEN = chessBoard.toFEN()
        val readableBoardGrid = chessBoard.toReadableGrid()
        val currentActiveSide = chessBoard.activeSide
        val legalMovesList = chessBoard.getLegalMoves(currentActiveSide)
        val stringMoves = legalMovesList.map { it.toAlgebraic() }

        if (stringMoves.isEmpty()) {
            withContext(Dispatchers.Main) {
                isThinking = false
            }
            return
        }

        withContext(Dispatchers.IO) {
            val personalChoice = listOf("Grandmaster Magnus", "Strategic Sage Kassparov", "The Witty Chess AI").random()
            val geminiResponse = GeminiChessOpponent.getMoveFromGemini(
                fen = currentFEN,
                readableBoard = readableBoardGrid,
                sideToPlay = currentActiveSide.name,
                legalMoves = stringMoves,
                aiPersonality = personalChoice
            )

            withContext(Dispatchers.Main) {
                if (geminiResponse != null) {
                    geminiThought = geminiResponse.thought
                    geminiCommentary = geminiResponse.commentary

                    // Parse the move coordinates (e.g., "e7e5") back to high-level Move objects
                    val selectedAlgebraic = geminiResponse.move
                    val matchedMove = legalMovesList.find { it.toAlgebraic() == selectedAlgebraic }

                    if (matchedMove != null) {
                        val success = chessBoard.makeMove(matchedMove)
                        if (success) {
                            lastMove = matchedMove
                        }
                    } else {
                        // Safe robust fallback if Gemini chose an unsupported move coordinates format
                        val fallbackMove = legalMovesList.firstOrNull()
                        if (fallbackMove != null) {
                            val success = chessBoard.makeMove(fallbackMove)
                            if (success) lastMove = fallbackMove
                        }
                    }
                } else {
                    // Total fallback to local depth-3 if Gemini service fails offline or lacks API key
                    val bestLocalMove = ChessAI.getBestMove(chessBoard, 3)
                    if (bestLocalMove != null) {
                        val success = chessBoard.makeMove(bestLocalMove)
                        if (success) {
                            lastMove = bestLocalMove
                        }
                    }
                    geminiCommentary = "My planetary network link is temporarily busy or your API key is inactive. I will play using my strong local offline tactical core!"
                }

                showApiKeyWarning = com.example.BuildConfig.GEMINI_API_KEY.isEmpty() || com.example.BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY"

                isThinking = false
                if (chessBoard.isGameOver) {
                    saveGameRecord()
                }
            }
        }
    }

    fun resetGame() {
        chessBoard.reset()
        lastMove = null
        clearSelection()
        isThinking = false
        geminiThought = ""
        geminiCommentary = "Game Reset! Select your side or difficulty above and make your opening move!"
        showApiKeyWarning = false

        // If player chose Black side, AI goes first immediately
        if (playerSide == PieceSide.BLACK) {
            triggerAiIfNeeded()
        }
    }

    fun undoMove() {
        if (isThinking) return
        // We rollback 2 moves (AI move and Human move) unless human is playing alone (which they are white/black but AI triggers, so it's always 2 moves)
        val undoneSelf = chessBoard.undoMove()
        if (undoneSelf) {
            if (chessBoard.activeSide != playerSide) {
                chessBoard.undoMove() // Undo AI's move as well
            }
        }
        lastMove = chessBoard.moveHistory.lastOrNull()
        clearSelection()
        geminiCommentary = "Time wound backwards! Let's choose an alternative strategy."
    }

    fun swapSides() {
        if (isThinking) return
        playerSide = playerSide.opponent()
        isFlipped = (playerSide == PieceSide.BLACK)
        resetGame()
    }

    private fun saveGameRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            val opponentName = when (aiLevel) {
                1 -> "AI Easy (Lvl 1)"
                2 -> "AI Medium (Lvl 2)"
                3 -> "AI Hard (Lvl 3)"
                4 -> "Gemini Grandmaster"
                else -> "Computer"
            }

            val resultText = when {
                chessBoard.isDraw -> "Draw"
                chessBoard.winner == playerSide -> "Won"
                else -> "Lost"
            }

            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = format.format(Date())

            val movesStr = chessBoard.moveHistory.joinToString("-") { it.toAlgebraic() }

            val record = ChessMatchRecord(
                opponentName = opponentName,
                playerColor = playerSide.name,
                gameResult = resultText,
                totalMoves = chessBoard.moveHistory.size,
                dateString = dateStr,
                moveList = movesStr
            )
            repository.insertMatch(record)
        }
    }

    fun clearDbHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }
}
