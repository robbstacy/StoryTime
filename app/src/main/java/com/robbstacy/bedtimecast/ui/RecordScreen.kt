package com.robbstacy.bedtimecast.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.audio.SegmentPlayer
import com.robbstacy.bedtimecast.audio.SegmentRecorder
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.ui.theme.AppColors

private data class ScriptLine(
    val pageIndex: Int,
    val segIndex: Int,
    val speaker: String,
    val text: String,
)

@Composable
fun RecordScreen(nav: NavController, storyId: String, startPage: Int, startSeg: Int) {
    val context = LocalContext.current
    val story = StoryRepository.story(context, storyId) ?: return
    val profile = ProfilesStore.activeProfile

    if (profile == null) {
        Scaffold(topBar = { AppTopBar("Record", nav) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Add a voice profile first, so BedtimeCast knows whose voice this is. " +
                        "Go back and tap the chip at the top of the library.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        return
    }

    // The story flattened into a script: one line per segment, in reading order.
    val script = remember(story) {
        story.pages.flatMapIndexed { pageIndex, page ->
            page.segments.mapIndexed { segIndex, segment ->
                ScriptLine(pageIndex, segIndex, segment.speaker, segment.text)
            }
        }
    }

    var lineIndex by remember {
        mutableIntStateOf(
            script.indexOfFirst { it.pageIndex == startPage && it.segIndex == startSeg }
                .coerceAtLeast(0),
        )
    }
    var matrix by remember {
        mutableStateOf(Narration.recordedMatrix(context, profile.id, story))
    }
    var isRecording by remember { mutableStateOf(false) }
    var showRedoDialog by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    val recorder = remember { SegmentRecorder(context) }
    val previewPlayer = remember { SegmentPlayer() }
    DisposableEffect(Unit) {
        onDispose {
            recorder.stop()
            previewPlayer.stop()
        }
    }

    val line = script[lineIndex]
    val character = if (line.speaker == "narrator") null else story.character(line.speaker)
    val speakerColor =
        if (character == null) MaterialTheme.colorScheme.onSurfaceVariant
        else characterColor(story, character.id)
    val recordedCount = matrix.flatten().count { it }
    val totalLines = script.size
    val currentRecorded = matrix[line.pageIndex][line.segIndex]
    val currentFile = Narration.segmentFile(context, profile.id, story.id, line.pageIndex, line.segIndex)

    fun refreshMatrix() {
        matrix = Narration.recordedMatrix(context, profile.id, story)
    }

    fun toggleRecording() {
        if (isRecording) {
            recorder.stop()
            isRecording = false
            refreshMatrix()
        } else {
            if (!permissionGranted) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            previewPlayer.stop()
            runCatching { recorder.start(currentFile) }
                .onSuccess { isRecording = true }
        }
    }

    fun goToLine(index: Int) {
        if (isRecording) return
        previewPlayer.stop()
        lineIndex = index.coerceIn(0, totalLines - 1)
    }

    if (showRedoDialog) {
        AlertDialog(
            onDismissRequest = { showRedoDialog = false },
            title = { Text("Re-record this line?") },
            text = { Text("The current take will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showRedoDialog = false
                    previewPlayer.stop()
                    Narration.deleteSegment(context, profile.id, story.id, line.pageIndex, line.segIndex)
                    refreshMatrix()
                }) { Text("Delete take", color = AppColors.record) }
            },
            dismissButton = {
                TextButton(onClick = { showRedoDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("Record · ${story.title}", nav) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${profile.emoji} ${profile.name} · $recordedCount of $totalLines lines recorded",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LinearProgressIndicator(
                progress = { if (totalLines == 0) 0f else recordedCount.toFloat() / totalLines },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = AppColors.recorded,
                trackColor = AppColors.selected,
            )

            // Speaker cue card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(character?.emoji ?: "📖", fontSize = 26.sp)
                    Column {
                        Text(
                            character?.name ?: "Narrator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (character == null) MaterialTheme.colorScheme.onSurface else speakerColor,
                        )
                        Text(
                            character?.let { "🎭 Perform it in ${it.voiceHint}!" }
                                ?: "Your own storytelling voice",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Teleprompter
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "PAGE ${line.pageIndex + 1} · LINE ${lineIndex + 1} OF $totalLines — READ THIS ALOUD:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        line.text,
                        fontSize = 26.sp,
                        lineHeight = 40.sp,
                        fontFamily = FontFamily.Serif,
                        color = if (character == null) MaterialTheme.colorScheme.onSurface else speakerColor,
                    )
                }
            }

            if (!permissionGranted) {
                Text(
                    "Microphone access is needed to record. Tap the record button to allow it.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = AppColors.record,
                )
            }

            // Controls: listen · record · redo
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(88.dp), contentAlignment = Alignment.Center) {
                    if (currentRecorded && !isRecording) {
                        Text(
                            "▶ Listen",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                previewPlayer.play(currentFile) {}
                            },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(4.dp, AppColors.record, CircleShape)
                        .background(
                            if (isRecording) AppColors.record else MaterialTheme.colorScheme.background,
                            CircleShape,
                        )
                        .clickable { toggleRecording() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isRecording) {
                        Box(
                            Modifier.size(30.dp).background(
                                MaterialTheme.colorScheme.onPrimary,
                                RoundedCornerShape(6.dp),
                            ),
                        )
                    } else {
                        Box(Modifier.size(60.dp).background(AppColors.record, CircleShape))
                    }
                }

                Box(Modifier.width(88.dp), contentAlignment = Alignment.Center) {
                    if (currentRecorded && !isRecording) {
                        Text(
                            "↺ Redo",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.record,
                            modifier = Modifier.clickable { showRedoDialog = true },
                        )
                    }
                }
            }

            Text(
                when {
                    isRecording -> "Recording… tap to stop"
                    currentRecorded -> "This line is saved ❤️"
                    else -> "Tap to start recording"
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Line navigation
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "‹ Previous",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (lineIndex == 0 || isRecording) 0.3f else 1f,
                    ),
                    modifier = Modifier.clickable(enabled = lineIndex > 0 && !isRecording) {
                        goToLine(lineIndex - 1)
                    },
                )
                Text(
                    "Next ›",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (lineIndex >= totalLines - 1 || isRecording) 0.3f else 1f,
                    ),
                    modifier = Modifier.clickable(enabled = lineIndex < totalLines - 1 && !isRecording) {
                        goToLine(lineIndex + 1)
                    },
                )
            }
        }
    }
}
