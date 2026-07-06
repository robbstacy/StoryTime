package com.robbstacy.bedtimecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.Story
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.ui.theme.AppColors

@Composable
fun LibraryScreen(nav: NavController) {
    val context = LocalContext.current
    val stories = StoryRepository.stories(context)
    val activeProfile = ProfilesStore.activeProfile

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("BedtimeCast") },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                }
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
    val profile = ProfilesStore.activeProfile
    val summary = Narration.summarize(context, profile?.id, story)

    val statusText: String
    val statusColor: androidx.compose.ui.graphics.Color
    when {
        summary.complete && profile != null -> {
            statusText = "❤️ Read by ${profile.name}"
            statusColor = AppColors.recorded
        }
        !summary.none -> {
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        }
    }
}
