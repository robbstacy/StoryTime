package com.robbstacy.bedtimecast.ui

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.audio.SegmentPlayer
import com.robbstacy.bedtimecast.data.AppPrefs
import com.robbstacy.bedtimecast.data.Backup
import com.robbstacy.bedtimecast.data.CloneEngine
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.data.VoiceModels
import com.robbstacy.bedtimecast.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NIGHT_BG = Color(0xFF12131F)
private val NIGHT_TEXT = Color(0xFFD8C6A8)
private val NIGHT_DIM = Color(0xFF8E8574)

@Composable
fun StoryScreen(nav: NavController, storyId: String, startPage: Int) {
    val context = LocalContext.current
    val story = StoryRepository.story(context, storyId) ?: return
    val profile = ProfilesStore.activeProfile
    val scope = rememberCoroutineScope()

    var narr by remember {
        mutableStateOf(Narration.resolveMatrix(context, story))
    }
    var pageIndex by remember {
        mutableIntStateOf(startPage.coerceIn(0, story.pages.size - 1))
    }
    var segIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var bedtime by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf<Int?>(null) }
    var timerEndAt by remember { mutableStateOf<Long?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var generating by remember { mutableStateOf<String?>(null) }
    var showCasting by remember { mutableStateOf(false) }
    var showReaders by remember { mutableStateOf(false) }

    val player = remember { SegmentPlayer() }
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    // Remember where this story was left off.
    LaunchedEffect(pageIndex) {
        AppPrefs.saveResume(story.id, pageIndex)
    }

    // Keep the screen on while reading at bedtime or during playback.
    val view = LocalView.current
    DisposableEffect(bedtime, playing) {
        view.keepScreenOn = bedtime || playing
        onDispose { view.keepScreenOn = false }
    }

    val page = story.pages[pageIndex]
    val pageNarr = narr[pageIndex]
    val pagePlayable = pageNarr.any { it.file != null }
    val isLastPage = pageIndex >= story.pages.size - 1
    val anyMissing = narr.flatten().any { it.kind == Narration.Kind.NONE }
    // Story gifts pack the ACTIVE profile's own recordings.
    val giftable = profile != null &&
        Narration.recordedMatrix(context, profile.id, story).flatten().any { it }

    fun playFrom(p: Int, s: Int) {
        val file = narr[p][s].file
        if (file == null) {
            playing = false
            return
        }
        pageIndex = p
        segIndex = s
        playing = true
        paused = false
        player.play(file) {
            val nextSeg = s + 1
            when {
                nextSeg < story.pages[p].segments.size -> playFrom(p, nextSeg)
                p + 1 < story.pages.size -> playFrom(p + 1, 0)
                else -> playing = false
            }
        }
    }

    fun goToPage(index: Int) {
        player.stop()
        playing = false
        paused = false
        pageIndex = index
        segIndex = 0
    }

    // Sleep timer: when it expires, fade the volume out gently and stop.
    LaunchedEffect(timerEndAt) {
        val end = timerEndAt ?: return@LaunchedEffect
        while (System.currentTimeMillis() < end) {
            delay(1_000)
        }
        for (step in 9 downTo 0) {
            player.setVolume(step / 10f)
            delay(500)
        }
        player.stop()
        playing = false
        paused = false
        timerEndAt = null
        timerMinutes = null
    }

    fun shareStoryGift() {
        val prof = profile ?: return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                Backup.exportPack(context, prof.id, story, prof.name, prof.emoji)
            }
            result.fold(
                onSuccess = { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.robbstacy.bedtimecast.fileprovider",
                        file,
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(send, "Send \"${story.title}\" read by ${prof.name}"),
                    )
                },
                onFailure = { infoMessage = it.message ?: "Could not create the story gift." },
            )
        }
    }

    fun generateMissing() {
        val prof = profile ?: return
        generating = "Starting…"
        scope.launch {
            val result = CloneEngine.generateMissing(context, prof, story) { done, total ->
                generating = "Generating line $done of $total…"
            }
            generating = null
            result.fold(
                onSuccess = { count ->
                    narr = Narration.resolveMatrix(context, story)
                    infoMessage = "✨ Generated $count lines."
                },
                onFailure = { infoMessage = it.message ?: "Generation failed." },
            )
        }
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text(story.title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { infoMessage = null }) { Text("OK") }
            },
        )
    }

    if (showReaders) {
        AlertDialog(
            onDismissRequest = { showReaders = false },
            title = { Text("👥 Who reads what?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Tap a role to choose its reader — perfect for stories read together. " +
                            "Auto plays whoever recorded each line.",
                        fontSize = 13.sp,
                    )
                    val roles = listOf("narrator" to "📖 Narrator") +
                        story.characters.map { it.id to "${it.emoji} ${it.name}" }
                    roles.forEach { (speakerId, label) ->
                        val assignedId = com.robbstacy.bedtimecast.data.ReaderCast
                            .reader(story.id, speakerId)
                        val options = listOf<String?>(null) + ProfilesStore.profiles.map { it.id }
                        val currentLabel = ProfilesStore.byId(assignedId)
                            ?.let { "${it.emoji} ${it.name}" } ?: "Auto"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val index = options.indexOf(assignedId)
                                val next = options[(index + 1) % options.size]
                                com.robbstacy.bedtimecast.data.ReaderCast
                                    .assign(story.id, speakerId, next)
                                narr = Narration.resolveMatrix(context, story)
                            },
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "→ $currentLabel",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReaders = false }) { Text("Done") }
            },
        )
    }

    if (showCasting && profile != null) {
        AlertDialog(
            onDismissRequest = { showCasting = false },
            title = { Text("🎭 Cast the roles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Tap a role to change which voice model reads its lines when " +
                            "generating. Real recordings always win.",
                        fontSize = 13.sp,
                    )
                    story.characters.forEach { character ->
                        val assignedKey = VoiceModels.assignedKey(profile.id, story.id, character.id)
                        val options = listOf<String?>(null) +
                            VoiceModels.modelsFor(profile.id).map { it.key }
                        val currentLabel = assignedKey
                            ?.let { VoiceModels.model(profile.id, it)?.displayName }
                            ?: "Auto (narrator voice)"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val index = options.indexOf(assignedKey)
                                val next = options[(index + 1) % options.size]
                                VoiceModels.assign(profile.id, story.id, character.id, next)
                            },
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    "${character.emoji} ${character.name}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    "→ $currentLabel",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCasting = false }) { Text("Done") }
            },
        )
    }

    // Bedtime mode swaps the palette for a dim, warm night look.
    val bgColor = if (bedtime) NIGHT_BG else MaterialTheme.colorScheme.background
    val mainText = if (bedtime) NIGHT_TEXT else MaterialTheme.colorScheme.onBackground
    val dimText = if (bedtime) NIGHT_DIM else MaterialTheme.colorScheme.onSurfaceVariant

    val activelyPlaying = playing && !paused
    val currentSegment = page.segments.getOrNull(segIndex)
    val currentNarration = pageNarr.getOrNull(segIndex)
    val currentCharacter = currentSegment
        ?.takeIf { it.speaker != "narrator" }
        ?.let { story.character(it.speaker) }
    val firstMissing = pageNarr.indexOfFirst { it.file == null }
    val hasModels = profile != null && VoiceModels.modelsFor(profile.id).isNotEmpty()
    val currentReaderName = ProfilesStore.byId(currentNarration?.readerId)?.name
        ?: profile?.name.orEmpty()
    val pageReaders = pageNarr.mapNotNull { ProfilesStore.byId(it.readerId)?.name }
        .distinct()
        .joinToString(" & ")

    Scaffold(
        containerColor = bgColor,
        topBar = {
            AppTopBar(
                title = story.title,
                nav = nav,
                containerColor = bgColor,
                titleColor = mainText,
                actions = {
                    if (!AppPrefs.kidMode && ProfilesStore.profiles.size > 1) {
                        TextButton(onClick = { showReaders = true }) {
                            Text("👥", fontSize = 18.sp)
                        }
                    }
                    if (!AppPrefs.kidMode && hasModels) {
                        TextButton(onClick = { showCasting = true }) {
                            Text("🎭", fontSize = 18.sp)
                        }
                    }
                    if (!AppPrefs.kidMode && giftable) {
                        TextButton(onClick = { shareStoryGift() }) {
                            Text("🎁", fontSize = 18.sp)
                        }
                    }
                    TextButton(onClick = {
                        bedtime = !bedtime
                        if (!bedtime) {
                            timerEndAt = null
                            timerMinutes = null
                        }
                    }) {
                        Text(if (bedtime) "☀️" else "🌙", fontSize = 18.sp)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Source badge
            val badgeText: String
            val badgeColor: Color
            when {
                activelyPlaying && currentNarration?.kind == Narration.Kind.CLONED -> {
                    badgeText = if (currentCharacter != null) {
                        "✨ ${currentCharacter.emoji} $currentReaderName's ${currentCharacter.name} (AI)"
                    } else {
                        "✨ $currentReaderName's voice (AI)"
                    }
                    badgeColor = if (bedtime) NIGHT_DIM else AppColors.gold
                }
                activelyPlaying && currentCharacter != null -> {
                    badgeText = "${currentCharacter.emoji} $currentReaderName's ${currentCharacter.name}"
                    badgeColor = if (bedtime) NIGHT_TEXT else characterColor(story, currentCharacter.id)
                }
                pageNarr.isNotEmpty() && pageNarr.all { it.kind == Narration.Kind.RECORDED } &&
                    pageReaders.isNotEmpty() -> {
                    badgeText = "❤️ Read by $pageReaders"
                    badgeColor = if (bedtime) NIGHT_DIM else AppColors.recorded
                }
                pagePlayable -> {
                    badgeText = if (pageNarr.any { it.kind == Narration.Kind.CLONED }) {
                        "❤️+✨ Recorded and AI lines"
                    } else {
                        "Some lines aren't recorded yet"
                    }
                    badgeColor = dimText
                }
                else -> {
                    badgeText = "This page isn't recorded yet"
                    badgeColor = dimText
                }
            }
            Text(
                badgeText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor,
            )

            // Sleep timer chips (bedtime mode only)
            if (bedtime) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    listOf<Pair<String, Int?>>(
                        "Timer off" to null,
                        "15 min" to 15,
                        "30 min" to 30,
                    ).forEach { (label, minutes) ->
                        val selected = timerMinutes == minutes
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) NIGHT_DIM.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier.clickable {
                                timerMinutes = minutes
                                timerEndAt = minutes?.let {
                                    System.currentTimeMillis() + it * 60_000L
                                }
                            },
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) NIGHT_TEXT else NIGHT_DIM,
                            )
                        }
                    }
                }
            }

            // Page card
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = coverColor(story).copy(alpha = if (bedtime) 0.08f else 0.14f),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(story.coverEmoji, fontSize = 42.sp)
                    Text(
                        buildAnnotatedString {
                            page.segments.forEachIndexed { index, segment ->
                                val isCurrent = activelyPlaying && index == segIndex
                                val isDialogue = segment.speaker != "narrator"
                                withStyle(
                                    SpanStyle(
                                        color = when {
                                            bedtime -> NIGHT_TEXT
                                            isDialogue -> characterColor(story, segment.speaker)
                                            else -> MaterialTheme.colorScheme.onBackground
                                        },
                                        fontWeight = if (isDialogue && !bedtime) FontWeight.SemiBold else null,
                                        textDecoration = if (isCurrent) TextDecoration.Underline else null,
                                    ),
                                ) {
                                    append(segment.text)
                                }
                                if (index < page.segments.size - 1) append(" ")
                            }
                        },
                        fontSize = 22.sp,
                        lineHeight = 34.sp,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Page dots
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                story.pages.forEachIndexed { index, _ ->
                    val row = narr[index]
                    val dotColor = when {
                        index == pageIndex -> if (bedtime) NIGHT_TEXT else MaterialTheme.colorScheme.primary
                        row.isNotEmpty() && row.all { it.kind == Narration.Kind.RECORDED } ->
                            if (bedtime) NIGHT_DIM else AppColors.recorded
                        row.isNotEmpty() && row.all { it.file != null } ->
                            if (bedtime) NIGHT_DIM else AppColors.gold
                        row.any { it.file != null } ->
                            if (bedtime) NIGHT_DIM.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else -> if (bedtime) NIGHT_DIM.copy(alpha = 0.25f) else AppColors.selected
                    }
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                            .clickable { goToPage(index) },
                    )
                }
            }

            // Controls
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    fontSize = 36.sp,
                    color = mainText.copy(alpha = if (pageIndex == 0) 0.3f else 1f),
                    modifier = Modifier.clickable(enabled = pageIndex > 0) { goToPage(pageIndex - 1) },
                )

                if (pagePlayable) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                if (bedtime) NIGHT_DIM else MaterialTheme.colorScheme.primary,
                                CircleShape,
                            )
                            .clickable {
                                when {
                                    playing && !paused -> {
                                        player.pause()
                                        paused = true
                                    }
                                    playing && paused -> {
                                        player.resume()
                                        paused = false
                                    }
                                    else -> playFrom(
                                        pageIndex,
                                        pageNarr.indexOfFirst { it.file != null }.coerceAtLeast(0),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (activelyPlaying) "⏸" else "▶",
                            fontSize = 28.sp,
                            color = if (bedtime) NIGHT_BG else MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else if (!AppPrefs.kidMode) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.clickable {
                            nav.navigate("record/${story.id}?page=$pageIndex&seg=0")
                        },
                    ) {
                        Text(
                            "🎙 Record this page",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        "Ask a grown-up to record this page 😊",
                        fontSize = 14.sp,
                        color = dimText,
                    )
                }

                Text(
                    "›",
                    fontSize = 36.sp,
                    color = mainText.copy(alpha = if (isLastPage) 0.3f else 1f),
                    modifier = Modifier.clickable(enabled = !isLastPage) { goToPage(pageIndex + 1) },
                )
            }

            if (!AppPrefs.kidMode && pagePlayable && firstMissing >= 0) {
                Text(
                    "🎙 Record the missing lines on this page",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("record/${story.id}?page=$pageIndex&seg=$firstMissing") },
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bedtime) NIGHT_DIM else MaterialTheme.colorScheme.primary,
                )
            }

            // Voice-model generation for everything still missing
            if (!AppPrefs.kidMode && profile != null && anyMissing &&
                AppPrefs.elevenLabsKey.isNotBlank() && VoiceModels.narrator(profile.id) != null
            ) {
                Text(
                    generating ?: "✨ Generate the missing lines in ${profile.name}'s voice",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = generating == null) { generateMissing() },
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bedtime) NIGHT_DIM else AppColors.gold,
                )
            }

            Text(
                "Page ${pageIndex + 1} of ${story.pages.size}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = dimText,
            )
        }
    }
}
