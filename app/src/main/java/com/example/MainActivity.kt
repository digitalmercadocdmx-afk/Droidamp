package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.ui.screens.DroidampPlayerDeck
import com.example.ui.screens.EqualizerDeckScreen
import com.example.ui.screens.RadioDeckScreen
import com.example.ui.screens.SongUploadScreen
import com.example.ui.theme.DroidampTheme
import com.example.ui.viewmodel.DroidampViewModel

enum class DeckScreen(val title: String) {
    PLAYER("Player Deck"),
    SONGS("Upload / Tracks"),
    EQUALIZER("10-Band EQ"),
    RADIO("Public Radio")
}

@UnstableApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: DroidampViewModel = viewModel()
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

            // Request Notification permission for Foreground Service on Android 13+
            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted or denied */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Notification permission launch ignored", e)
                    }
                }
            }

            DroidampTheme(skin = userSettings.skin) {
                var currentScreen by remember { mutableStateOf(DeckScreen.PLAYER) }

                BackHandler(enabled = currentScreen != DeckScreen.PLAYER) {
                    currentScreen = DeckScreen.PLAYER
                }

                val isPlaying by viewModel.playerController.isPlaying.collectAsStateWithLifecycle()
                val trackTitle by viewModel.playerController.trackTitle.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.navigationBars,
                    bottomBar = {
                        Column {
                            // Mini Player Bar if not on Player Deck
                            if (currentScreen != DeckScreen.PLAYER) {
                                MiniPlayerBar(
                                    trackTitle = trackTitle,
                                    isPlaying = isPlaying,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onOpenPlayer = { currentScreen = DeckScreen.PLAYER }
                                )
                            }

                            // Cyberdeck Navigation Bar
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == DeckScreen.PLAYER,
                                    onClick = { currentScreen = DeckScreen.PLAYER },
                                    icon = {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = "Deck",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "PLAYER",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (currentScreen == DeckScreen.PLAYER) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_player_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == DeckScreen.SONGS,
                                    onClick = { currentScreen = DeckScreen.SONGS },
                                    icon = {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = "Upload Songs",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "SONGS",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (currentScreen == DeckScreen.SONGS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_songs_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == DeckScreen.EQUALIZER,
                                    onClick = { currentScreen = DeckScreen.EQUALIZER },
                                    icon = {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = "Equalizer",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "10-EQ",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (currentScreen == DeckScreen.EQUALIZER) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_equalizer_tab")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == DeckScreen.RADIO,
                                    onClick = { currentScreen = DeckScreen.RADIO },
                                    icon = {
                                        Icon(
                                            Icons.Default.Radio,
                                            contentDescription = "Radio",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            "RADIO",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (currentScreen == DeckScreen.RADIO) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_radio_tab")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            DeckScreen.PLAYER -> DroidampPlayerDeck(viewModel = viewModel)
                            DeckScreen.SONGS -> SongUploadScreen(
                                viewModel = viewModel,
                                onNavigateToEqualizer = { currentScreen = DeckScreen.EQUALIZER }
                            )
                            DeckScreen.EQUALIZER -> EqualizerDeckScreen(viewModel = viewModel)
                            DeckScreen.RADIO -> RadioDeckScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    trackTitle: String,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            .clickable { onOpenPlayer() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary else Color.Red)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "TensorRT Audio Engine Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("mini_player_toggle")
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
