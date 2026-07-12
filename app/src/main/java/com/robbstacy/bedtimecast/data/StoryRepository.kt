package com.robbstacy.bedtimecast.data

import android.content.Context
import kotlinx.serialization.json.Json

object StoryRepository {

    /** Library display order. */
    private val ORDER = listOf(
        "the-tale-of-peter-rabbit",
        "the-three-little-pigs",
        "goldilocks-and-the-three-bears",
        "the-three-billy-goats-gruff",
        "the-boy-who-cried-wolf",
        "the-little-red-hen",
        "the-gingerbread-man",
        "the-monkey-and-the-turtle",
        "why-the-sky-is-high",
        "the-legend-of-the-pineapple",
        "the-hawk-and-the-hen",
        "the-bremen-town-musicians",
        "the-elves-and-the-shoemaker",
        "the-frog-prince",
        "the-ugly-duckling",
        "the-princess-and-the-pea",
        "tom-sawyer-whitewashes-the-fence",
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<Story>? = null

    fun stories(context: Context): List<Story> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val loaded = ORDER.map { id ->
                context.assets.open("stories/$id.json").bufferedReader().use { reader ->
                    json.decodeFromString<Story>(reader.readText())
                }
            }
            cache = loaded
            return loaded
        }
    }

    fun story(context: Context, id: String): Story? = stories(context).find { it.id == id }
}
