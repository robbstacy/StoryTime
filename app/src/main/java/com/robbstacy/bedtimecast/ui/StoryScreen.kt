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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.audio.SegmentPlayer
import com.robbstacy.bedtimecast.data.Narration
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.data.StoryRepository
import com.robbstacy.bedtimecast.ui.theme.AppColors

@Composable
fun StoryScreen(nav: NavController, storyId: String) {
    val context = LocalContext.current
    val story = StoryRepository.story(context, storyId) ?: return
    val profile = ProfilesStore.activeProfile

    var matrix by remember {
        mutableStateOf(Narration.recordedMatrix(context, profile?.id, story))
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    var segIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }

    val player = remember { SegmentPlayer() }
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val page = story.pages[pageIndex]
    val pageRecorded = matrix[pageIndex]
    val pagePlayable = pageRecorded.any { it }
    val isLastPage = pageIndex >= story.pages.size - 1

    // Plays segment (p, s); on completion chains to the next recorded segment,
    // turning the page when the current one ends.
    fun playFrom(p: Int, s: Int) {
        val prof = profile ?: return
        val file = Narration.segmentFile(context, prof.id, story.id, p, s)
        if (!file.exists()) {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(story.title, nav) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Source badge
            val currentSegment = page.segments.getOrNull(segIndex)
            val currentCharacter = currentSegment
                ?.takeIf { it.speaker != "narrator" }
                ?.let { story.character(it.speaker) }
            val badgeText: String
            val badgeColor: androidx.compose.ui.graphics.Color
            when {
                playing && !paused && currentCharacter != null && profile != null -> {
                    badgeText = "${currentCharacter.emoji} ${profile.name}'s ${currentCharacter.name}"
                    badgeColor = characterColor(story, currentCharacter.id)
                }
                pageRecorded.all { it } && profile != null -> {
                    badgeText = "❤️ Read by ${profile.name}"
                    badgeColor = AppColors.recorded
                }
                pagePlayable -> {
                    badgeText = "Some lines aren't recorded yet"
                    badgeColor = MaterialTheme.colorScheme.onSurfaceVariant
                }
                else -> {
                    badgeText = "This page isn't recorded yet"
                    badgeColor = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Page card
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = coverColor(story).copy(alpha = 0.14f),
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
                                val isCurrent = playing && index == segIndex
                                val isDialogue = segment.speaker != "narrator"
                                withStyle(
                                    SpanStyle(
                                        color = if (isDialogue) characterColor(story, segment.speaker)
                                        else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = if (isDialogue) FontWeight.SemiBold else null,
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
                    val dotColor = when {
                        index == pageIndex -> MaterialTheme.colorScheme.primary
                        matrix[index].all { it } && matrix[index].isNotEmpty() -> AppColors.recorded
                        matrix[index].any { it } -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else -> AppColors.selected
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
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (pageIndex == 0) 0.3f else 1f,
                    ),
                    modifier = Modifier.clickable(enabled = pageIndex > 0) { goToPage(pageIndex - 1) },
                )

                if (pagePlayable) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
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
                                    else -> playFrom(pageIndex, pageRecorded.indexOfFirst { it }.coerceAtLeast(0))
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (playing && !paused) "⏸" else "▶",
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
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
                }

                Text(
                    "›",
                    fontSize = 36.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (isLastPage) 0.3f else 1f,
                    ),
                    modifier = Modifier.clickable(enabled = !isLastPage) { goToPage(pageIndex + 1) },
                )
            }

            val firstMissing = pageRecorded.indexOfFirst { !it }
            if (pagePlayable && firstMissing >= 0) {
                Text(
                    "🎙 Record the missing lines on this page",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("record/${story.id}?page=$pageIndex&seg=$firstMissing") },
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                "Page ${pageIndex + 1} of ${story.pages.size}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
