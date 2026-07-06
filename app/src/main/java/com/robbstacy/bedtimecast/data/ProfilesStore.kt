package com.robbstacy.bedtimecast.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ProfilesStore {

    private const val PREFS_NAME = "bedtimecast"
    private const val KEY = "profiles.v1"

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var prefs: SharedPreferences

    val profiles = mutableStateListOf<VoiceProfile>()

    var activeProfileId by mutableStateOf<String?>(null)
        private set

    val activeProfile: VoiceProfile?
        get() = profiles.find { it.id == activeProfileId }

    @Serializable
    private data class Persisted(val profiles: List<VoiceProfile>, val activeProfileId: String?)

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return
        val persisted = runCatching { json.decodeFromString<Persisted>(raw) }.getOrNull() ?: return
        profiles.clear()
        profiles.addAll(persisted.profiles)
        activeProfileId = persisted.activeProfileId
    }

    fun add(name: String, emoji: String): VoiceProfile {
        val profile = VoiceProfile(
            id = "p-" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            emoji = emoji,
            createdAt = System.currentTimeMillis(),
        )
        profiles.add(profile)
        if (activeProfileId == null) {
            activeProfileId = profile.id
        }
        persist()
        return profile
    }

    fun remove(id: String) {
        profiles.removeAll { it.id == id }
        if (activeProfileId == id) {
            activeProfileId = profiles.firstOrNull()?.id
        }
        persist()
    }

    fun setActive(id: String) {
        activeProfileId = id
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY, json.encodeToString(Persisted(profiles.toList(), activeProfileId)))
            .apply()
    }
}
