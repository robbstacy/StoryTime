package com.robbstacy.bedtimecast.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf

/**
 * Who reads which role in each story — the duet system. Maps
 * (storyId, speaker) to a profile id; unassigned roles resolve
 * automatically (active profile first, then anyone who recorded the line).
 */
object ReaderCast {

    private const val PREFS_NAME = "bedtimecast.readers"
    private const val KEY = "readers.v1"

    private lateinit var prefs: SharedPreferences
    private val assignments = mutableStateMapOf<String, String>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.split(';')?.forEach { pair ->
            val split = pair.lastIndexOf('=')
            if (split > 0) {
                assignments[pair.substring(0, split)] = pair.substring(split + 1)
            }
        }
    }

    private fun key(storyId: String, speaker: String) = "$storyId|$speaker"

    fun reader(storyId: String, speaker: String): String? = assignments[key(storyId, speaker)]

    fun assign(storyId: String, speaker: String, profileId: String?) {
        if (profileId == null) {
            assignments.remove(key(storyId, speaker))
        } else {
            assignments[key(storyId, speaker)] = profileId
        }
        prefs.edit()
            .putString(KEY, assignments.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }
}
