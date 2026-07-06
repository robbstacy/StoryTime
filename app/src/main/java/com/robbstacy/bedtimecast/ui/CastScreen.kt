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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.data.AppPrefs
import com.robbstacy.bedtimecast.data.CastMember
import com.robbstacy.bedtimecast.data.CloneEngine
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.data.VoiceModels
import com.robbstacy.bedtimecast.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun CastScreen(nav: NavController) {
    val context = LocalContext.current
    val profile = ProfilesStore.activeProfile
    val scope = rememberCoroutineScope()

    if (AppPrefs.kidMode) {
        Scaffold(topBar = { AppTopBar("Voice Cast", nav) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "🔒 The voice cast is for grown-ups. Ask one to unlock kid mode from the library.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        return
    }

    var busyText by remember { mutableStateOf<String?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var pendingCreate by remember { mutableStateOf<(() -> Unit)?>(null) }

    val cast = Narration.cast(context, profile?.id, StoryRepository.stories(context))
    val performed = cast.filter { it.recordedLines > 0 }
    val waiting = cast.filter { it.recordedLines == 0 }
    val apiKeySet = AppPrefs.elevenLabsKey.isNotBlank()

    // Runs a model-creation action, inserting the consent step the first time.
    fun requestCreate(action: () -> Unit) {
        val prof = profile ?: return
        if (VoiceModels.hasConsent(prof.id)) {
            action()
        } else {
            pendingCreate = action
        }
    }

    resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text(if (message.startsWith("✨")) "Voice model" else "Something went wrong") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { resultMessage = null }) { Text("OK") }
            },
        )
    }

    pendingCreate?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingCreate = null },
            title = { Text("Before your voice is modeled") },
            text = {
                Text(
                    "Creating a voice model uploads ${profile?.name}'s recorded samples to " +
                        "ElevenLabs, an AI voice service, using your API key. The model lets " +
                        "BedtimeCast read unrecorded lines in this voice, always labeled ✨.\n\n" +
                        "You can delete all voice models at any time from the Voices screen — " +
                        "that removes them from ElevenLabs too. Only continue if you are the " +
                        "owner of this voice, or have their permission.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profile?.let { VoiceModels.grantConsent(it.id) }
                    pendingCreate = null
                    action()
                }) { Text("I agree — create it") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCreate = null }) { Text("Cancel") }
            },
        )
    }

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
                        "a labeled voice sample. With an ElevenLabs key, samples become voice " +
                        "models that can read lines you never recorded, always labeled ✨.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (!apiKeySet) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().clickable { nav.navigate("profiles") },
                    ) {
                        Text(
                            "🔑 Add your ElevenLabs API key on the Voices screen to unlock voice models →",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            busyText?.let { busy ->
                item {
                    Text(
                        busy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.gold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // The narrator voice — the foundation model for this profile.
            item {
                val stats = Narration.narratorStats(context, profile.id, StoryRepository.stories(context))
                val narratorModel = VoiceModels.narrator(profile.id)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(56.dp).background(
                                AppColors.gold.copy(alpha = 0.2f),
                                CircleShape,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("📖", fontSize = 24.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${profile.name} — Narrator",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${stats.lines} narrator lines · ${stats.words} words sampled",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            when {
                                narratorModel != null -> Text(
                                    "✨ Voice model ready",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.gold,
                                )
                                apiKeySet && stats.words >= CloneEngine.NARRATOR_MIN_WORDS -> Text(
                                    "✨ Create voice model",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(enabled = busyText == null) {
                                        requestCreate {
                                            busyText = "Creating ${profile.name}'s narrator voice…"
                                            scope.launch {
                                                val result = CloneEngine.createNarratorModel(context, profile)
                                                busyText = null
                                                resultMessage = result.fold(
                                                    onSuccess = { "✨ ${it.displayName} is ready. Open a story and generate the missing lines!" },
                                                    onFailure = { it.message ?: "Could not create the voice model." },
                                                )
                                            }
                                        }
                                    },
                                )
                                else -> Text(
                                    "Record ${CloneEngine.NARRATOR_MIN_WORDS}+ narrator words to enable modeling",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("${profile.emoji} ${profile.name.uppercase()}'S CHARACTERS")
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
                CastCard(
                    member = member,
                    profileId = profile.id,
                    canCreate = apiKeySet && busyText == null,
                    onCreate = {
                        requestCreate {
                            busyText = "Creating ${profile.name}'s ${member.character.name} voice…"
                            scope.launch {
                                val result = CloneEngine.createCharacterModel(
                                    context, profile, member.story, member.character,
                                )
                                busyText = null
                                resultMessage = result.fold(
                                    onSuccess = { "✨ ${it.displayName} is ready. Cast it in any story from the 🎭 button." },
                                    onFailure = { it.message ?: "Could not create the voice model." },
                                )
                            }
                        }
                    },
                    nav = nav,
                )
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
                    "🔒 Samples stay on this device until you create a voice model, which uploads " +
                        "them to ElevenLabs with your consent. Delete all voice models any time " +
                        "from the Voices screen.",
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
private fun CastCard(
    member: CastMember,
    profileId: String,
    canCreate: Boolean,
    onCreate: () -> Unit,
    nav: NavController,
) {
    val color = characterColor(member.story, member.character.id)
    val complete = member.recordedLines == member.totalLines
    val model = VoiceModels.model(profileId, "${member.story.id}/${member.character.id}")

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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(member.character.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
                Text(
                    "${member.story.title} · ${member.character.voiceHint}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (complete) {
                        "Sample complete — ${member.recordedWords} words captured"
                    } else {
                        "${member.recordedLines} of ${member.totalLines} lines · record the rest to finish the sample"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (complete) AppColors.recorded else MaterialTheme.colorScheme.primary,
                )
                when {
                    model != null -> Text(
                        "✨ Voice model ready",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.gold,
                    )
                    canCreate && member.recordedWords >= CloneEngine.CHARACTER_MIN_WORDS -> Text(
                        "✨ Create voice model",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onCreate() },
                    )
                    else -> {}
                }
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
