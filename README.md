# BedtimeCast 🎙🌙

A **native Android app** (Kotlin + Jetpack Compose) where kids hear bedtime stories **read in their parent's voice** — even when the parent can't be there. Parents record themselves reading public domain children's stories line-by-line, performing each character, and (in a future phase) optional voice models fill in anything they haven't recorded.

## How it works

Stories are structured as scripts: every page is a sequence of segments spoken by the narrator or a named character. Each segment has one audio slot per voice profile ("Mom", "Dad", "Grandma"), filled by, in order of preference:

1. **❤️ A real recording** — the parent performed this line in the app's teleprompter. Always preferred.
2. **✨ Cloned narration** *(planned)* — synthesized by a voice model trained from the recordings they've already made. Clearly labeled as the AI voice.
3. **Nothing yet** — the line shows as recordable.

Every recorded character line is a labeled (audio + exact text + character) sample — so performing stories quietly builds the training library for future character voice models. No separate "calibration" session, ever.

## Features

- 📚 **Story library** — 17 public domain stories (Beatrix Potter, Aesop, Grimm, Andersen, Mark Twain, and Philippine folk tales including Visayan) with 61 performable character roles, bundled as structured JSON.
- 🎙 **Guided recording** — a line-by-line teleprompter cues who's speaking and how ("🐺 The Big Bad Wolf — perform it in a big, gruff, growly voice!"); record, listen back, and redo individual lines.
- 🎭 **Voice cast** — tracks every character a profile has performed and its sample coverage, ready for future voice modeling so "Papa's Wolf" can be cast in new stories.
- ▶️ **Story player** — segment-by-segment narration stitched seamlessly, character-colored dialogue, auto page turns.
- 👨‍👩‍👧 **Voice profiles** — any loved one can record; each keeps its own recordings and cast.
- ✨ **Voice models (ElevenLabs)** — with an API key and the voice owner's consent, recorded samples become voice models that read unrecorded lines, clearly labeled ✨; a casting screen assigns character voices to roles, and one button deletes all models everywhere.
- 📱 **Local-first** — recordings stay on the device; only voice-model creation uploads samples, opt-in, to ElevenLabs. No accounts, no ads, no analytics.

## Getting started

1. Open this repo folder in [Android Studio](https://developer.android.com/studio) (File → Open).
2. Let Gradle sync (first sync downloads dependencies).
3. Pick an emulator or plug in a phone with USB debugging, and press **Run ▶**.

Build an installable release APK:

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release-unsigned.apk
```

(For sideloading, configure a signing key in Android Studio via Build → Generate Signed App Bundle/APK, or add a signingConfig — debug builds from **Run ▶** install directly without any of that.)

## Project layout

```
app/src/main/
  java/com/robbstacy/bedtimecast/
    MainActivity.kt          # activity + navigation
    data/Models.kt           # Story, segments, characters, profiles, cast
    data/StoryRepository.kt  # loads bundled stories from assets
    data/ProfilesStore.kt    # voice profiles (SharedPreferences)
    data/Narration.kt        # segment audio slots + voice cast coverage
    audio/SegmentRecorder.kt # MediaRecorder (AAC .m4a per line)
    audio/SegmentPlayer.kt   # MediaPlayer with completion chaining
    ui/                      # Compose screens: Library, Story, Record, Cast, Profiles
    ui/theme/Theme.kt        # warm bedtime palette, light + dark
  assets/stories/*.json      # public domain stories, segmented by speaker
  res/                       # launcher icons (adaptive + themed)
```

Recordings are saved under the app's private storage: `filesDir/recordings/<profileId>/<storyId>/p<page>-s<segment>.m4a`.

## Roadmap

- **v1** — line-by-line recording + playback (current), more stories with original public domain illustrations, recording quality nudges, bedtime mode.
- **v1.5** — voice cloning: consent flow, TTS for unrecorded lines, ✨ source labeling, deletable voice models.
- **v2** — character voice models cast into new stories; sync so Grandma records remotely; read-along highlighting.

See [docs/DESIGN.md](docs/DESIGN.md) for the full product and architecture plan. The earlier React Native/Expo implementation is preserved on the `react-native-version` branch.

## Content licensing

All bundled stories are in the public domain. Texts are lightly retold/abridged for read-aloud pacing; each story's `source` field notes its origin.
