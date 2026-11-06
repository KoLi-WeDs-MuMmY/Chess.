package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.chess.model.AiDifficulty
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.MatchOutcome
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.TimeControl
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "match_records")
data class MatchRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val aiDifficulty: String,
    val timeControl: String,
    val playerColor: String,
    val outcome: String,
    val terminationReason: String,
    val playerAccuracy: Float,
    val aiAccuracy: Float,
    val bestMoveCount: Int,
    val goodMoveCount: Int,
    val inaccuracyCount: Int,
    val mistakeCount: Int,
    val blunderCount: Int,
    val totalPlies: Int,
    val durationSeconds: Int,
    val finalFen: String,
    val movesJson: String
)

data class MatchRecord(
    val id: Long,
    val timestamp: Long,
    val aiDifficulty: AiDifficulty,
    val timeControl: TimeControl,
    val playerColor: PieceColor,
    val outcome: MatchOutcome,
    val terminationReason: String,
    val playerAccuracy: Float,
    val aiAccuracy: Float,
    val bestMoveCount: Int,
    val goodMoveCount: Int,
    val inaccuracyCount: Int,
    val mistakeCount: Int,
    val blunderCount: Int,
    val totalPlies: Int,
    val durationSeconds: Int,
    val finalFen: String,
    val analyzedMoves: List<AnalyzedMove>
) {
    val totalFullMoves: Int get() = (totalPlies + 1) / 2
}

data class DifficultyRecord(
    val difficulty: AiDifficulty,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

data class StreakStats(
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val winRatePercent: Float = 0f,
    val averageAccuracy: Float = 0f,
    val totalBlundersCommitted: Int = 0,
    val totalBestMovesFound: Int = 0,
    val byDifficulty: Map<AiDifficulty, DifficultyRecord> = AiDifficulty.entries.associateWith {
        DifficultyRecord(it)
    }
)

object MatchJsonSerializer {
    fun encodeMoves(moves: List<AnalyzedMove>): String {
        val array = JSONArray()
        for (m in moves) {
            val obj = JSONObject()
            obj.put("ply", m.ply)
            obj.put("moveNumber", m.moveNumber)
            obj.put("color", m.color.name)
            obj.put("san", m.san)
            obj.put("uciPlayed", m.uciPlayed)
            obj.put("uciBestMove", m.uciBestMove)
            obj.put("bestMoveSan", m.bestMoveSan)
            obj.put("evalBeforeWhiteCp", m.evalBeforeWhiteCp)
            obj.put("evalAfterWhiteCp", m.evalAfterWhiteCp)
            obj.put("cpLossForMover", m.cpLossForMover)
            obj.put("classification", m.classification.name)
            obj.put("fenBefore", m.fenBefore)
            obj.put("fenAfter", m.fenAfter)
            if (m.capturedPieceName != null) {
                obj.put("capturedPieceName", m.capturedPieceName)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeMoves(json: String): List<AnalyzedMove> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = ArrayList<AnalyzedMove>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AnalyzedMove(
                        ply = obj.optInt("ply", i + 1),
                        moveNumber = obj.optInt("moveNumber", (i / 2) + 1),
                        color = runCatching { PieceColor.valueOf(obj.optString("color", "WHITE")) }
                            .getOrDefault(PieceColor.WHITE),
                        san = obj.optString("san", ""),
                        uciPlayed = obj.optString("uciPlayed", ""),
                        uciBestMove = obj.optString("uciBestMove", ""),
                        bestMoveSan = obj.optString("bestMoveSan", ""),
                        evalBeforeWhiteCp = obj.optInt("evalBeforeWhiteCp", 0),
                        evalAfterWhiteCp = obj.optInt("evalAfterWhiteCp", 0),
                        cpLossForMover = obj.optInt("cpLossForMover", 0),
                        classification = runCatching {
                            MoveClassification.valueOf(obj.optString("classification", "GOOD"))
                        }.getOrDefault(MoveClassification.GOOD),
                        fenBefore = obj.optString("fenBefore", ""),
                        fenAfter = obj.optString("fenAfter", ""),
                        capturedPieceName = obj.optString("capturedPieceName", "").takeIf { it.isNotEmpty() }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun MatchRecordEntity.toDomain(): MatchRecord {
        return MatchRecord(
            id = id,
            timestamp = timestamp,
            aiDifficulty = runCatching { AiDifficulty.valueOf(aiDifficulty) }.getOrDefault(AiDifficulty.MEDIUM),
            timeControl = runCatching { TimeControl.valueOf(timeControl) }.getOrDefault(TimeControl.UNTIMED),
            playerColor = runCatching { PieceColor.valueOf(playerColor) }.getOrDefault(PieceColor.WHITE),
            outcome = runCatching { MatchOutcome.valueOf(outcome) }.getOrDefault(MatchOutcome.DRAW),
            terminationReason = terminationReason,
            playerAccuracy = playerAccuracy,
            aiAccuracy = aiAccuracy,
            bestMoveCount = bestMoveCount,
            goodMoveCount = goodMoveCount,
            inaccuracyCount = inaccuracyCount,
            mistakeCount = mistakeCount,
            blunderCount = blunderCount,
            totalPlies = totalPlies,
            durationSeconds = durationSeconds,
            finalFen = finalFen,
            analyzedMoves = decodeMoves(movesJson)
        )
    }
}
