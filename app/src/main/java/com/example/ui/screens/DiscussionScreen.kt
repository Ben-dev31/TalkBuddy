package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ChatMessage
import com.example.model.ConversationMode
import com.example.model.MessageSender
import com.example.ui.components.AudioPulseOrb
import com.example.ui.components.ExchangeTrackerBar
import com.example.ui.components.MessageBubble
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PrimaryIndigo
import com.example.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    // Scroll to bottom when message list updates
    LaunchedEffect(state.messages.size, state.isAiResponding) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("discussion_screen"),
        topBar = {
            DiscussionTopBar(
                buddyName = state.selectedBuddy.name,
                buddyCity = state.selectedBuddy.city,
                buddyFlag = state.selectedBuddy.flag,
                avatarRes = state.selectedBuddy.avatarRes,
                completedExchanges = state.completedExchanges,
                targetExchanges = state.targetExchanges,
                onSettingsClick = { viewModel.openSettings() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Selector: Text vs Voice
            ModeSelector(
                selectedMode = state.conversationMode,
                onModeSelected = { viewModel.setConversationMode(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Exchanges Progress Bar (replacing the timer constraint)
            ExchangeTrackerBar(
                completedExchanges = state.completedExchanges,
                targetExchanges = state.targetExchanges,
                isTargetReached = state.isTargetReached,
                onExtendTarget = { viewModel.extendTargetExchanges(5) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Dynamic Content based on Mode
            if (state.conversationMode == ConversationMode.TEXT) {
                TextModeContent(
                    state = state,
                    listState = listState,
                    onSend = { viewModel.sendUserMessage() },
                    onInputChanged = { viewModel.onInputTextChanged(it) },
                    onQuickSuggestionClicked = { viewModel.sendUserMessage(it) },
                    onToggleExpand = { viewModel.toggleMessageDetails(it) },
                    onSpeakMessage = { viewModel.speakMessage(it) },
                    onMicClick = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            viewModel.toggleVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                VoiceModeContent(
                    state = state,
                    onToggleListening = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            viewModel.toggleVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onReplayLastMessage = {
                        val lastBuddyMsg = state.messages.lastOrNull { it.sender == MessageSender.BUDDY }
                        if (lastBuddyMsg != null) {
                            viewModel.speakMessage(lastBuddyMsg)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DiscussionTopBar(
    buddyName: String,
    buddyCity: String,
    buddyFlag: String,
    avatarRes: Int,
    completedExchanges: Int,
    targetExchanges: Int,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box {
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = buddyName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, PrimaryIndigo.copy(alpha = 0.3f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MintEmerald)
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buddyName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = buddyFlag, fontSize = 16.sp)
                    }
                    Text(
                        text = "$buddyCity • En ligne pour discuter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryIndigo.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "💬 $completedExchanges/$targetExchanges",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryIndigo
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Paramètres de la discussion",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelector(
    selectedMode: ConversationMode,
    onModeSelected: (ConversationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Text Mode Option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onModeSelected(ConversationMode.TEXT) }
                    .testTag("mode_text"),
                color = if (selectedMode == ConversationMode.TEXT) PrimaryIndigo else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "💬 Discussion Texte",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedMode == ConversationMode.TEXT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Voice Mode Option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onModeSelected(ConversationMode.VOICE) }
                    .testTag("mode_voice"),
                color = if (selectedMode == ConversationMode.VOICE) PrimaryIndigo else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "🎙️ Discussion Vocale",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedMode == ConversationMode.VOICE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TextModeContent(
    state: com.example.viewmodel.ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSend: () -> Unit,
    onInputChanged: (String) -> Unit,
    onQuickSuggestionClicked: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onSpeakMessage: (ChatMessage) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Chat message history
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    isExpanded = state.expandedMessageId == msg.id,
                    onToggleExpand = { onToggleExpand(msg.id) },
                    onSpeakMessage = onSpeakMessage,
                    isSpeaking = state.isSpeaking && state.messages.lastOrNull()?.id == msg.id
                )
            }

            // Typing indicator when AI is generating reply
            if (state.isAiResponding) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryIndigo
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${state.selectedBuddy.name} est en train d'écrire...",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Suggestions Horizontal Carousel
        if (state.quickSuggestions.isNotEmpty() && !state.isAiResponding) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "💡 Idées de réponses pour t'inspirer :",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(state.quickSuggestions) { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                PrimaryIndigo.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clickable { onQuickSuggestionClicked(suggestion) }
                                .testTag("quick_suggestion_chip")
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input Bar (Text field, voice-to-text mic, send button)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speech-to-Text Dictation Button inside text field
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("dictation_mic_button")
                ) {
                    Icon(
                        imageVector = if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Dicter à la voix",
                        tint = if (state.isListening) AccentCoral else PrimaryIndigo
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = onInputChanged,
                    placeholder = {
                        Text(
                            text = if (state.isListening) "Écoute en cours..." else "Écris en anglais à ton ami...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = state.inputText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (state.inputText.isNotBlank()) PrimaryIndigo else PrimaryIndigo.copy(alpha = 0.4f))
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer le message",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceModeContent(
    state: com.example.viewmodel.ChatUiState,
    onToggleListening: () -> Unit,
    onReplayLastMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastBuddyMsg = state.messages.lastOrNull { it.sender == MessageSender.BUDDY }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Last Buddy Prompt Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_buddy_prompt_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dernière phrase de ${state.selectedBuddy.name}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryIndigo
                    )
                    IconButton(
                        onClick = onReplayLastMessage,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Réécouter",
                            tint = PrimaryIndigo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = lastBuddyMsg?.text ?: "Ready to chat!",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (lastBuddyMsg?.translationFr != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🇫🇷 ${lastBuddyMsg.translationFr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Central Visualizer / Orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AudioPulseOrb(
                    isSpeaking = state.isSpeaking,
                    isListening = state.isListening
                )
                Image(
                    painter = painterResource(id = state.selectedBuddy.avatarRes),
                    contentDescription = state.selectedBuddy.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val statusText = when {
                state.isSpeaking -> "🔊 ${state.selectedBuddy.name} parle en anglais..."
                state.isListening -> "🎙️ Écoute en cours... Parle en anglais !"
                state.isAiResponding -> "💭 ${state.selectedBuddy.name} réfléchit..."
                else -> "Appuie sur le micro ci-dessous pour répondre"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Transcription Preview if listening
            if (state.recognizedSpeechPreview != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentCoral.copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "🗣️ \"${state.recognizedSpeechPreview}\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = AccentCoral,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Big Push-to-Talk Microphone Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onToggleListening,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (state.isListening) AccentCoral else PrimaryIndigo)
                    .testTag("voice_mic_button")
            ) {
                Icon(
                    imageVector = if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Parler",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (state.isListening) "Tape pour valider l'envoi" else "Tape pour parler",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
