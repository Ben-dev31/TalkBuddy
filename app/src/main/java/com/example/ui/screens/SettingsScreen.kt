package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuddyCatalog
import com.example.model.BuddyPersona
import com.example.model.TopicCatalog
import com.example.model.UserLevel
import com.example.model.UserSettings
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryIndigo
import com.example.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var selectedBuddyId by remember(state.selectedBuddy.id) { mutableStateOf(state.selectedBuddy.id) }
    var selectedLevel by remember(state.userLevel) { mutableStateOf(state.userLevel) }
    var selectedTopicId by remember(state.selectedTopic.id) { mutableStateOf(state.selectedTopic.id) }
    var targetExchanges by remember(state.targetExchanges) { mutableIntStateOf(state.targetExchanges) }
    var autoPlayVoice by remember(state.autoPlayVoice) { mutableStateOf(state.autoPlayVoice) }
    var voiceSpeed by remember(state.voiceSpeechRate) { mutableFloatStateOf(state.voiceSpeechRate) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paramètres & Personnalisation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeSettings() },
                        modifier = Modifier.testTag("back_to_discussion")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour à la discussion"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. User Level Selection
            SectionHeader(
                icon = Icons.Default.School,
                title = "1. Votre niveau d'anglais",
                subtitle = "L'ami IA adapte son vocabulaire et sa complexité à vous"
            )

            UserLevel.values().forEach { level ->
                val isSelected = selectedLevel == level
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedLevel = level }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("level_card_${level.name.lowercase()}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = level.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = level.cefr,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = level.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Sélectionné",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 2. Native Speaker Persona Selection
            SectionHeader(
                icon = Icons.Default.Person,
                title = "2. Choisissez votre ami natif",
                subtitle = "Chaque ami a son accent, son histoire et son style décontracté"
            )

            BuddyCatalog.buddies.forEach { buddy ->
                val isSelected = selectedBuddyId == buddy.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedBuddyId = buddy.id }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("buddy_card_${buddy.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = buddy.avatarRes),
                                contentDescription = buddy.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (isSelected) PrimaryIndigo else Color.Transparent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${buddy.name} ${buddy.flag}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${buddy.city}, ${buddy.country} • ${buddy.accentName}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sélectionné",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = buddy.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ex : \"${buddy.sampleExpressions.first()}\"",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryIndigo
                                )
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryIndigo.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.voiceManager.setBuddyLocale(buddy.country)
                                        viewModel.voiceManager.speak(buddy.defaultGreeting, voiceSpeed)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Écouter la voix",
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Voix",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryIndigo
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Number of Exchanges Target (Replacing the text timer)
            SectionHeader(
                icon = Icons.Default.Forum,
                title = "3. Objectif d'échanges par session",
                subtitle = "Remplace le chrono par un nombre d'échanges à votre rythme"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            5 to "5 (Rapide ⚡)",
                            10 to "10 (Recommandé 🎯)",
                            15 to "15 (Approfondi 💬)",
                            20 to "20 (Immersion 🚀)"
                        ).forEach { (count, label) ->
                            val isChosen = targetExchanges == count
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { targetExchanges = count }
                                    .testTag("exchange_option_$count"),
                                color = if (isChosen) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Conseil",
                            tint = AccentCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pas de pression de temps ! Chaque aller-retour avec votre ami compte comme 1 échange.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Conversation Starting Mood (Not restricting the AI)
            SectionHeader(
                icon = Icons.Default.AutoAwesome,
                title = "4. Démarrage de la discussion",
                subtitle = "La discussion démarre naturellement comme avec un vrai ami"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopicCatalog.topics.forEach { topic ->
                    val isSelected = selectedTopicId == topic.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTopicId = topic.id }
                            .testTag("topic_chip_${topic.id}")
                    ) {
                        Text(
                            text = "${topic.emoji} ${topic.title}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 5. Audio & Voice Settings
            SectionHeader(
                icon = Icons.Default.Speed,
                title = "5. Voix & Prononciation",
                subtitle = "Personnalisez la lecture audio et la vitesse"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lecture vocale automatique",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "L'ami lit sa réponse à voix haute automatiquement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoPlayVoice,
                            onCheckedChange = { autoPlayVoice = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryIndigo),
                            modifier = Modifier.testTag("auto_play_voice_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Vitesse d'élocution :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.8f to "Lent (0.8x)", 1.0f to "Normal (1.0x)", 1.2f to "Rapide (1.2x)").forEach { (speed, label) ->
                            val isSpeedSelected = voiceSpeed == speed
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { voiceSpeed = speed },
                                color = if (isSpeedSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSpeedSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 6. User Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 Vos progrès du jour",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            number = "${state.completedExchanges}",
                            label = "Échanges"
                        )
                        StatItem(
                            number = "${state.stats.wordsSpoken}",
                            label = "Mots dits"
                        )
                        StatItem(
                            number = "${state.stats.tipsReceived}",
                            label = "Astuces reçues"
                        )
                    }
                }
            }

            // Save and Apply Button
            Button(
                onClick = {
                    val settings = UserSettings(
                        selectedBuddyId = selectedBuddyId,
                        userLevel = selectedLevel,
                        targetExchanges = targetExchanges,
                        selectedTopicId = selectedTopicId,
                        autoPlayVoice = autoPlayVoice,
                        voiceSpeechRate = voiceSpeed
                    )
                    viewModel.applySettings(settings)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(
                    text = "Enregistrer et continuer",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }

            // Reset conversation button
            OutlinedButton(
                onClick = {
                    val settings = UserSettings(
                        selectedBuddyId = selectedBuddyId,
                        userLevel = selectedLevel,
                        targetExchanges = targetExchanges,
                        selectedTopicId = selectedTopicId,
                        autoPlayVoice = autoPlayVoice,
                        voiceSpeechRate = voiceSpeed
                    )
                    viewModel.applySettings(settings)
                    viewModel.resetChat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_chat_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Réinitialiser",
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nouvelle discussion spontanée",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = PrimaryIndigo)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = PrimaryIndigo
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
