# BedtimeCast — Project Guidance

## Content rules (set by Robb — always follow)

- **Story fidelity is the standard.** Classic stories must stay faithful to their original/traditional tellings. Do not change characters' genders, roles, family relationships, or story elements from the source material.
- **No DEI, LGBT, or identity-politics content** in story text, app copy, feature ideas, code comments, commit messages, or any other project output. Do not inject modern social or political themes into the classic stories or the app.
- Softening violence for bedtime suitability is acceptable (e.g., a comic ending instead of a grim one), and abridging for read-aloud pacing is fine — note the adaptation in the story's `source` field.
- When adding new stories: public domain only, retold close to the original text, bedtime-appropriate.

## Project facts

- Native Android app: Kotlin + Jetpack Compose, no React Native/Expo (the old RN implementation is on the `react-native-version` branch).
- Open the repo root in Android Studio to build; CI (`.github/workflows/build-apk.yml`) builds a debug APK on every push to `main` and publishes it to the rolling `latest` release for phone installs.
- App identity: BedtimeCast, `com.robbstacy.bedtimecast`. All builds signed with the committed `signing/shared-debug.keystore` so sideload updates install over the top.
- Voice cloning: ElevenLabs (user's API key, entered in-app). Consent dialog before any sample upload; ✨ labels on generated audio; delete-all-models button must keep working.
