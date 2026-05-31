package com.example.chess.engine

import kotlin.math.abs

enum class PieceType(val value: Int) {
    PAWN(100),
    KNIGHT(320),
    BISHOP(330),
    ROOK(500),
    QUEEN(900),
    KING(20000)
}

enum class PieceSide {
    WHITE, BLACK;
    fun opponent() = if (this == WHITE) BLACK else WHITE
}

data class ChessPiece(val type: PieceType, val side: PieceSide) {
    fun getSymbol(): String {
        return when (type) {
            PieceType.PAWN -> if (side == PieceSide.WHITE) "♙" else "♟"
            PieceType.KNIGHT -> if (side == PieceSide.WHITE) "♘" else "♞"
            PieceType.BISHOP -> if (side == PieceSide.WHITE) "♗" else "♝"
            PieceType.ROOK -> if (side == PieceSide.WHITE) "♖" else "♜"
            PieceType.QUEEN -> if (side == PieceSide.WHITE) "♕" else "♛"
            PieceType.KING -> if (side == PieceSide.WHITE) "♔" else "♚"
        }
    }
}

data class Position(val row: Int, val col: Int) {
    fun isValid() = row in 0..7 && col in 0..7
    override fun toString(): String {
        val fileChar = ('a' + col)
        val rankChar = ('8' - row)
        return "$fileChar$rankChar"
    }
}

data class ChessMove(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCheck: Boolean = false
) {
    fun toAlgebraic(): String {
        return "$from$to"
    }
}

class ChessBoard {
    var grid: Array<Array<ChessPiece?>> = Array(8) { Array(8) { null } }
    var activeSide: PieceSide = PieceSide.WHITE
    var moveHistory: MutableList<ChessMove> = mutableListOf()
    var isGameOver: Boolean = false
    var winner: PieceSide? = null
    var isDraw: Boolean = false
    var drawReason: String? = null

    init {
        reset()
    }

    fun reset() {
        grid = Array(8) { Array(8) { null } }
        activeSide = PieceSide.WHITE
        moveHistory.clear()
        isGameOver = false
        winner = null
        isDraw = false
        drawReason = null

        // Set up major pieces for Black (Row 0)
        grid[0][0] = ChessPiece(PieceType.ROOK, PieceSide.BLACK)
        grid[0][1] = ChessPiece(PieceType.KNIGHT, PieceSide.BLACK)
        grid[0][2] = ChessPiece(PieceType.BISHOP, PieceSide.BLACK)
        grid[0][3] = ChessPiece(PieceType.QUEEN, PieceSide.BLACK)
        grid[0][4] = ChessPiece(PieceType.KING, PieceSide.BLACK)
        grid[0][5] = ChessPiece(PieceType.BISHOP, PieceSide.BLACK)
        grid[0][1] = ChessPiece(PieceType.KNIGHT, PieceSide.BLACK) // wait, index 1 and 6
        grid[0][6] = ChessPiece(PieceType.KNIGHT, PieceSide.BLACK)
        grid[0][7] = ChessPiece(PieceType.ROOK, PieceSide.BLACK)

        // Black pawns (Row 1)
        for (col in 0..7) {
            grid[1][col] = ChessPiece(PieceType.PAWN, PieceSide.BLACK)
        }

        // White pawns (Row 6)
        for (col in 0..7) {
            grid[6][col] = ChessPiece(PieceType.PAWN, PieceSide.WHITE)
        }

        // Set up major pieces for White (Row 7)
        grid[7][0] = ChessPiece(PieceType.ROOK, PieceSide.WHITE)
        grid[7][1] = ChessPiece(PieceType.KNIGHT, PieceSide.WHITE)
        grid[7][2] = ChessPiece(PieceType.BISHOP, PieceSide.WHITE)
        grid[7][3] = ChessPiece(PieceType.QUEEN, PieceSide.WHITE)
        grid[7][4] = ChessPiece(PieceType.KING, PieceSide.WHITE)
        grid[7][5] = ChessPiece(PieceType.BISHOP, PieceSide.WHITE)
        grid[7][6] = ChessPiece(PieceType.KNIGHT, PieceSide.WHITE)
        grid[7][7] = ChessPiece(PieceType.ROOK, PieceSide.WHITE)
    }

    fun getPiece(pos: Position): ChessPiece? {
        return if (pos.isValid()) grid[pos.row][pos.col] else null
    }

    fun copyGrid(): Array<Array<ChessPiece?>> {
        return Array(8) { r ->
            Array(8) { c ->
                grid[r][c]
            }
        }
    }

    /**
     * Generates all moves that are physical movement matches for the pieces,
     * regardless of whether they leave the king in check.
     */
    fun generatePseudoLegalMoves(side: PieceSide, customGrid: Array<Array<ChessPiece?>> = grid): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = customGrid[r][c] ?: continue
                if (piece.side != side) continue
                val pos = Position(r, c)

                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(pos, side, customGrid, moves)
                    PieceType.KNIGHT -> generateKnightMoves(pos, side, customGrid, moves)
                    PieceType.BISHOP -> generateSlidingMoves(pos, side, customGrid, moves, diagonalDirs)
                    PieceType.ROOK -> generateSlidingMoves(pos, side, customGrid, moves, straightDirs)
                    PieceType.QUEEN -> generateSlidingMoves(pos, side, customGrid, moves, allDirs)
                    PieceType.KING -> generateKingMoves(pos, side, customGrid, moves)
                }
            }
        }
        return moves
    }

    private val diagonalDirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
    private val straightDirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
    private val allDirs = diagonalDirs + straightDirs

    private fun generatePawnMoves(
        from: Position,
        side: PieceSide,
        currentGrid: Array<Array<ChessPiece?>>,
        moves: MutableList<ChessMove>
    ) {
        val direction = if (side == PieceSide.WHITE) -1 else 1
        val startRow = if (side == PieceSide.WHITE) 6 else 1
        val promoteRow = if (side == PieceSide.WHITE) 0 else 7

        // 1. One square forward
        val forwardPos = Position(from.row + direction, from.col)
        if (forwardPos.isValid() && currentGrid[forwardPos.row][forwardPos.col] == null) {
            if (forwardPos.row == promoteRow) {
                // Pawn promotion options
                moves.add(ChessMove(from, forwardPos, PieceType.QUEEN))
                moves.add(ChessMove(from, forwardPos, PieceType.ROOK))
                moves.add(ChessMove(from, forwardPos, PieceType.BISHOP))
                moves.add(ChessMove(from, forwardPos, PieceType.KNIGHT))
            } else {
                moves.add(ChessMove(from, forwardPos))
            }

            // 2. Two squares forward from starting line
            val doubleForwardPos = Position(from.row + 2 * direction, from.col)
            if (from.row == startRow && currentGrid[doubleForwardPos.row][doubleForwardPos.col] == null) {
                moves.add(ChessMove(from, doubleForwardPos))
            }
        }

        // 3. Captures
        val captureCols = listOf(from.col - 1, from.col + 1)
        for (c in captureCols) {
            val capturePos = Position(from.row + direction, c)
            if (capturePos.isValid()) {
                val target = currentGrid[capturePos.row][capturePos.col]
                if (target != null && target.side != side) {
                    if (capturePos.row == promoteRow) {
                        moves.add(ChessMove(from, capturePos, PieceType.QUEEN, isCapture = true))
                        moves.add(ChessMove(from, capturePos, PieceType.ROOK, isCapture = true))
                        moves.add(ChessMove(from, capturePos, PieceType.BISHOP, isCapture = true))
                        moves.add(ChessMove(from, capturePos, PieceType.KNIGHT, isCapture = true))
                    } else {
                        moves.add(ChessMove(from, capturePos, isCapture = true))
                    }
                }
            }
        }
    }

    private fun generateKnightMoves(
        from: Position,
        side: PieceSide,
        currentGrid: Array<Array<ChessPiece?>>,
        moves: MutableList<ChessMove>
    ) {
        val jumps = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (j in jumps) {
            val to = Position(from.row + j.first, from.col + j.second)
            if (to.isValid()) {
                val target = currentGrid[to.row][to.col]
                if (target == null) {
                    moves.add(ChessMove(from, to))
                } else if (target.side != side) {
                    moves.add(ChessMove(from, to, isCapture = true))
                }
            }
        }
    }

    private fun generateSlidingMoves(
        from: Position,
        side: PieceSide,
        currentGrid: Array<Array<ChessPiece?>>,
        moves: MutableList<ChessMove>,
        directions: List<Pair<Int, Int>>
    ) {
        for (dir in directions) {
            var step = 1
            while (true) {
                val to = Position(from.row + dir.first * step, from.col + dir.second * step)
                if (!to.isValid()) break

                val target = currentGrid[to.row][to.col]
                if (target == null) {
                    moves.add(ChessMove(from, to))
                } else {
                    if (target.side != side) {
                        moves.add(ChessMove(from, to, isCapture = true))
                    }
                    break // Blocked by piece
                }
                step++
            }
        }
    }

    private fun generateKingMoves(
        from: Position,
        side: PieceSide,
        currentGrid: Array<Array<ChessPiece?>>,
        moves: MutableList<ChessMove>
    ) {
        for (rOffset in -1..1) {
            for (cOffset in -1..1) {
                if (rOffset == 0 && cOffset == 0) continue
                val to = Position(from.row + rOffset, from.col + cOffset)
                if (to.isValid()) {
                    val target = currentGrid[to.row][to.col]
                    if (target == null) {
                        moves.add(ChessMove(from, to))
                    } else if (target.side != side) {
                        moves.add(ChessMove(from, to, isCapture = true))
                    }
                }
            }
        }
    }

    /**
     * Determines if the King of the given side is currently under check.
     */
    fun isInCheck(side: PieceSide, currentGrid: Array<Array<ChessPiece?>> = grid): Boolean {
        // 1. Locate King
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = currentGrid[r][c]
                if (p != null && p.type == PieceType.KING && p.side == side) {
                    kingPos = Position(r, c)
                    break
                }
            }
            if (kingPos != null) break
        }

        if (kingPos == null) return false // No king? Shouldn't happen in real games but safe check

        // 2. Check if opponent can attack King
        val opponentSide = side.opponent()
        val opponentMoves = generatePseudoLegalMoves(opponentSide, currentGrid)
        for (m in opponentMoves) {
            if (m.to == kingPos) {
                return true
            }
        }
        return false
    }

    /**
     * Obtains strictly legal moves. Suppresses moves that expose or leave the king in check.
     */
    fun getLegalMoves(side: PieceSide): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves(side)
        val legalMoves = mutableListOf<ChessMove>()

        for (m in pseudoMoves) {
            // Simulate the move on a cloned board
            val simGrid = copyGrid()
            val piece = simGrid[m.from.row][m.from.col]
            simGrid[m.to.row][m.to.col] = if (m.promotion != null) {
                ChessPiece(m.promotion, side)
            } else {
                piece
            }
            simGrid[m.from.row][m.from.col] = null

            // Check if our king is in check after the simulated move
            if (!isInCheck(side, simGrid)) {
                // If a check is delivered to opponent, tag it
                val isDeliveringCheck = isInCheck(side.opponent(), simGrid)
                legalMoves.add(m.copy(isCheck = isDeliveringCheck))
            }
        }
        return legalMoves
    }

    /**
     * Executes the move, adjusts game state parameters, and flips the active player.
     * Checks for Checkmate and Stalemate automatically.
     */
    fun makeMove(move: ChessMove): Boolean {
        if (isGameOver) return false

        val legalMoves = getLegalMoves(activeSide)
        val validMove = legalMoves.find {
            it.from == move.from && it.to == move.to && (move.promotion == null || it.promotion == move.promotion)
        } ?: return false

        // Execute
        val piece = grid[validMove.from.row][validMove.from.col] ?: return false
        grid[validMove.to.row][validMove.to.col] = if (validMove.promotion != null) {
            ChessPiece(validMove.promotion, activeSide)
        } else {
            piece
        }
        grid[validMove.from.row][validMove.from.col] = null

        moveHistory.add(validMove)
        activeSide = activeSide.opponent()

        // Check for Game Over conditions on next side
        val nextLegalMoves = getLegalMoves(activeSide)
        val inCheck = isInCheck(activeSide)

        if (nextLegalMoves.isEmpty()) {
            isGameOver = true
            if (inCheck) {
                // Checkmate! Opposing side won.
                winner = activeSide.opponent()
            } else {
                // Stalemate
                isDraw = true
                drawReason = "Stalemate"
            }
        } else {
            // Other simple draws: Threefold repetition or insufficient material could go here.
            // Let's implement active check of Fifty-move rule or Material limit:
            if (isInsufficientMaterial()) {
                isGameOver = true
                isDraw = true
                drawReason = "Insufficient Material"
            }
        }

        return true
    }

    /**
     * Drops the last move in history and restores previous board grid and active player.
     */
    fun undoMove(): Boolean {
        if (moveHistory.isEmpty()) return false

        // To undo cleanly, we just reset the board and replay all moves except the last one.
        val targetHistory = moveHistory.dropLast(1)
        reset()
        for (m in targetHistory) {
            // Find matched move in legal moves of corresponding turn
            val matched = getLegalMoves(activeSide).find {
                it.from == m.from && it.to == m.to && (m.promotion == null || it.promotion == m.promotion)
            }
            if (matched != null) {
                val p = grid[matched.from.row][matched.from.col]
                grid[matched.to.row][matched.to.col] = if (matched.promotion != null) {
                    ChessPiece(matched.promotion, activeSide)
                } else {
                    p
                }
                grid[matched.from.row][matched.from.col] = null
                moveHistory.add(matched)
                activeSide = activeSide.opponent()
            }
        }

        // Re-evaluate game over flags after undoing
        isGameOver = false
        winner = null
        isDraw = false
        drawReason = null

        return true
    }

    private fun isInsufficientMaterial(): Boolean {
        var whitePieces = 0
        var whiteKnight = false
        var whiteBishop = false
        var blackPieces = 0
        var blackKnight = false
        var blackBishop = false

        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c] ?: continue
                if (p.type == PieceType.KING) continue

                if (p.side == PieceSide.WHITE) {
                    whitePieces++
                    if (p.type == PieceType.KNIGHT) whiteKnight = true
                    if (p.type == PieceType.BISHOP) whiteBishop = true
                } else {
                    blackPieces++
                    if (p.type == PieceType.KNIGHT) blackKnight = true
                    if (p.type == PieceType.BISHOP) blackBishop = true
                }
            }
        }

        val totalOutsideKings = whitePieces + blackPieces
        if (totalOutsideKings == 0) return true // Only Kings left
        if (totalOutsideKings == 1) {
            // King + Knight vs King OR King + Bishop vs King
            if (whiteBishop || whiteKnight || blackBishop || blackKnight) return true
        }

        return false
    }

    /**
     * Converts board state into standard Forsyth-Edwards Notation (FEN) for consumption by Gemini API!
     */
    fun toFEN(): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var emptyCount = 0
            for (c in 0..7) {
                val piece = grid[r][c]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    val letter = when (piece.type) {
                        PieceType.PAWN -> 'p'
                        PieceType.KNIGHT -> 'n'
                        PieceType.BISHOP -> 'b'
                        PieceType.ROOK -> 'r'
                        PieceType.QUEEN -> 'q'
                        PieceType.KING -> 'k'
                    }
                    sb.append(if (piece.side == PieceSide.WHITE) letter.uppercaseChar() else letter)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (r < 7) sb.append('/')
        }

        // Active color
        sb.append(" ").append(if (activeSide == PieceSide.WHITE) "w" else "b")

        // Hardcode simple castling availability and en passant for compatibility
        sb.append(" - - 0 1")

        return sb.toString()
    }

    /**
     * Renders FEN grid into a highly readable text configuration
     */
    fun toReadableGrid(): String {
        val sb = StringBuilder()
        sb.append("Current Chess Board Grid:\n")
        sb.append("  a b c d e f g h\n")
        for (r in 0..7) {
            sb.append(8 - r).append(" ")
            for (c in 0..7) {
                val piece = grid[r][c]
                if (piece != null) {
                    sb.append(piece.getSymbol()).append(" ")
                } else {
                    sb.append(". ")
                }
            }
            sb.append(8 - r).append("\n")
        }
        sb.append("  a b c d e f g h\n")
        sb.append("Current Turn: ${activeSide.name}\n")
        return sb.toString()
    }
}
