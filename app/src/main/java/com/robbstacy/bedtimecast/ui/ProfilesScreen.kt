package com.robbstacy.bedtimecast.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.data.AppPrefs
import com.robbstacy.bedtimecast.data.Backup
import com.robbstacy.bedtimecast.data.CloneEngine
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.VoiceModels
import com.robbstacy.bedtimecast.data.VoiceProfile
import com.robbstacy.bedtimecast.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EMOJI_CHOICES = listOf("👩", "👨", "👵", "👴", "🧑", "🦸")

@Composable
fun ProfilesScreen(nav: NavController) {
    if (AppPrefs.kidMode) {
        Scaffold(topBar = { AppTopBar("Voices", nav) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "🔒 Voice settings are for grown-ups. Ask one to unlock kid mode from the library.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(EMOJI_CHOICES[0]) }
    var removeTarget by remember { mutableStateOf<VoiceProfile?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    var apiKeyDraft by remember { mutableStateOf(AppPrefs.elevenLabsKey) }
    var showDeleteModels by remember { mutableStateOf(false) }

    if (showDeleteModels) {
        AlertDialog(
            onDismissRequest = { showDeleteModels = false },
            title = { Text("Delete all voice models?") },
            text = {
                Text(
                    "This removes every voice model from ElevenLabs and deletes all " +
                        "AI-generated narration from this phone. Your real recordings are " +
                        "not touched.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteModels = false
                    backupBusy = true
                    scope.launch {
                        val result = CloneEngine.deleteAllModels(context)
                        backupBusy = false
                        backupMessage = result.fold(
                            onSuccess = { "Deleted $it voice models ✅" },
                            onFailure = { "Delete failed: ${it.message}" },
                        )
                    }
                }) { Text("Delete them", color = AppColors.record) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModels = false }) { Text("Cancel") }
            },
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            backupBusy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { Backup.export(context, uri) }
                backupBusy = false
                backupMessage = result.fold(
                    onSuccess = { "Backup saved ✅\n${it.profiles} voices, ${it.recordings} recordings." },
                    onFailure = { "Backup failed: ${it.message}" },
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            backupBusy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { Backup.import(context, uri) }
                backupBusy = false
                backupMessage = result.fold(
                    onSuccess = { "Backup restored ✅\n${it.profiles} voices, ${it.recordings} recordings." },
                    onFailure = { "Restore failed: ${it.message}" },
                )
            }
        }
    }

    backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupMessage = null },
            title = { Text(if (message.contains("✅")) "Done" else "Something went wrong") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { backupMessage = null }) { Text("OK") }
            },
        )
    }

    removeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove ${target.name}?") },
            text = {
                Text(
                    "Their recordings will stay on this device, but no stories will play in " +
                        "this voice until the profile is added again.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ProfilesStore.remove(target.id)
                    removeTarget = null
                }) { Text("Remove", color = AppColors.record) }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("Voices", nav) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "WHO READS THE STORIES?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (ProfilesStore.profiles.isEmpty()) {
                item {
                    Text(
                        "Add a voice for each person who will read — Mom, Dad, Grandma, anyone " +
                            "your little one loves to hear.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(ProfilesStore.profiles, key = { it.id }) { profile ->
                val isActive = profile.id == ProfilesStore.activeProfileId
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { ProfilesStore.setActive(profile.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) AppColors.selected else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(profile.emoji, fontSize = 28.sp)
                        Text(
                            profile.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isActive) {
                            Text(
                                "Reading now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "✕",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { removeTarget = profile },
                        )
                    }
                }
            }

            item {
                Text(
                    "ADD A VOICE",
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EMOJI_CHOICES.forEach { choice ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (choice == emoji) AppColors.selected
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape,
                                )
                                .border(
                                    2.dp,
                                    if (choice == emoji) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable { emoji = choice },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(choice, fontSize = 24.sp)
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Name (e.g. Mom)") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                ProfilesStore.add(name, emoji)
                                name = ""
                                emoji = EMOJI_CHOICES[0]
                            }
                        },
                        enabled = name.isNotBlank(),
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text(
                    "BACKUP",
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(Backup.SUGGESTED_FILE_NAME) },
                        modifier = Modifier.weight(1f),
                        enabled = !backupBusy && ProfilesStore.profiles.isNotEmpty(),
                    ) {
                        Text("💾 Back up", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/zip")) },
                        modifier = Modifier.weight(1f),
                        enabled = !backupBusy,
                    ) {
                        Text("📥 Restore", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Text(
                    if (backupBusy) "Working…"
                    else "Back up saves every voice and recording into one .zip — keep it in " +
                        "Downloads or Google Drive. Restore accepts backups and story gifts " +
                        "sent from another phone.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Text(
                    "VOICE AI (ELEVENLABS)",
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("ElevenLabs API key") },
                        singleLine = true,
                    )
                    Button(
                        onClick = { AppPrefs.updateElevenLabsKey(apiKeyDraft) },
                        enabled = apiKeyDraft.trim() != AppPrefs.elevenLabsKey,
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text(
                    if (AppPrefs.elevenLabsKey.isNotBlank()) {
                        "Key saved ✅ — create voice models from the 🎭 Voice Cast screen. " +
                            "Models read unrecorded lines in that voice, always labeled ✨."
                    } else {
                        "Paste an API key from elevenlabs.io (Profile → API Keys) to turn " +
                            "recorded samples into voice models."
                    },
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (VoiceModels.models.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { showDeleteModels = true },
                        enabled = !backupBusy,
                    ) {
                        Text(
                            "🗑 Delete all voice models (${VoiceModels.models.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.record,
                        )
                    }
                }
            }

            item {
                Text(
                    "🔒 Recordings stay on this device. Creating a voice model uploads that " +
                        "voice's samples to ElevenLabs with the owner's consent — and the " +
                        "delete button above removes them again, everywhere.",
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
