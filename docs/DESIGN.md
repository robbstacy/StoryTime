# StoryTime — Design & Planning

## Vision

Kids hear bedtime stories in the voice of someone who loves them, even when that person is traveling, working late, or far away. Parents (and grandparents, aunts, uncles…) record themselves reading public domain stories; an optional voice model extends their voice to the whole library.

## The hybrid narration model

The product combines two approaches, and they reinforce each other:

- **Option A — real recordings.** The app is a guided recording studio. A teleprompter shows the story page by page; the parent reads aloud; audio is saved per page. Authentic (giggles, character voices), private, works fully offline.
- **Option B — voice cloning.** From a few minutes of recorded speech, a TTS voice model can read *any* story in the library in the parent's voice.

**Key synergy: every real recording doubles as training data for the clone.** There is no sterile "read these calibration sentences" onboarding — the parent's first 2–3 recorded stories *are* the voice sample, and they deliver immediate value on their own.

### Narration resolution (per page, per voice profile)

| Priority | Source | UI label |
|---|---|---|
| 1 | Real recording | ❤️ Read by Mom |
| 2 | Cloned TTS (future) | ✨ Mom's voice |
| 3 | None | "Record this page" prompt |

Rules that follow from this:

- Real recordings always win over cloned audio.
- Sources are honestly labeled — parents should always know which is which.
- Stories can blend sources: if a parent recorded pages 1–8 and stopped, the clone finishes pages 9–12. A half-finished recording is never wasted effort.

## Product principles

1. **The recording UX is the product.** The biggest risk is parents not finishing recordings. Short stories first, per-page re-records, visible progress, encouragement.
2. **Local-first.** Bedtime happens in airplane mode. Audio lives on-device; the cloud is an optional sync layer, not a requirement.
3. **Consent and control for voices.** A voice belongs to its owner. Cloning (when added) requires explicit opt-in recorded on the owner's own device, and "delete my voice model" is one obvious button that actually deletes it, server-side included.
4. **Kid-safe by construction.** No ads, no feeds, no chat. Content is a hand-curated public domain library. Minimal data collection (COPPA applies).

## Architecture

- **Platform:** React Native + Expo (expo-router, expo-audio, expo-file-system).
- **Stories:** structured JSON — `{ id, title, author, year, source, pages: [{ text }] }`. This single decision enables the teleprompter, per-page audio slots, blending, read-along highlighting, and future TTS input.
- **Recordings:** `documents/recordings/<profileId>/<storyId>/page-<n>.m4a`. Existence of the file *is* the manifest for v1; a richer manifest (durations, cloned URIs, timings) comes with sync/cloning.
- **Profiles:** AsyncStorage. Cloud accounts arrive with sync in v2.
- **Cloning (v1.5, undecided):** hosted API (ElevenLabs — best quality, fastest, per-use cost, voice data on their servers) vs. self-hosted open models (XTTS/Fish Speech — data stays ours, GPU infra to run). Decision deferred until real recorded samples exist to evaluate quality with.

## Roadmap

### v1 — Recording + playback (current)
- [x] Story library with structured public domain stories
- [x] Voice profiles
- [x] Per-page guided recording with listen-back and redo
- [x] Player with auto-advance and source labels
- [ ] More curated stories (target ~15) with original public domain illustrations
- [ ] Recording quality nudges (background noise / clipping detection)
- [ ] Sentence-level timing captured during recording (enables read-along highlighting)

### v1.5 — The cloning leap
- [ ] TTS provider decision + integration
- [ ] Consent flow (owner opts in on their own device; deletable voice model)
- [ ] Cloned narration for unrecorded pages, ✨-labeled
- [ ] "Unlock the whole library by recording 3 stories" onboarding arc

### v2 — Together, apart
- [ ] Cloud sync (Supabase/Firebase): Grandma records on her phone, story appears on the kid's tablet
- [ ] Blended playback across recorded + cloned pages
- [ ] Bedtime mode: dimmed warm UI, sleep timer, end-of-story fadeout
- [ ] Read-along text highlighting synced to narration
