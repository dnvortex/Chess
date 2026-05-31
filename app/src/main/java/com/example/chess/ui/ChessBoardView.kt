package com.example.chess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.db.ChessMatchRecord
import com.example.chess.engine.ChessPiece
import com.example.chess.engine.PieceSide
import com.example.chess.engine.PieceType
import com.example.chess.engine.Position
import java.util.Locale

// Beautiful color palettes optimized for the premium, dark Chess App
object ChessTheme {
    val LightSquare = Color(0xFFEADDFF) // Warm lavender light square
    val DarkSquare = Color(0xFF4F378B)  // Elegant classic dark violet square
    val HighlightSelected = Color(0x70D0BCFF) // Translucent theme color glow
    val HighlightLegalDot = Color(0xAA81C784) // Glowing light gold-green target
    val HighlightLegalCapture = Color(0xBBFF8A80) // Soft alert-red for capture opportunities
    val HighlightLastMove = Color(0x35D0BCFF) // Calmer translucent theme purple
    
    // Core color variables for the Immersive design theme
    val DarkBackground = Color(0xFF1C1B1F)  // Primary body background
    val SurfaceDark = Color(0xFF313033)     // Chess arena board outer padding container
    val FooterDark = Color(0xFF2B2930)      // Sleek top-rounded bottom panel
    val AccentColor = Color(0xFFD0BCFF)     // Glowing violet key active states
    val OnAccentColor = Color(0xFF381E72)   // High-contrast deep purple text
    val BodyText = Color(0xFFE6E1E5)        // Light primary text
    val SubtitleText = Color(0xFFCAC4D0)    // Muted grey text
}

// Utility to calculate real-time evaluation balance based on piece material value weights
fun getEvaluationBalance(board: com.example.chess.engine.ChessBoard): Float {
    var valSum = 0f
    for (r in 0..7) {
        for (c in 0..7) {
            val p = board.grid[r][c] ?: continue
            val sign = if (p.side == PieceSide.WHITE) 1f else -1f
            val weight = when (p.type) {
                PieceType.PAWN -> 1.0f
                PieceType.KNIGHT -> 3.0f
                PieceType.BISHOP -> 3.1f
                PieceType.ROOK -> 5.0f
                PieceType.QUEEN -> 9.0f
                PieceType.KING -> 0.0f
            }
            valSum += sign * weight
        }
    }
    return valSum
}

@Composable
fun ChessGameScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.gameHistory.collectAsState()
    var showHistoryPanel by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ChessTheme.DarkBackground)
            .padding(16.dp)
    ) {
        val isWide = maxWidth > 680.dp

        if (isWide) {
            // Adaptive Side-by-Side layout for tablets/foldables
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImmersiveHeader(viewModel)
                    EvaluationBar(viewModel)
                    ChessBoardArena(viewModel)
                    ChessCapturedPiecesRow(viewModel, PieceSide.BLACK)
                    ChessCapturedPiecesRow(viewModel, PieceSide.WHITE)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GeminiCommentaryPanel(viewModel)
                    
                    if (showHistoryPanel) {
                        GameHistoryPanel(
                            records = history,
                            onClear = { viewModel.clearDbHistory() },
                            onClose = { showHistoryPanel = false }
                        )
                    } else {
                        QuickStatsAndHelpPanel(
                            onViewHistory = { showHistoryPanel = true }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    ImmersiveFooter(viewModel)
                }
            }
        } else {
            // Mobile Stack Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ImmersiveHeader(viewModel)
                }

                item {
                    EvaluationBar(viewModel)
                }

                item {
                    ChessBoardArena(viewModel)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ChessCapturedPiecesRow(viewModel, PieceSide.BLACK)
                            ChessCapturedPiecesRow(viewModel, PieceSide.WHITE)
                        }
                    }
                }

                item {
                    GeminiCommentaryPanel(viewModel)
                }

                item {
                    ImmersiveFooter(viewModel)
                }

                item {
                    if (showHistoryPanel) {
                        GameHistoryPanel(
                            records = history,
                            onClear = { viewModel.clearDbHistory() },
                            onClose = { showHistoryPanel = false }
                        )
                    } else {
                        QuickStatsAndHelpPanel(
                            onViewHistory = { showHistoryPanel = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImmersiveHeader(viewModel: ChessViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val opponentName = when (viewModel.aiLevel) {
            1 -> "Local Engine (Easy)"
            2 -> "Local Engine (Medium)"
            3 -> "Local Engine (Hard)"
            4 -> "Grandmaster Gemini"
            else -> "Stockfish v16"
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChessTheme.DarkSquare)
                    .border(1.dp, ChessTheme.AccentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Bot Avatar",
                    tint = ChessTheme.AccentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = opponentName,
                    color = ChessTheme.BodyText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = viewModel.currentTurnText,
                    color = ChessTheme.AccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Active state context (Online / Offline status indicator)
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(ChessTheme.SurfaceDark, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (viewModel.isThinking) "ANALYZING" else "IDLE",
                    color = if (viewModel.isThinking) ChessTheme.AccentColor else Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = if (viewModel.aiLevel == 4) "GEMINI ENGINE" else "LOCAL OFFLINE",
                color = ChessTheme.SubtitleText.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EvaluationBar(viewModel: ChessViewModel) {
    val score = getEvaluationBalance(viewModel.chessBoard)
    // Convert score to a proportional balance slider
    val mid = 15f
    val percentage = ((score + mid).coerceIn(0f, 2 * mid) / (2 * mid))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ChessTheme.SurfaceDark)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(percentage.coerceAtLeast(0.01f))
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight((1f - percentage).coerceAtLeast(0.01f))
                        .background(ChessTheme.DarkBackground)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val formattedScore = if (score > 0) "+${String.format(Locale.US, "%.1f", score)}" else String.format(Locale.US, "%.1f", score)
        Text(
            text = formattedScore,
            color = ChessTheme.AccentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ChessCapturedPiecesRow(viewModel: ChessViewModel, side: PieceSide) {
    val originalPieces = when (side) {
        PieceSide.WHITE -> listOf(
            PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN,
            PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN,
            PieceType.KNIGHT, PieceType.KNIGHT, PieceType.BISHOP, PieceType.BISHOP,
            PieceType.ROOK, PieceType.ROOK, PieceType.QUEEN
        )
        PieceSide.BLACK -> listOf(
            PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN,
            PieceType.PAWN, PieceType.PAWN, PieceType.PAWN, PieceType.PAWN,
            PieceType.KNIGHT, PieceType.KNIGHT, PieceType.BISHOP, PieceType.BISHOP,
            PieceType.ROOK, PieceType.ROOK, PieceType.QUEEN
        )
    }

    val currentOnBoard = mutableListOf<PieceType>()
    for (r in 0..7) {
        for (c in 0..7) {
            val p = viewModel.chessBoard.grid[r][c]
            if (p != null && p.side == side) {
                currentOnBoard.add(p.type)
            }
        }
    }

    val capturedList = mutableListOf<PieceType>()
    val tempOnBoard = currentOnBoard.toMutableList()
    for (pType in originalPieces) {
        if (tempOnBoard.contains(pType)) {
            tempOnBoard.remove(pType)
        } else {
            capturedList.add(pType)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (side == PieceSide.WHITE) "Captured White Pieces:" else "Captured Black Pieces:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = ChessTheme.SubtitleText.copy(alpha = 0.7f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (capturedList.isEmpty()) {
                Text(
                    text = "None",
                    fontSize = 11.sp,
                    color = ChessTheme.SubtitleText.copy(alpha = 0.4f)
                )
            } else {
                capturedList.sortedBy { it.value }.forEach { pType ->
                    val piece = ChessPiece(pType, side)
                    Text(
                        text = piece.getSymbol(),
                        fontSize = 15.sp,
                        color = if (side == PieceSide.WHITE) Color.White else Color(0xFF1C1B1F),
                        modifier = Modifier
                            .background(
                                color = if (side == PieceSide.WHITE) Color(0xFF475569) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .size(22.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ChessBoardArena(viewModel: ChessViewModel) {
    val board = viewModel.chessBoard
    val isFlipped = viewModel.isFlipped
    val inCheckSide = board.isInCheck(board.activeSide)
    
    val infiniteTransition = rememberInfiniteTransition(label = "CheckPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(ChessTheme.SurfaceDark, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                val r = if (isFlipped) displayRow else 7 - displayRow

                Row(modifier = Modifier.weight(1f)) {
                    for (displayCol in 0..7) {
                        val c = if (isFlipped) 7 - displayCol else displayCol
                        val pos = Position(r, c)
                        val piece = board.getPiece(pos)

                        val isDarkSquare = (r + c) % 2 == 1
                        val baseColor = if (isDarkSquare) ChessTheme.DarkSquare else ChessTheme.LightSquare

                        val isSelected = viewModel.selectedSquare == pos
                        val isLegalTarget = viewModel.legalTargets.contains(pos)
                        val isLastMoveSrc = viewModel.lastMove?.from == pos
                        val isLastMoveDst = viewModel.lastMove?.to == pos
                        val isKingInCheckSquare = inCheckSide && piece?.type == PieceType.KING && piece.side == board.activeSide

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(baseColor)
                                .clickable { viewModel.selectSquare(pos) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isSelected -> {
                                    Box(modifier = Modifier.fillMaxSize().background(ChessTheme.HighlightSelected))
                                }
                                isLastMoveSrc || isLastMoveDst -> {
                                    Box(modifier = Modifier.fillMaxSize().background(ChessTheme.HighlightLastMove))
                                }
                            }

                            if (isKingInCheckSquare) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Red.copy(alpha = pulseAlpha))
                                )
                            }

                            if (piece != null) {
                                Text(
                                    text = piece.getSymbol(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (piece.side == PieceSide.WHITE) Color.White else Color(0xFF1C1B1F),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.alpha(if (viewModel.isThinking && piece.side == board.activeSide) 0.5f else 1.0f)
                                )
                            }

                            if (isLegalTarget) {
                                val containsOpponent = piece != null
                                val indicatorColor = if (containsOpponent) ChessTheme.HighlightLegalCapture else ChessTheme.HighlightLegalDot
                                
                                Box(
                                    modifier = Modifier
                                        .size(if (containsOpponent) 32.dp else 12.dp)
                                        .border(
                                            width = if (containsOpponent) 3.dp else 0.dp,
                                            color = if (containsOpponent) indicatorColor else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .background(
                                            color = if (containsOpponent) Color.Transparent else indicatorColor,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiCommentaryPanel(viewModel: ChessViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ChessTheme.SurfaceDark.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ChessTheme.DarkSquare.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiObjects,
                        contentDescription = "Idea",
                        tint = ChessTheme.AccentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Commentary & Wit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChessTheme.AccentColor
                    )
                }
                
                if (viewModel.isThinking) {
                    Text(
                        text = "Thinking...",
                        color = ChessTheme.AccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.isThinking) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = ChessTheme.AccentColor,
                    trackColor = ChessTheme.SurfaceDark
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = "\"${viewModel.geminiCommentary}\"",
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                lineHeight = 18.sp,
                color = ChessTheme.BodyText
            )

            if (viewModel.geminiThought.isNotEmpty() && viewModel.aiLevel == 4) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = ChessTheme.BodyText.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Internal Strategy Formulation:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChessTheme.AccentColor
                )
                Text(
                    text = viewModel.geminiThought,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = ChessTheme.BodyText.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ImmersiveFooter(viewModel: ChessViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = ChessTheme.FooterDark
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Difficulty Selector Title & Slider representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val difficultyLabel = when (viewModel.aiLevel) {
                    1 -> "LVL 1: Easy"
                    2 -> "LVL 2: Medium"
                    3 -> "LVL 3: Hard"
                    4 -> "LVL 4: Gemini GM"
                    else -> "LVL 2: Medium"
                }
                Text(
                    text = "Engine Difficulty",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChessTheme.BodyText
                )
                Text(
                    text = difficultyLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChessTheme.AccentColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Level Selection Row (MD3 Slider representation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ChessTheme.SurfaceDark)
            ) {
                val fraction = when (viewModel.aiLevel) {
                    1 -> 0.25f
                    2 -> 0.50f
                    3 -> 0.75f
                    4 -> 1.00f
                    else -> 0.5f
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(fraction)
                        .background(ChessTheme.AccentColor)
                )
                if (fraction < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f - fraction)
                            .background(Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Micro-pills for switching difficulty level on click
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair(1, "EASY"),
                    Pair(2, "MED"),
                    Pair(3, "HARD"),
                    Pair(4, "GEMINI GM")
                ).forEach { (lvl, name) ->
                    val isSelected = viewModel.aiLevel == lvl
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ChessTheme.AccentColor else ChessTheme.SurfaceDark)
                            .clickable {
                                viewModel.aiLevel = lvl
                                if (lvl == 4) {
                                    viewModel.geminiCommentary = "You have invoked the Gemini Grandmaster! Prepare your mind for strategic analysis and banter."
                                } else {
                                    viewModel.geminiCommentary = "Switched to Local AI Level $lvl. Play offline instantly!"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ChessTheme.OnAccentColor else ChessTheme.BodyText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer main actions (Undo, Reset, Swap Sides)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo
                Button(
                    onClick = { viewModel.undoMove() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChessTheme.DarkSquare,
                        contentColor = ChessTheme.AccentColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("undo_move_button")
                ) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("UNDO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Reset
                Button(
                    onClick = { viewModel.resetGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChessTheme.AccentColor,
                        contentColor = ChessTheme.OnAccentColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("reset_game_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESET", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Swap
                IconButton(
                    onClick = { viewModel.swapSides() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ChessTheme.SurfaceDark, RoundedCornerShape(12.dp))
                        .testTag("swap_sides_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Sides",
                        tint = ChessTheme.AccentColor
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatsAndHelpPanel(onViewHistory: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ChessTheme.SurfaceDark.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, ChessTheme.DarkSquare.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chess Arena Insights",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ChessTheme.BodyText
                )

                Button(
                    onClick = onViewHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChessTheme.AccentColor.copy(alpha = 0.12f),
                        contentColor = ChessTheme.AccentColor
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("view_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Match History", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Divider(color = ChessTheme.BodyText.copy(alpha = 0.1f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BulletPoint("Center Domination: Fight to establish pawns on d4/e4/d5/e5 slots to maximize mobile bishop avenues.")
                BulletPoint("Early Kingside Castle: Safeguard your King away in a protected shelter to release both rooks.")
                BulletPoint("Avoid Pre-Moves: Deep scan all newly generated check paths before triggering rapid moves.")
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ChessTheme.AccentColor)
        Text(text = text, fontSize = 11.sp, lineHeight = 14.sp, color = ChessTheme.SubtitleText)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameHistoryPanel(
    records: List<ChessMatchRecord>,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ChessTheme.SurfaceDark.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ChessTheme.AccentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Local Matches",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ChessTheme.BodyText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = Color(0xFFEF9A9A)
                        )
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChessTheme.SurfaceDark,
                            contentColor = ChessTheme.BodyText
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Close", fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No games recorded yet!\nFinish matches against AI to save them.",
                        fontSize = 11.sp,
                        color = ChessTheme.SubtitleText.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(records) { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ChessTheme.FooterDark
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "vs ${r.opponentName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ChessTheme.BodyText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${r.playerColor} • ${r.totalMoves} moves • ${r.dateString}",
                                        fontSize = 9.sp,
                                        color = ChessTheme.SubtitleText.copy(alpha = 0.7f)
                                    )
                                }

                                Text(
                                    text = r.gameResult,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (r.gameResult) {
                                        "Won" -> Color(0xFF81C784)
                                        "Lost" -> Color(0xFFE57373)
                                        else -> Color(0xFFFFB74D)
                                    },
                                    modifier = Modifier
                                        .background(
                                            when (r.gameResult) {
                                                "Won" -> Color(0x2281C784)
                                                "Lost" -> Color(0x22E57373)
                                                else -> Color(0x22FFB74D)
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
