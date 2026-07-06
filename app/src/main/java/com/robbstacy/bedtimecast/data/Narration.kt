package com.robbstacy.bedtimecast.data

import android.content.Context
import java.io.File

/**
 * Narration storage — the heart of the hybrid recorded/cloned model.
 *
 * The unit of audio is the SEGMENT: a run of text spoken by one voice
 * (the narrator or a character). Recordings live on-device under:
 *   filesDir/recordings/<profileId>/<storyId>/p<page>-s<segment>.m4a
 *
 * Because every character segment is recorded against known text and a known
 * character, recordings double as labeled training samples for future
 * character voice models (see [cast]).
 */
object Narration {

    fun segmentFile(
        context: Context,
        profileId: String,
        storyId: String,
        page: Int,
        segment: Int,
    ): File {
        val dir = File(File(File(context.filesDir, "recordings"), profileId), storyId)
        return File(dir, "p$page-s$segment.m4a")
    }

    fun deleteSegment(context: Context, profileId: String, storyId: String, page: Int, segment: Int) {
        segmentFile(context, profileId, storyId, page, segment).delete()
    }

    /** Cloned (AI-generated) narration cache, mirroring the recordings layout. */
    fun clonedFile(
        context: Context,
        profileId: String,
        storyId: String,
        page: Int,
        segment: Int,
    ): File {
        val dir = File(File(File(context.filesDir, "cloned"), profileId), storyId)
        return File(dir, "p$page-s$segment.mp3")
    }

    enum class Kind { RECORDED, CLONED, NONE }

    data class SegmentNarration(val kind: Kind, val file: File?)

    /** Real recording first, then cached cloned audio, then nothing. */
    fun segmentNarration(
        context: Context,
        profileId: String?,
        storyId: String,
        page: Int,
        segment: Int,
    ): SegmentNarration {
        if (profileId == null) return SegmentNarration(Kind.NONE, null)
        val recorded = segmentFile(context, profileId, storyId, page, segment)
        if (recorded.exists()) return SegmentNarration(Kind.RECORDED, recorded)
        val cloned = clonedFile(context, profileId, storyId, page, segment)
        if (cloned.exists()) return SegmentNarration(Kind.CLONED, cloned)
        return SegmentNarration(Kind.NONE, null)
    }

    fun narrationMatrix(context: Context, profileId: String?, story: Story): List<List<SegmentNarration>> =
        story.pages.mapIndexed { pageIndex, page ->
            page.segments.indices.map { segIndex ->
                segmentNarration(context, profileId, story.id, pageIndex, segIndex)
            }
        }

    data class SampleStats(val lines: Int, val words: Int)

    /** Recorded narrator material across the whole library for one profile. */
    fun narratorStats(context: Context, profileId: String, stories: List<Story>): SampleStats {
        var lines = 0
        var words = 0
        for (story in stories) {
            story.pages.forEachIndexed { pageIndex, page ->
                page.segments.forEachIndexed { segIndex, segment ->
                    if (segment.speaker != "narrator") return@forEachIndexed
                    if (segmentFile(context, profileId, story.id, pageIndex, segIndex).exists()) {
                        lines++
                        words += segment.text.split(Regex("\\s+")).count { it.isNotBlank() }
                    }
                }
            }
        }
        return SampleStats(lines, words)
    }

    fun narratorFiles(context: Context, profileId: String, stories: List<Story>): List<File> {
        val files = mutableListOf<File>()
        for (story in stories) {
            story.pages.forEachIndexed { pageIndex, page ->
                page.segments.forEachIndexed { segIndex, segment ->
                    if (segment.speaker != "narrator") return@forEachIndexed
                    val file = segmentFile(context, profileId, story.id, pageIndex, segIndex)
                    if (file.exists()) files.add(file)
                }
            }
        }
        return files
    }

    fun characterFiles(
        context: Context,
        profileId: String,
        story: Story,
        characterId: String,
    ): List<File> {
        val files = mutableListOf<File>()
        story.pages.forEachIndexed { pageIndex, page ->
            page.segments.forEachIndexed { segIndex, segment ->
                if (segment.speaker != characterId) return@forEachIndexed
                val file = segmentFile(context, profileId, story.id, pageIndex, segIndex)
                if (file.exists()) files.add(file)
            }
        }
        return files
    }

    /** Per page, per segment: is a recording present? */
    fun recordedMatrix(context: Context, profileId: String?, story: Story): List<List<Boolean>> =
        story.pages.mapIndexed { pageIndex, page ->
            page.segments.indices.map { segIndex ->
                profileId != null &&
                    segmentFile(context, profileId, story.id, pageIndex, segIndex).exists()
            }
        }

    data class Summary(val recorded: Int, val total: Int) {
        val complete: Boolean get() = total > 0 && recorded == total
        val none: Boolean get() = recorded == 0
    }

    fun summarize(context: Context, profileId: String?, story: Story): Summary {
        val flat = recordedMatrix(context, profileId, story).flatten()
        return Summary(recorded = flat.count { it }, total = flat.size)
    }

    /** All characters the profile could perform, with recording coverage. */
    fun cast(context: Context, profileId: String?, stories: List<Story>): List<CastMember> {
        if (profileId == null) return emptyList()
        val members = mutableListOf<CastMember>()
        for (story in stories) {
            for (character in story.characters) {
                var totalLines = 0
                var recordedLines = 0
                var recordedWords = 0
                story.pages.forEachIndexed { pageIndex, page ->
                    page.segments.forEachIndexed { segIndex, segment ->
                        if (segment.speaker != character.id) return@forEachIndexed
                        totalLines++
                        if (segmentFile(context, profileId, story.id, pageIndex, segIndex).exists()) {
                            recordedLines++
                            recordedWords += segment.text.split(Regex("\\s+")).count { it.isNotBlank() }
                        }
                    }
                }
                if (totalLines > 0) {
                    members.add(CastMember(story, character, totalLines, recordedLines, recordedWords))
                }
            }
        }
        return members
    }
}
