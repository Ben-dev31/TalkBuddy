package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.model.BuddyPersona
import com.example.model.ChatMessage
import com.example.model.DailyTopic
import com.example.model.MessageSender
import com.example.model.UserLevel
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class AiBuddyResponse(
    @Json(name = "reply") val reply: String,
    @Json(name = "friendlyTip") val friendlyTip: String? = null,
    @Json(name = "translationFr") val translationFr: String? = null
)

class GeminiChatService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateBuddyReply(
        buddy: BuddyPersona,
        userLevel: UserLevel,
        topic: DailyTopic,
        recentMessages: List<ChatMessage>,
        userMessage: String
    ): AiBuddyResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiChatService", "No valid Gemini API key found. Using smart native fallback engine.")
            return@withContext getOfflineSmartReply(buddy, userLevel, topic, userMessage, recentMessages.size)
        }

        try {
            val systemInstruction = buildSystemPrompt(buddy, userLevel, topic)
            
            // Build request JSON manually or via Moshi
            val contentsJson = StringBuilder()
            contentsJson.append("[")
            
            // Add last 4 turns for context
            val turns = recentMessages.takeLast(6)
            for ((index, msg) in turns.withIndex()) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val escaped = escapeJson(msg.text)
                contentsJson.append("{\"role\":\"$role\",\"parts\":[{\"text\":\"$escaped\"}]}")
                if (index < turns.size - 1 || userMessage.isNotBlank()) {
                    contentsJson.append(",")
                }
            }
            if (userMessage.isNotBlank()) {
                val escapedUser = escapeJson(userMessage)
                contentsJson.append("{\"role\":\"user\",\"parts\":[{\"text\":\"$escapedUser\"}]}")
            }
            contentsJson.append("]")

            val escapedSystemPrompt = escapeJson(systemInstruction)

            val requestBodyString = """
            {
              "systemInstruction": {
                "parts": [{"text": "$escapedSystemPrompt"}]
              },
              "contents": $contentsJson,
              "generationConfig": {
                "temperature": 0.85,
                "topP": 0.95,
                "responseMimeType": "application/json"
              }
            }
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBodyString.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("GeminiChatService", "API error: ${response.code} body: $responseBody")
                return@withContext getOfflineSmartReply(buddy, userLevel, topic, userMessage, recentMessages.size)
            }

            val parsedResponse = moshi.adapter(GeminiResponse::class.java).fromJson(responseBody)
            val rawText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (rawText != null) {
                val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                try {
                    val buddyResponse = moshi.adapter(AiBuddyResponse::class.java).fromJson(cleanJson)
                    if (buddyResponse != null && buddyResponse.reply.isNotBlank()) {
                        return@withContext buddyResponse
                    }
                } catch (jsonErr: Exception) {
                    Log.e("GeminiChatService", "Failed to parse structured JSON: $rawText", jsonErr)
                    // If not valid JSON, use rawText directly
                    return@withContext AiBuddyResponse(
                        reply = cleanJson,
                        friendlyTip = null,
                        translationFr = null
                    )
                }
            }

            getOfflineSmartReply(buddy, userLevel, topic, userMessage, recentMessages.size)
        } catch (e: Exception) {
            Log.e("GeminiChatService", "Exception calling Gemini API", e)
            getOfflineSmartReply(buddy, userLevel, topic, userMessage, recentMessages.size)
        }
    }

    private fun buildSystemPrompt(buddy: BuddyPersona, userLevel: UserLevel, topic: DailyTopic): String {
        val levelInstruction = when (userLevel) {
            UserLevel.BEGINNER -> "The user is an absolute beginner (CEFR A1-A2). Use simple everyday vocabulary, short sentences (1-2 sentences), clear grammar, and very warm, encouraging words. Speak slowly and naturally."
            UserLevel.INTERMEDIATE -> "The user is at intermediate level (CEFR B1-B2). Talk naturally like peers in their 20s/30s. Use common conversational idioms and modern everyday phrasing. Keep it 2 to 3 sentences."
            UserLevel.ADVANCED -> "The user is advanced (CEFR C1-C2). Converse at a full native speed with rich vocabulary, authentic colloquialisms, humor, witty banter, and cultural references."
        }

        val topicContext = if (topic.id == "free") {
            "Start the conversation completely naturally like close friends catching up. Do NOT restrict the user to any specific topic."
        } else {
            "Initial inspiration (optional mood): '${topic.title}'. However, do NOT be rigid! If the user branches into any other topic, follow them naturally."
        }

        return """
        You are ${buddy.name} from ${buddy.city}, ${buddy.country}.
        Your personality: ${buddy.personality}.
        Accent/Style: ${buddy.accentName}. Use occasional signature expressions naturally (e.g. ${buddy.sampleExpressions.joinToString(", ")}).
        $topicContext

        ROLE AND BEHAVIOR:
        1. You are a warm, curious, fun, close friend having a casual chat (like WhatsApp or over coffee), NOT a teacher, tutor or rigid bot. Never lecture, test, or give grades.
        2. BE TOTALLY OPEN AND NATURAL: Follow the user wherever they take the conversation! If they talk about their day, work, relationships, food, jokes, hobbies, or whatever is on their mind, chat with them enthusiastically about that. Do not force them into a pre-set curriculum.
        3. Always keep the conversation rolling! React warmly with empathy, banter, or personal anecdotes, and ALWAYS end your turn with a natural, easy-to-answer follow-up question.
        4. $levelInstruction
        5. Provide response strictly as JSON with the following fields:
           - "reply": Your spoken answer in English (concise, 2-3 sentences max).
           - "friendlyTip": An optional short friendly tip (in French or English) pointing out a cool native way to say something or cheering them on, or null.
           - "translationFr": A simple French translation of your reply so the user can check comprehension if they tap.
        """.trimIndent()
    }

    private fun escapeJson(string: String): String {
        return string.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Highly responsive, context-aware native buddy dialogue engine for offline testing or when API key is missing.
     */
    fun getOfflineSmartReply(
        buddy: BuddyPersona,
        userLevel: UserLevel,
        topic: DailyTopic,
        userMessage: String,
        historyCount: Int
    ): AiBuddyResponse {
        val cleanMsg = userMessage.lowercase().trim()

        val reply: String
        val tip: String?
        val translation: String

        when {
            cleanMsg.contains("hello") || cleanMsg.contains("hi") || cleanMsg.contains("hey") -> {
                when (buddy.id) {
                    "alex" -> {
                        reply = "Hey mate! Brilliant to hear from you. I was just having a quick cuppa. What have you been up to today?"
                        tip = "Astuce native : 'cuppa' est l'argot londonien très populaire pour une tasse de thé !"
                        translation = "Salut l'ami ! Super d'avoir de tes nouvelles. Je prenais justement une tasse de thé. Qu'as-tu fait de beau aujourd'hui ?"
                    }
                    "sam" -> {
                        reply = "Hey! What's good? Always great catching up with you. Did you have a busy morning or are you taking it easy?"
                        tip = "Astuce native : 'What's good?' est une façon chaleureuse et moderne à New York de demander des nouvelles."
                        translation = "Salut ! Quoi de neuf ? Toujours un plaisir d'échanger avec toi. Tu as eu une matinée chargée ou tu te reposes ?"
                    }
                    else -> {
                        reply = "G'day! Awesome to see you around today. How's the weather on your side of the world?"
                        tip = "Astuce native : 'G'day' est le salut incontournable en Australie, ultra chaleureux !"
                        translation = "Salut ! Génial de te voir aujourd'hui. Quel temps fait-il de ton côté du monde ?"
                    }
                }
            }

            cleanMsg.contains("tired") || cleanMsg.contains("fatigue") || cleanMsg.contains("sleep") || cleanMsg.contains("hard") -> {
                reply = "Oh I totally feel you! Some days just drain all your energy, right? Did work or school keep you running all day?"
                tip = "Astuce native : Pour dire que vous êtes épuisé(e), un locuteur natif dira souvent 'I'm beat' ou 'I'm wiped out' !"
                translation = "Oh je te comprends tellement ! Certaines journées vident toute l'énergie, pas vrai ? Le travail ou les cours t'ont occupé toute la journée ?"
            }

            cleanMsg.contains("coffee") || cleanMsg.contains("tea") || cleanMsg.contains("drink") || cleanMsg.contains("eat") || cleanMsg.contains("food") -> {
                reply = "Haha you're speaking my language now! I can't even function in the morning without my usual caffeine fix. How do you take your coffee or tea?"
                tip = "Astuce native : 'Caffeine fix' désigne cette dose de café indispensable pour démarrer la journée !"
                translation = "Haha là tu me parles ! Je ne peux même pas fonctionner le matin sans ma dose de caféine. Tu prends ton café ou thé comment ?"
            }

            cleanMsg.contains("travel") || cleanMsg.contains("vacation") || cleanMsg.contains("trip") || cleanMsg.contains("flight") || topic.id == "travel" -> {
                reply = "Traveling is honestly the best thing ever. Exploring new streets, food, and culture! If someone handed you a free plane ticket right now, where would you fly?"
                tip = "Astuce native : 'Bucket list' désigne la liste des choses ou voyages dont on rêve absolument avant de mourir."
                translation = "Voyager est honnêtement la meilleure chose au monde. Découvrir de nouvelles rues, de la nourriture et de la culture ! Si quelqu'un t'offrait un billet d'avion là tout de suite, où irais-tu ?"
            }

            cleanMsg.contains("movie") || cleanMsg.contains("series") || cleanMsg.contains("netflix") || cleanMsg.contains("film") || topic.id == "cinema" -> {
                reply = "Oh nice! I'm always looking for solid recommendations to binge-watch. Do you prefer gripping thrillers or comedy to unwind?"
                tip = "Astuce native : 'Binge-watch' signifie enchaîner les épisodes d'une série sans s'arrêter !"
                translation = "Oh sympa ! Je cherche toujours de bonnes recommandations à dévorer. Tu préfères les thrillers captivants ou les comédies pour décompresser ?"
            }

            cleanMsg.contains("weekend") || cleanMsg.contains("sunday") || cleanMsg.contains("saturday") || topic.id == "weekend" -> {
                reply = "Nothing beats that weekend feeling! Are you more of an outdoor adventure person or a chill movie on the couch person?"
                tip = "Astuce native : 'Chill' est indispensable en anglais pour désigner la détente complète sans prise de tête."
                translation = "Rien ne vaut cette sensation du week-end ! Tu es plutôt aventures en plein air ou posé sur le canapé devant un film ?"
            }

            else -> {
                // Adaptive friendly open conversation response
                val reactions = listOf(
                    "Haha, that makes total sense! That's actually really interesting.",
                    "Oh really? That's so cool, love your take on that!",
                    "100% with you on that! Tell me more.",
                    "No way! That sounds quite eventful, how did you feel about it?"
                )
                val followUps = listOf(
                    "What would you say has been the highlight of your week so far?",
                    "If you had a totally free afternoon tomorrow, what would you spend it doing?",
                    "Have you picked up any fun hobbies or listened to any good tracks recently?",
                    "How do you usually like to unwind after a long day?"
                )
                val chosenReaction = reactions[historyCount % reactions.size]
                val chosenFollowUp = followUps[(historyCount + 1) % followUps.size]

                reply = "$chosenReaction $chosenFollowUp"
                tip = "Bravo pour ta spontanéité ! Échanger régulièrement comme avec un ami est la clé pour parler avec aisance."
                translation = "Haha, ça se comprend tout à fait ! C'est vraiment très intéressant. Au fait, quelle a été la meilleure partie de ta semaine jusqu'ici ?"
            }
        }

        return AiBuddyResponse(
            reply = reply,
            friendlyTip = tip,
            translationFr = translation
        )
    }
}
