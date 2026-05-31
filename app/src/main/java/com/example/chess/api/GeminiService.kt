package com.example.chess.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseSchemaText(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "schema") val schema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "text") val text: ResponseSchemaText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseSchema") val responseSchema: Map<String, Any>? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

// Moshi Representation for the Gemini Chess Board Move Choice
@JsonClass(generateAdapter = true)
data class GeminiChessMove(
    @Json(name = "move") val move: String, // e.g. "e7e5"
    @Json(name = "thought") val thought: String, // strategic reasoning
    @Json(name = "commentary") val commentary: String // conversational comment
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val moshiParser: Moshi get() = moshi
}

object GeminiChessOpponent {

    suspend fun getMoveFromGemini(
        fen: String,
        readableBoard: String,
        sideToPlay: String,
        legalMoves: List<String>,
        aiPersonality: String = "Grandmaster Magnus"
    ): GeminiChessMove? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return GeminiChessMove(
                move = legalMoves.randomOrNull() ?: "",
                thought = "Gemini API Key is missing. Using local random fallback.",
                commentary = "I'm playing offline right now because your Gemini API Key is not set in the Secrets workspace!"
            )
        }

        // Construct a strong strategic prompt instructing Gemini to act as a Chess Grandmaster
        val prompt = """
            As $aiPersonality, you are playing a match against a human opponent.
            You are playing side: $sideToPlay.
            
            Current board FEN: $fen
            
            $readableBoard
            
            Here is the EXACT list of all current legal moves available to you (in algebraic from-to coordinates):
            ${legalMoves.joinToString(", ")}
            
            Your Objective:
            1. Select exactly ONE move from the legal moves list: [${legalMoves.joinToString(", ")}].
            2. Share your internal strategic reasoning (thought) for making this move.
            3. Write a fun, context-aware chess quote or comment (commentary) addressing the user. Match your personality: $aiPersonality.
            
            You MUST respond in JSON following this direct format:
            {
               "move": "EXACT_SELECTED_MOVE_STRING_FROM_LEGAL_LIST",
               "thought": "Your grandmaster strategy reasoning...",
               "commentary": "Your witty, strategic or encouraging trash-talk/comment..."
            }
        """.trimIndent()

        // Specify strict JSON Schema to ensure a valid formatted response
        val responseSchema = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "move" to mapOf("type" to "STRING", "description" to "The chosen algebraic move. Perfect match to one of the available legal moves."),
                "thought" to mapOf("type" to "STRING", "description" to "Strategic reasoning behind the move choice."),
                "commentary" to mapOf("type" to "STRING", "description" to "A conversational, highly charismatic comment or reaction written in character.")
            ),
            "required" to listOf("move", "thought", "commentary")
        )

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = responseSchema,
                temperature = 0.7
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are a professional, charismatic chess grandmaster AI who speaks delightfully and analyzes boards strategically.")))
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = RetrofitClient.moshiParser.adapter(GeminiChessMove::class.java)
                val geminiChoice = adapter.fromJson(jsonText)
                if (geminiChoice != null && legalMoves.contains(geminiChoice.move)) {
                    geminiChoice
                } else if (geminiChoice != null) {
                    // Gemini picked a move but it wasn't in the legal moves list. Fall back to first legal/random, but keep Gemini's speech!
                    GeminiChessMove(
                        move = legalMoves.find { it.equals(geminiChoice.move, ignoreCase = true) } ?: legalMoves.randomOrNull() ?: "",
                        thought = "Gemini selected move '${geminiChoice.move}', which was slightly out-of-sync. Adjusted to legal move.",
                        commentary = geminiChoice.commentary
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
