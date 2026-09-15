package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiChatService
import com.example.audio.VoiceManager
import com.example.model.BuddyCatalog
import com.example.model.BuddyPersona
import com.example.model.ChatMessage
import com.example.model.ChatStats
import com.example.model.ConversationMode
import com.example.model.DailyTopic
import com.example.model.MessageSender
import com.example.model.TopicCatalog
import com.example.model.UserLevel
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val selectedBuddy: BuddyPersona = BuddyCatalog.buddies.first(),
    val userLevel: UserLevel = UserLevel.INTERMEDIATE,
    val selectedTopic: DailyTopic = TopicCatalog.topics.first(),
    val conversationMode: ConversationMode = ConversationMode.TEXT,
    val messages: List<ChatMessage> = emptyList(),
    val isAiResponding: Boolean = false,
    val inputText: String = "",
    val completedExchanges: Int = 0,
    val targetExchanges: Int = 10,
    val isTargetReached: Boolean = false,
    val quickSuggestions: List<String> = emptyList(),
    val showSettings: Boolean = false,
    val expandedMessageId: String? = null,
    val stats: ChatStats = ChatStats(),
    val autoPlayVoice: Boolean = true,
    val voiceSpeechRate: Float = 1.0f,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val recognizedSpeechPreview: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiChatService()
    val voiceManager = VoiceManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe voice manager speaking and listening state
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }
        viewModelScope.launch {
            voiceManager.isListening.collect { listening ->
                _uiState.update { it.copy(isListening = listening) }
            }
        }
        viewModelScope.launch {
            voiceManager.recognizedText.collect { text ->
                if (!text.isNullOrBlank()) {
                    _uiState.update { it.copy(recognizedSpeechPreview = text, inputText = text) }
                }
            }
        }

        // Initialize first natural conversation
        initConversation(
            buddy = BuddyCatalog.buddies.first(),
            topic = TopicCatalog.topics.first(),
            level = UserLevel.INTERMEDIATE
        )
    }

    fun initConversation(buddy: BuddyPersona, topic: DailyTopic, level: UserLevel) {
        voiceManager.setBuddyLocale(buddy.country)

        // The conversation starts naturally like two friends catching up
        val greetingText = if (topic.id == "free") {
            buddy.defaultGreeting
        } else {
            "${buddy.defaultGreeting} By the way, ${topic.starterQuestion}"
        }

        val translationGreeting = "Salut ! Super de discuter avec toi. Comment se passe ta journée ?"

        val initialMsg = ChatMessage(
            sender = MessageSender.BUDDY,
            text = greetingText,
            suggestedCorrection = null,
            translationFr = translationGreeting
        )

        val initialSuggestions = getSuggestionsForTopic(topic)

        _uiState.update {
            it.copy(
                selectedBuddy = buddy,
                selectedTopic = topic,
                userLevel = level,
                messages = listOf(initialMsg),
                completedExchanges = 0,
                isTargetReached = false,
                quickSuggestions = initialSuggestions,
                isAiResponding = false,
                inputText = ""
            )
        }

        // Speak greeting if in voice mode or auto-play
        if (_uiState.value.autoPlayVoice || _uiState.value.conversationMode == ConversationMode.VOICE) {
            voiceManager.speak(greetingText, _uiState.value.voiceSpeechRate)
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendUserMessage(text: String? = null) {
        val messageToSend = text ?: _uiState.value.inputText.trim()
        if (messageToSend.isBlank()) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = messageToSend
        )

        // Count words
        val wordCount = messageToSend.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

        _uiState.update { current ->
            val updatedMessages = current.messages + userMsg
            val newStats = current.stats.copy(
                totalMessages = current.stats.totalMessages + 1,
                wordsSpoken = current.stats.wordsSpoken + wordCount
            )
            current.copy(
                messages = updatedMessages,
                inputText = "",
                recognizedSpeechPreview = null,
                isAiResponding = true,
                stats = newStats
            )
        }

        // Call Gemini for native buddy reply
        viewModelScope.launch {
            val state = _uiState.value
            val response = geminiService.generateBuddyReply(
                buddy = state.selectedBuddy,
                userLevel = state.userLevel,
                topic = state.selectedTopic,
                recentMessages = state.messages,
                userMessage = messageToSend
            )

            val buddyMsg = ChatMessage(
                sender = MessageSender.BUDDY,
                text = response.reply,
                suggestedCorrection = response.friendlyTip,
                translationFr = response.translationFr
            )

            val nextSuggestions = generateNextSuggestions(response.reply, state.selectedTopic)

            _uiState.update { current ->
                val updatedMessages = current.messages + buddyMsg
                val newCompletedExchanges = current.completedExchanges + 1
                val targetReached = newCompletedExchanges >= current.targetExchanges
                val updatedTipsCount = if (response.friendlyTip != null) current.stats.tipsReceived + 1 else current.stats.tipsReceived
                current.copy(
                    messages = updatedMessages,
                    completedExchanges = newCompletedExchanges,
                    isTargetReached = targetReached,
                    isAiResponding = false,
                    quickSuggestions = nextSuggestions,
                    stats = current.stats.copy(
                        completedExchanges = newCompletedExchanges,
                        tipsReceived = updatedTipsCount
                    )
                )
            }

            // Audio reply handling
            if (state.autoPlayVoice || state.conversationMode == ConversationMode.VOICE) {
                voiceManager.speak(response.reply, state.voiceSpeechRate)
            }
        }
    }

    fun extendTargetExchanges(extra: Int = 5) {
        _uiState.update {
            it.copy(
                targetExchanges = it.targetExchanges + extra,
                isTargetReached = false
            )
        }
    }

    fun setConversationMode(mode: ConversationMode) {
        voiceManager.stopSpeaking()
        voiceManager.stopListening()
        _uiState.update { it.copy(conversationMode = mode) }
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isListening) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening { speechText ->
                _uiState.update { it.copy(inputText = speechText) }
                // In Voice mode, automatically send after recognizing speech
                if (_uiState.value.conversationMode == ConversationMode.VOICE) {
                    sendUserMessage(speechText)
                }
            }
        }
    }

    fun speakMessage(message: ChatMessage) {
        voiceManager.speak(message.text, _uiState.value.voiceSpeechRate)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun toggleMessageDetails(messageId: String) {
        _uiState.update {
            val nextExpanded = if (it.expandedMessageId == messageId) null else messageId
            it.copy(expandedMessageId = nextExpanded)
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun applySettings(settings: UserSettings) {
        val buddy = BuddyCatalog.getById(settings.selectedBuddyId)
        val topic = TopicCatalog.getById(settings.selectedTopicId)

        val buddyChanged = buddy.id != _uiState.value.selectedBuddy.id
        val topicChanged = topic.id != _uiState.value.selectedTopic.id
        val levelChanged = settings.userLevel != _uiState.value.userLevel

        _uiState.update {
            it.copy(
                selectedBuddy = buddy,
                selectedTopic = topic,
                userLevel = settings.userLevel,
                targetExchanges = settings.targetExchanges,
                isTargetReached = it.completedExchanges >= settings.targetExchanges,
                autoPlayVoice = settings.autoPlayVoice,
                voiceSpeechRate = settings.voiceSpeechRate,
                showSettings = false
            )
        }

        voiceManager.setBuddyLocale(buddy.country)

        // If buddy, topic or level changed, start a fresh conversation!
        if (buddyChanged || topicChanged || levelChanged) {
            initConversation(buddy, topic, settings.userLevel)
        }
    }

    fun resetChat() {
        initConversation(
            buddy = _uiState.value.selectedBuddy,
            topic = _uiState.value.selectedTopic,
            level = _uiState.value.userLevel
        )
    }

    private fun getSuggestionsForTopic(topic: DailyTopic): List<String> {
        return when (topic.id) {
            "free" -> listOf(
                "I had a pretty busy day, what about you?",
                "Not much honestly, just relaxing! How are things with you?",
                "I've got so much on my mind today haha!"
            )
            "travel" -> listOf(
                "I'd love to visit Japan or Australia!",
                "Somewhere sunny by the ocean for sure!",
                "Honestly, I'd go anywhere with good food!"
            )
            "cinema" -> listOf(
                "I watched a really great show recently!",
                "I haven't watched anything good in ages haha.",
                "What kind of movies do you like best?"
            )
            "food" -> listOf(
                "Definitely homemade pasta or pizza!",
                "I'm a big fan of street food and spicy dishes.",
                "Anything sweet, especially chocolate!"
            )
            else -> listOf(
                "Pretty good, thanks for asking! How about you?",
                "It's been quite a long day, but good overall!",
                "I'm super happy to be chatting with you today!"
            )
        }
    }

    private fun generateNextSuggestions(aiReply: String, topic: DailyTopic): List<String> {
        val lower = aiReply.lowercase()
        return when {
            lower.contains("coffee") || lower.contains("tea") -> listOf(
                "I can't start my day without coffee!",
                "I'm definitely more of a tea lover.",
                "Actually, I prefer fresh juice or smoothies!"
            )
            lower.contains("food") || lower.contains("eat") || lower.contains("cook") -> listOf(
                "That sounds so delicious!",
                "I'm not the best cook, but I try!",
                "What's your favorite meal to make?"
            )
            lower.contains("weekend") || lower.contains("plans") -> listOf(
                "Just taking it easy and catching up on sleep.",
                "Going out with some good friends!",
                "I might do a bit of sport or walk outside."
            )
            else -> listOf(
                "Haha totally with you on that!",
                "That's so true! What do you think about it?",
                "Tell me more about what you mean!"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
    }
}
