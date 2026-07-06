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
import androidx.compose.foundation.shape.CircleShape
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
import com.robbstacy.bedtimecast.data.CastMember
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.ui.theme.AppColors

@Composable
fun CastScreen(nav: NavController) {
    val context = LocalContext.current
    val profile = ProfilesStore.activeProfile
    val cast = Narration.cast(context, profile?.id, StoryRepository.stories(context))
    val performed = cast.filter { it.recordedLines > 0 }
    val waiting = cast.filter { it.recordedLines == 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("Voice Cast", nav) },
    ) { padding ->
        if (profile == null) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Add a voice profile first — then every character you perform builds your voice cast.",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Every character line ${profile.name} performs is saved with its exact words — " +
                        "building a labeled voice sample for that character. Once a character has " +
                        "enough lines, its voice can be modeled and cast in future stories. 🎭",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            item {
                SectionTitle("${profile.emoji} ${profile.name.uppercase()}'S CAST")
            }

            if (performed.isEmpty()) {
                item {
                    Text(
                        "No characters performed yet. Open a story, tap record, and give the Wolf " +
                            "your best growl — it will show up here.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(performed, key = { "${it.story.id}-${it.character.id}" }) { member ->
                CastCard(member, nav)
            }

            if (waiting.isNotEmpty()) {
                item {
                    SectionTitle(
                        "ROLES WAITING TO BE PERFORMED",
                        Modifier.padding(top = 16.dp),
                    )
                }
                items(waiting, key = { "w-${it.story.id}-${it.character.id}" }) { member ->
                    WaitingRow(member, nav)
                }
            }

            item {
                Text(
                    "🔒 Voice samples stay on this device. If character voice modeling is added " +
                        "later, it will only ever happen with the voice owner's permission, and any " +
                        "voice model can be deleted at any time.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CastCard(member: CastMember, nav: NavController) {
    val color = characterColor(member.story, member.character.id)
    val complete = member.recordedLines == member.totalLines

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { nav.navigate("record/${member.story.id}") },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(member.character.emoji, fontSize = 24.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(member.character.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
                Text(
                    "${member.story.title} · ${member.character.voiceHint}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (complete) {
                        "✨ Voice sample complete — ${member.recordedWords} words captured"
                    } else {
                        "${member.recordedLines} of ${member.totalLines} lines · record the rest to finish the sample"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (complete) AppColors.recorded else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WaitingRow(member: CastMember, nav: NavController) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { nav.navigate("record/${member.story.id}") },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(member.character.emoji, fontSize = 22.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    member.character.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${member.story.title} · ${member.totalLines} " +
                        if (member.totalLines == 1) "line" else "lines",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Record ›",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
