package com.robbstacy.bedtimecast.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small app-wide preferences: kid mode, favorites, resume positions. */
object AppPrefs {

    private const val PREFS_NAME = "bedtimecast.prefs"
    private lateinit var prefs: SharedPreferences

    /** When on, recording and settings are locked behind the grown-up gate. */
    var kidMode by mutableStateOf(false)
        private set

    val favorites = mutableStateListOf<String>()

    var lastStoryId by mutableStateOf<String?>(null)
        private set

    var elevenLabsKey by mutableStateOf("")
        private set

    private val resumePages = LinkedHashMap<String, Int>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        kidMode = prefs.getBoolean("kidMode", false)
        favorites.clear()
        favorites.addAll(prefs.getStringSet("favorites", emptySet()).orEmpty())
        lastStoryId = prefs.getString("lastStoryId", null)
        elevenLabsKey = prefs.getString("elevenLabsKey", "").orEmpty()
        prefs.getString("resume", null)?.split(';')?.forEach { pair ->
            val split = pair.lastIndexOf('=')
            if (split > 0) {
                val page = pair.substring(split + 1).toIntOrNull() ?: return@forEach
                resumePages[pair.substring(0, split)] = page
            }
        }
    }

    fun updateKidMode(on: Boolean) {
        kidMode = on
        prefs.edit().putBoolean("kidMode", on).apply()
    }

    fun updateElevenLabsKey(key: String) {
        elevenLabsKey = key.trim()
        prefs.edit().putString("elevenLabsKey", elevenLabsKey).apply()
    }

    fun isFavorite(storyId: String): Boolean = favorites.contains(storyId)

    fun toggleFavorite(storyId: String) {
        if (!favorites.remove(storyId)) {
            favorites.add(storyId)
        }
        prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
    }

    fun saveResume(storyId: String, pageIndex: Int) {
        resumePages[storyId] = pageIndex
        lastStoryId = storyId
        prefs.edit()
            .putString("lastStoryId", storyId)
            .putString("resume", resumePages.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }

    fun resumePage(storyId: String): Int = resumePages[storyId] ?: 0
}
