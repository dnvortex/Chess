package com.example.chess.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.random.Random

object ChessAI {

    // Piece-Square Tables (PST) are defined from WHITE's perspective.
    // Row 0 is the opponent back rank (from WHITE's POV), Row 7 is WHITE's back rank.
    // For BLACK, we mirror these tables vertically.

    private val pawnPST = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),
        intArrayOf( 50,  50,  50,  50,  50,  50,  50,  50),
        intArrayOf( 10,  10,  20,  30,  30,  20,  10,  10),
        intArrayOf(  5,   5,  10,  25,  25,  10,   5,   5),
        intArrayOf(  0,   0,   0,  20,  20,   0,   0,   0),
        intArrayOf(  5,  -5, -10,   0,   0, -10,  -5,   5),
        intArrayOf(  5,  10,  10, -20, -20,  10,  10,   5),
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0)
    )

    private val knightPST = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20,   0,   0,   0,   0, -20, -40),
        intArrayOf(-30,   0,  10,  15,  15,  10,   0, -30),
        intArrayOf(-30,   5,  15,  20,  20,  15,   5, -30),
        intArrayOf(-30,   0,  15,  20,  20,  15,   0, -30),
        intArrayOf(-30,   5,  10,  15,  15,  10,   5, -30),
        intArrayOf(-40, -20,   0,   5,   5,   0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val bishopPST = arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10,   0,   0,   0,   0,   0,   0, -10),
        intArrayOf(-10,   0,   5,  10,  10,   5,   0, -10),
        intArrayOf(-10,   5,   5,  10,  10,   5,   5, -10),
        intArrayOf(-10,   0,  10,  10,  10,  10,   0, -10),
        intArrayOf(-10,  10,  10,  10,  10,  10,  10, -10),
        intArrayOf(-10,   5,   0,   0,   0,   0,   5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    private val rookPST = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),
        intArrayOf(  5,  10,  10,  10,  10,  10,  10,   5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf(  0,   0,   0,   5,   5,   0,   0,   0)
    )

    private val queenPST = arrayOf(
        intArrayOf(-20, -10, -10,  -5,  -5, -10, -10, -20),
        intArrayOf(-10,   0,   0,   0,   0,   0,   0, -10),
        intArrayOf(-10,   0,   5,   5,   5,   5,   0, -10),
        intArrayOf( -5,   0,   5,   5,   5,   5,   0,  -5),
        intArrayOf(  0,   0,   5,   5,   5,   5,   0,  -5),
        intArrayOf(-10,   5,   5,   5,   5,   5,   5, -10),
        intArrayOf(-10,   0,   5,   0,   0,   5,   0, -10),
        intArrayOf(-20, -10, -10,  -5,  -5, -10, -10, -20)
    )

    private val kingMiddleGamePST = arrayOf(
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
        intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
        intArrayOf( 20,  20,   0,   0,   0,   0,  20,  20),
        intArrayOf( 20,  30,  10,   0,   0,  10,  30,  20)
    )

    /**
     * Finds the best move for the active player.
     * Starts a background thread Minimax with alpha-beta pruning.
     *
     * @param board The chess board.
     * @param level 1 for Easy (depth 1 + random), 2 for Medium (depth 2), 3 for Hard (depth 3), 4 for Expert (depth 4)
     */
    fun getBestMove(board: ChessBoard, level: Int): ChessMove? {
        val side = board.activeSide
        val legalMoves = board.getLegalMoves(side)
        if (legalMoves.isEmpty()) return null

        if (level == 1) {
            // Easy Mode: Pick randomly among the top 3 moves to feel natural, but sometimes perfect.
            val scoredMoves = legalMoves.map { move ->
                val simulatedGrid = simulateMove(board, move, side)
                val score = evaluateBoard(simulatedGrid, side)
                Pair(move, score)
            }.sortedByDescending { it.second }

            // Take one of the top moves with a slight random deviation
            val range = min(3, scoredMoves.size)
            return scoredMoves[Random.nextInt(range)].first
        }

        // Adjust search depth based on level
        val searchDepth = when (level) {
            2 -> 2 // Medium
            3 -> 3 // Hard
            4 -> 4 // Expert
            else -> 2
        }

        var bestMove: ChessMove? = null
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        var beta = Int.MAX_VALUE

        // Shuffle moves briefly to add variation in case of equal evaluations
        val shuffledMoves = legalMoves.shuffled()

        for (move in shuffledMoves) {
            val simGrid = simulateMove(board, move, side)
            // Call minimax for other side (minimizing step)
            val score = minimax(simGrid, searchDepth - 1, alpha, beta, false, side.opponent(), side)

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, score)
            if (beta <= alpha) {
                break // beta cut-off
            }
        }

        // Fallback to random if bestMove didn't get set (which is extremely rare)
        return bestMove ?: legalMoves.randomOrNull()
    }

    private fun minimax(
        grid: Array<Array<ChessPiece?>>,
        depth: Int,
        initAlpha: Int,
        initBeta: Int,
        isMaximizing: Boolean,
        activeSide: PieceSide,
        evaluatingSide: PieceSide
    ): Int {
        if (depth == 0) {
            return evaluateBoard(grid, evaluatingSide)
        }

        var alpha = initAlpha
        var beta = initBeta

        val legalMoves = generatePseudoLegalMovesFiltered(grid, activeSide)
        if (legalMoves.isEmpty()) {
            // Checkmate or Stalemate
            val inCheck = isInCheck(activeSide, grid)
            return if (inCheck) {
                // Opponent delivered checkmate
                if (activeSide == evaluatingSide) -99999 - depth else 99999 + depth
            } else {
                0 // Draw / Stalemate
            }
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in legalMoves) {
                val nextGrid = simulateGridMove(grid, move, activeSide)
                val evaluation = minimax(nextGrid, depth - 1, alpha, beta, false, activeSide.opponent(), evaluatingSide)
                maxEval = max(maxEval, evaluation)
                alpha = max(alpha, evaluation)
                if (beta <= alpha) {
                    break
                }
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in legalMoves) {
                val nextGrid = simulateGridMove(grid, move, activeSide)
                val evaluation = minimax(nextGrid, depth - 1, alpha, beta, true, activeSide.opponent(), evaluatingSide)
                minEval = min(minEval, evaluation)
                beta = min(beta, evaluation)
                if (beta <= alpha) {
                    break
                }
            }
            return minEval
        }
    }

    /**
     * Evaluates the absolute score of a board grid from the perspective of evaluatingSide.
     * Score = (Sum(EvaluatingSide material + positional)) - (Sum(Opponent material + positional))
     */
    private fun evaluateBoard(grid: Array<Array<ChessPiece?>>, side: PieceSide): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = grid[r][c] ?: continue
                val isSelf = piece.side == side

                // Basic value
                val rawPieceVal = piece.type.value
                val posVal = getPositionalValue(piece.type, piece.side, r, c)

                val valSum = rawPieceVal + posVal
                if (isSelf) {
                    score += valSum
                } else {
                    score -= valSum
                }
            }
        }
        return score
    }

    private fun getPositionalValue(type: PieceType, side: PieceSide, row: Int, col: Int): Int {
        // Mirrored if Black
        val mappedRow = if (side == PieceSide.WHITE) row else 7 - row
        val mappedCol = col

        return try {
            when (type) {
                PieceType.PAWN -> pawnPST[mappedRow][mappedCol]
                PieceType.KNIGHT -> knightPST[mappedRow][mappedCol]
                PieceType.BISHOP -> bishopPST[mappedRow][mappedCol]
                PieceType.ROOK -> rookPST[mappedRow][mappedCol]
                PieceType.QUEEN -> queenPST[mappedRow][mappedCol]
                PieceType.KING -> kingMiddleGamePST[mappedRow][mappedCol]
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun simulateMove(board: ChessBoard, move: ChessMove, side: PieceSide): Array<Array<ChessPiece?>> {
        val nextGrid = board.copyGrid()
        val piece = nextGrid[move.from.row][move.from.col]
        nextGrid[move.to.row][move.to.col] = if (move.promotion != null) {
            ChessPiece(move.promotion, side)
        } else {
            piece
        }
        nextGrid[move.from.row][move.from.col] = null
        return nextGrid
    }

    private fun simulateGridMove(grid: Array<Array<ChessPiece?>>, move: ChessMove, side: PieceSide): Array<Array<ChessPiece?>> {
        val nextGrid = Array(8) { r -> Array(8) { c -> grid[r][c] } }
        val piece = nextGrid[move.from.row][move.from.col]
        nextGrid[move.to.row][move.to.col] = if (move.promotion != null) {
            ChessPiece(move.promotion, side)
        } else {
            piece
        }
        nextGrid[move.from.row][move.from.col] = null
        return nextGrid
    }

    private fun generatePseudoLegalMovesFiltered(grid: Array<Array<ChessPiece?>>, side: PieceSide): List<ChessMove> {
        // Similar to ChessBoard's legal move generation, but purely static for recursive minimax steps
        val pseudoMoves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = grid[r][c] ?: continue
                if (piece.side != side) continue
                val pos = Position(r, c)

                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(pos, side, grid, pseudoMoves)
                    PieceType.KNIGHT -> generateKnightMoves(pos, side, grid, pseudoMoves)
                    PieceType.BISHOP -> generateSlidingMoves(pos, side, grid, pseudoMoves, diagonalDirs)
                    PieceType.ROOK -> generateSlidingMoves(pos, side, grid, pseudoMoves, straightDirs)
                    PieceType.QUEEN -> generateSlidingMoves(pos, side, grid, pseudoMoves, allDirs)
                    PieceType.KING -> generateKingMoves(pos, side, grid, pseudoMoves)
                }
            }
        }

        // Filter out moves that leave our king in check
        return pseudoMoves.filter { m ->
            val sim = simulateGridMove(grid, m, side)
            !isInCheck(side, sim)
        }
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

        val forwardPos = Position(from.row + direction, from.col)
        if (forwardPos.isValid() && currentGrid[forwardPos.row][forwardPos.col] == null) {
            if (forwardPos.row == promoteRow) {
                moves.add(ChessMove(from, forwardPos, PieceType.QUEEN))
            } else {
                moves.add(ChessMove(from, forwardPos))
            }

            val doubleForwardPos = Position(from.row + 2 * direction, from.col)
            if (from.row == startRow && currentGrid[doubleForwardPos.row][doubleForwardPos.col] == null) {
                moves.add(ChessMove(from, doubleForwardPos))
            }
        }

        val captureCols = listOf(from.col - 1, from.col + 1)
        for (c in captureCols) {
            val capturePos = Position(from.row + direction, c)
            if (capturePos.isValid()) {
                val target = currentGrid[capturePos.row][capturePos.col]
                if (target != null && target.side != side) {
                    if (capturePos.row == promoteRow) {
                        moves.add(ChessMove(from, capturePos, PieceType.QUEEN, isCapture = true))
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
                    break
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

    private fun isInCheck(side: PieceSide, currentGrid: Array<Array<ChessPiece?>>): Boolean {
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
        if (kingPos == null) return false

        val opponentSide = side.opponent()
        // Compute pseudo-legal for opponent on standard rules
        val oppGrid = currentGrid
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = oppGrid[r][c] ?: continue
                if (piece.side != opponentSide) continue
                val pos = Position(r, c)

                // Simple check attack pattern
                val attacksKing = when (piece.type) {
                    PieceType.PAWN -> {
                        val dir = if (opponentSide == PieceSide.WHITE) -1 else 1
                        val diffRow = kingPos.row - pos.row
                        val diffCol = abs(kingPos.col - pos.col)
                        diffRow == dir && diffCol == 1
                    }
                    PieceType.KNIGHT -> {
                        val dR = abs(kingPos.row - pos.row)
                        val dC = abs(kingPos.col - pos.col)
                        (dR == 2 && dC == 1) || (dR == 1 && dC == 2)
                    }
                    PieceType.BISHOP -> checksSliding(pos, kingPos, oppGrid, diagonalDirs)
                    PieceType.ROOK -> checksSliding(pos, kingPos, oppGrid, straightDirs)
                    PieceType.QUEEN -> checksSliding(pos, kingPos, oppGrid, allDirs)
                    PieceType.KING -> {
                        val dR = abs(kingPos.row - pos.row)
                        val dC = abs(kingPos.col - pos.col)
                        dR <= 1 && dC <= 1
                    }
                }
                if (attacksKing) return true
            }
        }
        return false
    }

    private fun checksSliding(from: Position, to: Position, grid: Array<Array<ChessPiece?>>, directions: List<Pair<Int, Int>>): Boolean {
        for (dir in directions) {
            var step = 1
            while (true) {
                val curr = Position(from.row + dir.first * step, from.col + dir.second * step)
                if (!curr.isValid()) break
                if (curr == to) return true
                if (grid[curr.row][curr.col] != null) break // blocked by another piece
                step++
            }
        }
        return false
    }
}
