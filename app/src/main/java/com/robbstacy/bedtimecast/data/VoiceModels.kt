package com.robbstacy.bedtimecast.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A cloned voice built from a profile's recorded samples.
 * key is [VoiceModels.NARRATOR_KEY] or "<storyId>/<characterId>" for a
 * performed character voice (e.g. Papa's Big Bad Wolf).
 */
@Serializable
data class VoiceModel(
    val key: String,
    val profileId: String,
    val displayName: String,
    val elevenVoiceId: String,
    val createdAt: Long,
)

/** Registry of voice models, casting assignments, and cloning consent. */
object VoiceModels {

    const val NARRATOR_KEY = "narrator"

    private const val PREFS_NAME = "bedtimecast.voices"
    private const val KEY = "models.v1"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    val models = mutableStateListOf<VoiceModel>()

    /** "<profileId>|<storyId>|<characterId>" -> model key */
    private val castings = mutableStateMapOf<String, String>()
    private val consents = mutableStateListOf<String>()

    @Serializable
    private data class Persisted(
        val models: List<VoiceModel> = emptyList(),
        val castings: Map<String, String> = emptyMap(),
        val consents: List<String> = emptyList(),
    )

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return
        val persisted = runCatching { json.decodeFromString<Persisted>(raw) }.getOrNull() ?: return
        models.addAll(persisted.models)
        castings.putAll(persisted.castings)
        consents.addAll(persisted.consents)
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY, json.encodeToString(Persisted(models.toList(), castings.toMap(), consents.toList())))
            .apply()
    }

    fun hasConsent(profileId: String): Boolean = consents.contains(profileId)

    fun grantConsent(profileId: String) {
        if (!consents.contains(profileId)) {
            consents.add(profileId)
            persist()
        }
    }

    fun modelsFor(profileId: String): List<VoiceModel> = models.filter { it.profileId == profileId }

    fun model(profileId: String, key: String): VoiceModel? =
        models.firstOrNull { it.profileId == profileId && it.key == key }

    fun narrator(profileId: String): VoiceModel? = model(profileId, NARRATOR_KEY)

    fun add(model: VoiceModel) {
        models.removeAll { it.profileId == model.profileId && it.key == model.key }
        models.add(model)
        persist()
    }

    /** Clears everything locally; returns the remote voice ids to delete. */
    fun clearAll(): List<String> {
        val voiceIds = models.map { it.elevenVoiceId }
        models.clear()
        castings.clear()
        persist()
        return voiceIds
    }

    private fun castKey(profileId: String, storyId: String, characterId: String) =
        "$profileId|$storyId|$characterId"

    fun assignedKey(profileId: String, storyId: String, characterId: String): String? =
        castings[castKey(profileId, storyId, characterId)]

    fun assign(profileId: String, storyId: String, characterId: String, modelKey: String?) {
        if (modelKey == null) {
            castings.remove(castKey(profileId, storyId, characterId))
        } else {
            castings[castKey(profileId, storyId, characterId)] = modelKey
        }
        persist()
    }

    /** The voice to use for a character: its casting, else the narrator model. */
    fun resolve(profileId: String, storyId: String, characterId: String): VoiceModel? =
        assignedKey(profileId, storyId, characterId)?.let { model(profileId, it) }
            ?: narrator(profileId)
}
