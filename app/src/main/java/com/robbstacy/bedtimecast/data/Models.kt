package com.robbstacy.bedtimecast.data

import kotlinx.serialization.Serializable

/**
 * A run of text spoken by one voice. `speaker` is "narrator" or the id of a
 * StoryCharacter. Segments are the unit of recording and playback — and every
 * recorded character segment doubles as a labeled (text, audio, character)
 * sample for future voice modeling.
 */
@Serializable
data class StorySegment(val speaker: String, val text: String)

@Serializable
data class StoryCharacter(
    val id: String,
    val name: String,
    val emoji: String,
    /** How the parent should perform it, e.g. "a big, gruff, growly voice". */
    val voiceHint: String,
)

@Serializable
data class StoryPage(val segments: List<StorySegment>)

@Serializable
data class Story(
    val id: String,
    val title: String,
    val author: String,
    val year: Int,
    val source: String,
    val coverEmoji: String,
    val coverColor: String,
    val minutes: Int,
    val characters: List<StoryCharacter>,
    val pages: List<StoryPage>,
) {
    fun character(speakerId: String): StoryCharacter? = characters.find { it.id == speakerId }
}

@Serializable
data class VoiceProfile(
    val id: String,
    val name: String,
    val emoji: String,
    val createdAt: Long,
)

/**
 * One character as performed by one voice profile in one story — the unit of
 * the "voice cast". Recorded lines are labeled voice-model training samples.
 */
data class CastMember(
    val story: Story,
    val character: StoryCharacter,
    val totalLines: Int,
    val recordedLines: Int,
    /** Word count of recorded lines — a rough proxy for sample richness. */
    val recordedWords: Int,
)
