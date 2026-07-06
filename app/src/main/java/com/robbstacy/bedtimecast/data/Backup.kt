package com.robbstacy.bedtimecast.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Backups and story gifts, both plain .zip files.
 *
 * Full backup:  backup.json (all profiles) + recordings/<profileId>/<storyId>/p#-s#.m4a
 * Story gift:   pack.json (story + sender) + recordings/<storyId>/p#-s#.m4a
 *
 * Import detects which kind it is. Everything goes through the system
 * document picker / share sheet, so no storage permissions are needed.
 */
object Backup {

    const val SUGGESTED_FILE_NAME = "bedtimecast-backup.zip"
    private const val BACKUP_MANIFEST = "backup.json"
    private const val PACK_MANIFEST = "pack.json"
    private const val RECORDINGS_PREFIX = "recordings/"

    @Serializable
    data class Manifest(
        val version: Int = 1,
        val profiles: List<VoiceProfile>,
        val activeProfileId: String?,
    )

    @Serializable
    data class PackManifest(
        val version: Int = 1,
        val storyId: String,
        val storyTitle: String,
        val senderName: String,
        val senderEmoji: String,
    )

    data class Summary(val profiles: Int, val recordings: Int)

    private val json = Json { ignoreUnknownKeys = true }

    fun export(context: Context, uri: Uri): Result<Summary> = runCatching {
        val recordingsRoot = File(context.filesDir, "recordings")
        var recordingCount = 0
        val output = context.contentResolver.openOutputStream(uri)
            ?: error("Could not open the selected location for writing.")
        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(BACKUP_MANIFEST))
                val manifest = Manifest(
                    profiles = ProfilesStore.profiles.toList(),
                    activeProfileId = ProfilesStore.activeProfileId,
                )
                zip.write(json.encodeToString(manifest).toByteArray())
                zip.closeEntry()

                if (recordingsRoot.isDirectory) {
                    recordingsRoot.walkTopDown().filter { it.isFile }.forEach { file ->
                        val rel = file.relativeTo(context.filesDir).invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry(rel))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        recordingCount++
                    }
                }
            }
        }
        Summary(profiles = ProfilesStore.profiles.size, recordings = recordingCount)
    }

    /** Packs one story's recordings by one profile into a shareable gift file. */
    fun exportPack(
        context: Context,
        profileId: String,
        story: Story,
        senderName: String,
        senderEmoji: String,
    ): Result<File> = runCatching {
        val sourceDir = File(File(File(context.filesDir, "recordings"), profileId), story.id)
        val files = sourceDir.listFiles()?.filter { it.isFile }.orEmpty()
        if (files.isEmpty()) {
            error("No recorded lines to share yet — record some of this story first.")
        }
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val packFile = File(sharedDir, "${story.id}-story-gift.zip")
        ZipOutputStream(packFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(PACK_MANIFEST))
            val manifest = PackManifest(
                storyId = story.id,
                storyTitle = story.title,
                senderName = senderName,
                senderEmoji = senderEmoji,
            )
            zip.write(json.encodeToString(manifest).toByteArray())
            zip.closeEntry()
            files.forEach { file ->
                zip.putNextEntry(ZipEntry("$RECORDINGS_PREFIX${story.id}/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        packFile
    }

    /** Restores a full backup or imports a story gift, whichever the file is. */
    fun import(context: Context, uri: Uri): Result<Summary> = runCatching {
        val temp = File.createTempFile("import", ".zip", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Could not open the backup file.")
            input.use { raw ->
                temp.outputStream().use { raw.copyTo(it) }
            }
            ZipFile(temp).use { zip ->
                val backupEntry = zip.getEntry(BACKUP_MANIFEST)
                val packEntry = zip.getEntry(PACK_MANIFEST)
                when {
                    backupEntry != null -> importBackup(context, zip, backupEntry)
                    packEntry != null -> importPack(context, zip, packEntry)
                    else -> error("This file is not a BedtimeCast backup or story gift.")
                }
            }
        } finally {
            temp.delete()
        }
    }

    private fun importBackup(context: Context, zip: ZipFile, manifestEntry: ZipEntry): Summary {
        val manifest = json.decodeFromString<Manifest>(
            zip.getInputStream(manifestEntry).readBytes().decodeToString(),
        )
        val filesRoot = context.filesDir.canonicalPath + File.separator
        var recordingCount = 0
        for (entry in zip.entries()) {
            if (entry.isDirectory || !entry.name.startsWith(RECORDINGS_PREFIX)) continue
            val dest = File(context.filesDir, entry.name)
            if (!dest.canonicalPath.startsWith(filesRoot)) {
                error("Backup contains an invalid file path.")
            }
            dest.parentFile?.mkdirs()
            zip.getInputStream(entry).use { stream ->
                dest.outputStream().use { stream.copyTo(it) }
            }
            recordingCount++
        }
        ProfilesStore.mergeFrom(manifest.profiles, manifest.activeProfileId)
        return Summary(profiles = manifest.profiles.size, recordings = recordingCount)
    }

    private fun importPack(context: Context, zip: ZipFile, manifestEntry: ZipEntry): Summary {
        val manifest = json.decodeFromString<PackManifest>(
            zip.getInputStream(manifestEntry).readBytes().decodeToString(),
        )
        // Recordings land under a profile named after the sender — reused if
        // a profile with that name already exists.
        val profile = ProfilesStore.profiles.firstOrNull {
            it.name.equals(manifest.senderName, ignoreCase = true)
        } ?: ProfilesStore.add(manifest.senderName, manifest.senderEmoji.ifBlank { "🎁" })

        val storyRoot = File(
            File(File(context.filesDir, "recordings"), profile.id),
            manifest.storyId,
        ).apply { mkdirs() }
        val canonicalRoot = storyRoot.canonicalPath + File.separator
        var recordingCount = 0
        val expectedPrefix = "$RECORDINGS_PREFIX${manifest.storyId}/"
        for (entry in zip.entries()) {
            if (entry.isDirectory || !entry.name.startsWith(expectedPrefix)) continue
            val dest = File(storyRoot, entry.name.removePrefix(expectedPrefix))
            if (!dest.canonicalPath.startsWith(canonicalRoot)) {
                error("Story gift contains an invalid file path.")
            }
            zip.getInputStream(entry).use { stream ->
                dest.outputStream().use { stream.copyTo(it) }
            }
            recordingCount++
        }
        return Summary(profiles = 1, recordings = recordingCount)
    }
}
