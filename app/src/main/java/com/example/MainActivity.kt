package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DiscussionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TalkBuddyApp()
                }
            }
        }
    }
}

@Composable
fun TalkBuddyApp(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    // Handle back button when in Settings to return to Discussion
    BackHandler(enabled = state.showSettings) {
        viewModel.closeSettings()
    }

    AnimatedContent(
        targetState = state.showSettings,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { showSettings ->
        if (showSettings) {
            SettingsScreen(viewModel = viewModel)
        } else {
            DiscussionScreen(viewModel = viewModel)
        }
    }
}
