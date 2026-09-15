package com.example.model

import com.example.R
import java.util.UUID

enum class UserLevel(
    val title: String,
    val cefr: String,
    val description: String,
    val badge: String
) {
    BEGINNER(
        title = "Débutant",
        cefr = "A1-A2",
        description = "Mots simples, phrases courtes, explications douces et encouragement constant",
        badge = "🌱 Débutant"
    ),
    INTERMEDIATE(
        title = "Intermédiaire",
        cefr = "B1-B2",
        description = "Conversations fluides, argot naturel, expressions du quotidien et relances dynamiques",
        badge = "⚡ Intermédiaire"
    ),
    ADVANCED(
        title = "Avancé",
        cefr = "C1-C2",
        description = "Débats spontanés, expressions idiomatiques fines, humour et répartie native",
        badge = "🚀 Avancé"
    )
}

data class BuddyPersona(
    val id: String,
    val name: String,
    val country: String,
    val flag: String,
    val city: String,
    val accentName: String,
    val avatarRes: Int,
    val bio: String,
    val personality: String,
    val sampleExpressions: List<String>,
    val defaultGreeting: String
)

object BuddyCatalog {
    val buddies = listOf(
        BuddyPersona(
            id = "alex",
            name = "Alex Turner",
            country = "United Kingdom",
            flag = "🇬🇧",
            city = "London",
            accentName = "British Accent",
            avatarRes = R.drawable.avatar_alex,
            bio = "Fan de foot, de musique indie et de discussions autour d'un thé. Parle avec l'esprit et l'élégance britannique.",
            personality = "Warm, witty, supportive British chap. Uses British expressions like 'cheers', 'fancy', 'mate', 'brilliant'.",
            sampleExpressions = listOf("Cheers mate!", "Fancy a cuppa?", "Bloody brilliant!", "No worries!"),
            defaultGreeting = "Hey there! Brilliant to chat with you today! How are you doing? What's new with you?"
        ),
        BuddyPersona(
            id = "sam",
            name = "Sam Rivera",
            country = "United States",
            flag = "🇺🇸",
            city = "New York",
            accentName = "American Accent",
            avatarRes = R.drawable.avatar_sam,
            bio = "Toujours un café glacé à la main, passionné de tech, streetwear et sorties à Brooklyn.",
            personality = "Upbeat, energetic, friendly New Yorker. Uses American idioms like 'what's good', 'totally', 'hit me up', 'grab a bite'.",
            sampleExpressions = listOf("What's good?", "Hit me up!", "Grab a bite", "Totally cool!"),
            defaultGreeting = "Hey! What's good? Always awesome catching up. How has your day been so far?"
        ),
        BuddyPersona(
            id = "chloe",
            name = "Chloe Bennett",
            country = "Australia",
            flag = "🇦🇺",
            city = "Sydney",
            accentName = "Australian Accent",
            avatarRes = R.drawable.avatar_chloe,
            bio = "Amoureuse de la mer, du surf et des road trips. Très décontractée, toujours positive et chaleureuse.",
            personality = "Laid-back, sunny, adventurous Aussie. Uses Australian idioms like 'G'day', 'no dramas', 'arvo', 'heaps good'.",
            sampleExpressions = listOf("G'day mate!", "No dramas!", "Catch ya this arvo", "Too easy!"),
            defaultGreeting = "G'day mate! Great to connect. How's everything going on your side today?"
        )
    )

    fun getById(id: String): BuddyPersona {
        return buddies.firstOrNull { it.id == id } ?: buddies.first()
    }
}

data class DailyTopic(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val starterQuestion: String
)

object TopicCatalog {
    val topics = listOf(
        DailyTopic(
            id = "free",
            title = "Discussion 100% Libre",
            emoji = "💬",
            description = "Aucun sujet imposé : discutez spontanément de tout ce qui vous passe par la tête",
            starterQuestion = "How has your day been so far? Tell me anything that's on your mind!"
        ),
        DailyTopic(
            id = "daily",
            title = "Vibes du Jour & Routine",
            emoji = "☕",
            description = "Petits bonheurs, journée de travail ou moments sympas",
            starterQuestion = "Tell me one fun thing that happened to you today, or how your morning went!"
        ),
        DailyTopic(
            id = "travel",
            title = "Voyages & Aventures",
            emoji = "✈️",
            description = "Destinations de rêve, anecdotes de voyage et projets",
            starterQuestion = "If you could catch a direct flight anywhere in the world right now, where would you go?"
        ),
        DailyTopic(
            id = "cinema",
            title = "Cinéma, Séries & Culture",
            emoji = "🎬",
            description = "Séries du moment, films cultes et musique",
            starterQuestion = "Have you watched any good series or movies lately? What was the last thing you enjoyed?"
        ),
        DailyTopic(
            id = "food",
            title = "Nourriture & Cuisine",
            emoji = "🍕",
            description = "Plats préférés, spécialités et cafés",
            starterQuestion = "What is your absolute favorite comfort food or drink when you want to treat yourself?"
        ),
        DailyTopic(
            id = "weekend",
            title = "Week-end & Loisirs",
            emoji = "🎉",
            description = "Sorties, détente et projets personnels",
            starterQuestion = "Any fun plans lined up for this weekend, or are you just going to relax?"
        )
    )

    fun getById(id: String): DailyTopic {
        return topics.firstOrNull { it.id == id } ?: topics.first()
    }
}

enum class MessageSender {
    USER,
    BUDDY
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedCorrection: String? = null,
    val translationFr: String? = null,
    val isAudioPlaying: Boolean = false
)

enum class ConversationMode {
    TEXT,
    VOICE
}

data class UserSettings(
    val selectedBuddyId: String = "alex",
    val userLevel: UserLevel = UserLevel.INTERMEDIATE,
    val targetExchanges: Int = 10, // 5, 10, 15, 20
    val selectedTopicId: String = "free",
    val autoPlayVoice: Boolean = true,
    val voiceSpeechRate: Float = 1.0f
)

data class ChatStats(
    val totalMessages: Int = 0,
    val completedExchanges: Int = 0,
    val wordsSpoken: Int = 0,
    val tipsReceived: Int = 0
)
