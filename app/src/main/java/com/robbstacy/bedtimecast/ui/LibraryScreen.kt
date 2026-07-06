package com.robbstacy.bedtimecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.data.AppPrefs
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.Story
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.ui.theme.AppColors
import kotlin.random.Random

@Composable
fun LibraryScreen(nav: NavController) {
    val context = LocalContext.current
    val activeProfile = ProfilesStore.activeProfile
    val kidMode = AppPrefs.kidMode
    // Favorites first, library order otherwise. Reading AppPrefs.favorites
    // keeps this reactive to heart taps.
    val stories = StoryRepository.stories(context)
        .sortedByDescending { AppPrefs.isFavorite(it.id) }

    var showKidModeConfirm by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }
    var gateA by remember { mutableIntStateOf(0) }
    var gateB by remember { mutableIntStateOf(0) }
    var gateAnswer by remember { mutableStateOf("") }

    if (showKidModeConfirm) {
        AlertDialog(
            onDismissRequest = { showKidModeConfirm = false },
            title = { Text("Hand it to your little one? 🧸") },
            text = {
                Text(
                    "Kid mode locks recording, voices, and backups behind a grown-up " +
                        "question. Your child can only browse and play stories.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showKidModeConfirm = false
                    AppPrefs.updateKidMode(true)
                }) { Text("Turn on kid mode") }
            },
            dismissButton = {
                TextButton(onClick = { showKidModeConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showGate) {
        AlertDialog(
            onDismissRequest = { showGate = false },
            title = { Text("Grown-ups only 🔒") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What is $gateA + $gateB?")
                    OutlinedTextField(
                        value = gateAnswer,
                        onValueChange = { gateAnswer = it },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (gateAnswer.trim().toIntOrNull() == gateA + gateB) {
                        showGate = false
                        AppPrefs.updateKidMode(false)
                    } else {
                        gateAnswer = ""
                    }
                }) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { showGate = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("BedtimeCast") },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (kidMode) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable {
                            gateA = Random.nextInt(3, 10)
                            gateB = Random.nextInt(3, 10)
                            gateAnswer = ""
                            showGate = true
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            "🔒 Kid mode is on — grown-ups tap here",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { nav.navigate("profiles") },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = activeProfile?.let { "${it.emoji} Reading with ${it.name}" }
                                ?: "👋 Tap to add a voice — Mom, Dad, Grandma…",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = if (activeProfile != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                    if (activeProfile != null) {
                        Surface(
                            modifier = Modifier.clickable { nav.navigate("cast") },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                "🎭",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = 17.sp,
                            )
                        }
                        Surface(
                            modifier = Modifier.clickable { showKidModeConfirm = true },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                "🧸",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val continueStory = AppPrefs.lastStoryId?.let { StoryRepository.story(context, it) }
                if (continueStory != null && AppPrefs.resumePage(continueStory.id) > 0) {
                    item(key = "continue") {
                        val resumePage = AppPrefs.resumePage(continueStory.id)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                nav.navigate("story/${continueStory.id}?page=$resumePage")
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(
                                        "Continue ${continueStory.title} ${continueStory.coverEmoji}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Page ${resumePage + 1} of ${continueStory.pages.size}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                items(stories, key = { it.id }) { story ->
                    StoryCard(story, nav)
                }
            }
        }
    }
}

@Composable
private fun StoryCard(story: Story, nav: NavController) {
    val context = LocalContext.current
    val summary = Narration.summarizeResolved(context, story)
    val favorite = AppPrefs.isFavorite(story.id)
    val readerNames = summary.readerIds
        .mapNotNull { ProfilesStore.byId(it)?.name }
        .joinToString(" & ")

    val statusText: String
    val statusColor: androidx.compose.ui.graphics.Color
    when {
        summary.total > 0 && summary.recorded == summary.total && readerNames.isNotEmpty() -> {
            statusText = "❤️ Read by $readerNames"
            statusColor = AppColors.recorded
        }
        summary.total > 0 && summary.playable == summary.total && readerNames.isNotEmpty() -> {
            statusText = "❤️✨ Ready to play — $readerNames"
            statusColor = AppColors.gold
        }
        summary.playable > 0 -> {
            statusText = "${summary.recorded} of ${summary.total} lines recorded"
            statusColor = MaterialTheme.colorScheme.primary
        }
        else -> {
            statusText = "Not recorded yet"
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { nav.navigate("story/${story.id}") },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(coverColor(story), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(story.coverEmoji, fontSize = 34.sp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    story.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${story.author} · ${story.minutes} min",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
            }
            Text(
                if (favorite) "❤️" else "🤍",
                fontSize = 20.sp,
                modifier = Modifier.clickable { AppPrefs.toggleFavorite(story.id) },
            )
        }
    }
}
