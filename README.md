# StoryTime 📖🌙

A bedtime story app where kids hear stories **read in their parent's voice** — even when the parent can't be there. Parents record themselves reading public domain children's stories, and (in a future phase) an optional voice model fills in any story they haven't recorded yet.

## How it works

Every story page has one audio slot per voice profile ("Mom", "Dad", "Grandma"). The slot is filled by, in order of preference:

1. **❤️ A real recording** — the parent read this page aloud in the app's teleprompter. Always preferred.
2. **✨ Cloned narration** *(planned)* — synthesized in the parent's voice by a TTS voice model, trained from the recordings they've already made. Clearly labeled as the AI voice.
3. **Nothing yet** — the page shows as recordable, with a gentle prompt to record it.

Real recordings and cloned audio share the same playback pipeline, so recording a story is never wasted work — and every recording doubles as training data for the voice model later.

## Current features (v1 scaffold)

- 📚 **Story library** — curated public domain stories (Aesop, the Brothers' classics, Beatrix Potter), stored as structured JSON pages.
- 🎙 **Guided recording** — a teleprompter shows one page at a time; record, listen back, and redo individual pages without re-reading the whole story.
- ▶️ **Story player** — big friendly pages with auto-advancing narration, page dots, and source labels ("❤️ Read by Mom").
- 👨‍👩‍👧 **Voice profiles** — any loved one can record; each profile keeps its own set of recordings.
- 📱 **Local-first** — all audio stays on the device. Cloud sync and remote sharing are planned, cloning is opt-in-by-design.

## Getting started

```bash
npm install
npx expo start
```

Then open the app in [Expo Go](https://expo.dev/go) on a phone (recording needs a real microphone — the iOS simulator works too). Useful scripts:

```bash
npm run typecheck   # TypeScript check
npm run lint        # ESLint via expo lint
```

## Project layout

```
src/
  app/              # expo-router screens
    index.tsx       #   library (home)
    story/[id].tsx  #   playback
    record/[id].tsx #   recording teleprompter
    profiles.tsx    #   voice profiles (modal)
  content/stories/  # public domain stories as structured JSON
  lib/
    stories.ts      # story registry
    narration.ts    # audio slot resolution: recorded > cloned > none
    profiles.tsx    # voice profile store (AsyncStorage)
  types/story.ts    # shared types
```

Recordings are saved under the app's documents directory: `recordings/<profileId>/<storyId>/page-<n>.m4a`.

## Roadmap

- **v1** — recording + playback (this scaffold), more curated stories, recording quality nudges.
- **v1.5** — voice cloning: consent flow, server-side TTS for unrecorded stories, ✨ source labeling, "delete my voice model" control.
- **v2** — cloud sync (record on Grandma's phone, play on the kid's tablet), blended stories (real pages + cloned pages), bedtime mode with sleep timer, sentence-level read-along highlighting.

See [docs/DESIGN.md](docs/DESIGN.md) for the full product and architecture plan.

## Content licensing

All bundled stories are in the public domain. Texts are lightly retold/abridged for read-aloud pacing; each story's `source` field notes its origin.
