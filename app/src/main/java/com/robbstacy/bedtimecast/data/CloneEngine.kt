package com.robbstacy.bedtimecast.data

import android.content.Context
import com.robbstacy.bedtimecast.net.ElevenLabs
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Builds voice models from recorded samples and generates missing narration. */
object CloneEngine {

    const val NARRATOR_MIN_WORDS = 40
    const val CHARACTER_MIN_WORDS = 20
    private const val MAX_SAMPLE_FILES = 15

    private fun apiKeyOrNull(): String? = AppPrefs.elevenLabsKey.ifBlank { null }

    suspend fun createNarratorModel(context: Context, profile: VoiceProfile): Result<VoiceModel> =
        withContext(Dispatchers.IO) {
            val apiKey = apiKeyOrNull()
                ?: return@withContext Result.failure(
                    IllegalStateException("Add your ElevenLabs API key on the Voices screen first."),
                )
            val files = Narration
                .narratorFiles(context, profile.id, StoryRepository.stories(context))
                .sortedByDescending { it.length() }
                .take(MAX_SAMPLE_FILES)
            if (files.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Record some narrator lines first — they are the voice sample."),
                )
            }
            ElevenLabs.createVoice(apiKey, "${profile.name} Narrator (BedtimeCast)", files)
                .map { voiceId ->
                    val model = VoiceModel(
                        key = VoiceModels.NARRATOR_KEY,
                        profileId = profile.id,
                        displayName = "${profile.name} — Narrator",
                        elevenVoiceId = voiceId,
                        createdAt = System.currentTimeMillis(),
                    )
                    VoiceModels.add(model)
                    model
                }
        }

    suspend fun createCharacterModel(
        context: Context,
        profile: VoiceProfile,
        story: Story,
        character: StoryCharacter,
    ): Result<VoiceModel> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOrNull()
            ?: return@withContext Result.failure(
                IllegalStateException("Add your ElevenLabs API key on the Voices screen first."),
            )
        val files = Narration.characterFiles(context, profile.id, story, character.id)
        if (files.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Record ${character.name}'s lines first."),
            )
        }
        val name = "${profile.name} ${character.name} (BedtimeCast)"
        ElevenLabs.createVoice(apiKey, name, files).map { voiceId ->
            val model = VoiceModel(
                key = "${story.id}/${character.id}",
                profileId = profile.id,
                displayName = "${profile.name}'s ${character.name}",
                elevenVoiceId = voiceId,
                createdAt = System.currentTimeMillis(),
            )
            VoiceModels.add(model)
            model
        }
    }

    private data class Target(val page: Int, val segment: Int, val speaker: String, val text: String)

    /** Generates cloned audio for every segment with no narration yet. */
    suspend fun generateMissing(
        context: Context,
        profile: VoiceProfile,
        story: Story,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Result<Int> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOrNull()
            ?: return@withContext Result.failure(
                IllegalStateException("Add your ElevenLabs API key on the Voices screen first."),
            )
        val targets = mutableListOf<Target>()
        story.pages.forEachIndexed { pageIndex, page ->
            page.segments.forEachIndexed { segIndex, segment ->
                // Duet-aware: only lines no reader has covered need generating.
                val current = Narration.resolveSegment(context, story, pageIndex, segIndex)
                if (current.kind == Narration.Kind.NONE) {
                    targets.add(Target(pageIndex, segIndex, segment.speaker, segment.text))
                }
            }
        }
        if (targets.isEmpty()) return@withContext Result.success(0)

        var done = 0
        for (target in targets) {
            // Prefer the assigned reader's voice models, else the active profile's.
            val reader = ProfilesStore.byId(ReaderCast.reader(story.id, target.speaker)) ?: profile
            val model = if (target.speaker == "narrator") {
                VoiceModels.narrator(reader.id) ?: VoiceModels.narrator(profile.id)
            } else {
                VoiceModels.resolve(reader.id, story.id, target.speaker)
                    ?: VoiceModels.resolve(profile.id, story.id, target.speaker)
            } ?: return@withContext Result.failure(
                IllegalStateException(
                    "Create ${profile.name}'s narrator voice model first (Voice Cast screen).",
                ),
            )
            val audio = ElevenLabs.textToSpeech(apiKey, model.elevenVoiceId, target.text)
                .getOrElse { return@withContext Result.failure(it) }
            val dest = Narration.clonedFile(
                context, model.profileId, story.id, target.page, target.segment,
            )
            dest.parentFile?.mkdirs()
            dest.writeBytes(audio)
            done++
            onProgress(done, targets.size)
        }
        Result.success(done)
    }

    /** Deletes every voice model remotely and all cached cloned audio locally. */
    suspend fun deleteAllModels(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOrNull()
        val voiceIds = VoiceModels.clearAll()
        if (apiKey != null) {
            voiceIds.forEach { ElevenLabs.deleteVoice(apiKey, it) }
        }
        File(context.filesDir, "cloned").deleteRecursively()
        Result.success(voiceIds.size)
    }
}
