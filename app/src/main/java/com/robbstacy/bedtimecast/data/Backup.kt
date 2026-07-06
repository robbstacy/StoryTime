package com.robbstacy.bedtimecast.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One-file backup and restore: a .zip containing backup.json (profiles) and
 * every recording under recordings/<profileId>/<storyId>/p<page>-s<seg>.m4a.
 * Written and read through the system document picker, so no storage
 * permissions are needed and the user chooses where it lives.
 */
object Backup {

    const val SUGGESTED_FILE_NAME = "bedtimecast-backup.zip"
    private const val MANIFEST_ENTRY = "backup.json"
    private const val RECORDINGS_PREFIX = "recordings/"

    @Serializable
    data class Manifest(
        val version: Int = 1,
        val profiles: List<VoiceProfile>,
        val activeProfileId: String?,
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
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
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

    fun import(context: Context, uri: Uri): Result<Summary> = runCatching {
        var manifest: Manifest? = null
        var recordingCount = 0
        val filesRoot = context.filesDir.canonicalPath + File.separator
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Could not open the backup file.")
        input.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == MANIFEST_ENTRY -> {
                            manifest = json.decodeFromString<Manifest>(
                                zip.readBytes().decodeToString(),
                            )
                        }
                        name.startsWith(RECORDINGS_PREFIX) && !entry.isDirectory -> {
                            val dest = File(context.filesDir, name)
                            // Guard against zip entries escaping the app directory.
                            if (!dest.canonicalPath.startsWith(filesRoot)) {
                                error("Backup contains an invalid file path.")
                            }
                            dest.parentFile?.mkdirs()
                            dest.outputStream().use { zip.copyTo(it) }
                            recordingCount++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val restored = manifest ?: error("This file is not a BedtimeCast backup.")
        ProfilesStore.mergeFrom(restored.profiles, restored.activeProfileId)
        Summary(profiles = restored.profiles.size, recordings = recordingCount)
    }
}
