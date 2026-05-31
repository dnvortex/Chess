package com.example.chess.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chess_matches")
data class ChessMatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val opponentName: String, // e.g., "AI Level 1 (Easy)", "Gemini GM"
    val playerColor: String, // "White" or "Black"
    val gameResult: String, // "Won", "Lost", "Drawn"
    val totalMoves: Int,
    val dateString: String,
    val moveList: String // e.g. "e2e4-e7e5-g1f3"
)

@Dao
interface ChessMatchDao {
    @Query("SELECT * FROM chess_matches ORDER BY id DESC")
    fun getAllHistory(): Flow<List<ChessMatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(record: ChessMatchRecord)

    @Query("DELETE FROM chess_matches")
    suspend fun clearHistory()
}

@Database(entities = [ChessMatchRecord::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun chessMatchDao(): ChessMatchDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChessRepository(private val dao: ChessMatchDao) {
    val gameHistory: Flow<List<ChessMatchRecord>> = dao.getAllHistory()

    suspend fun insertMatch(record: ChessMatchRecord) {
        dao.insertMatch(record)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
