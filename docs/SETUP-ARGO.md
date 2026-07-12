# Setting up Argo (replacement dev machine)

Checklist for getting BedtimeCast development running on Argo after Vroom's death. The good news: **CI builds every APK automatically**, so Argo needs very little — it does not have to be powerful.

## 0. Rescue from Vroom first (if the drive still works)

- [ ] **PuroVida source code** — `C:\Users\robb\Projects\PuroVida` was never pushed to GitHub; that folder is the ONLY copy. Pull Vroom's drive (or boot it once) and copy it out, then push it to the empty `robbstacy/PuroVida` repo.
- [ ] Anything else under `C:\Users\robb\Projects\`.
- [ ] Nothing BedtimeCast-related needs rescuing — everything is on GitHub.

## 1. Essentials (BedtimeCast needs only these)

- [ ] **Git** — install from https://git-scm.com (or `sudo apt install git` on Linux).
- [ ] Configure identity (this bit Vroom never got right 🙂):
  ```bash
  git config --global user.name "Robb Stacy"
  git config --global user.email "rstacy@gmail.com"
  ```
- [ ] **Clone the repository:**
  ```bash
  git clone https://github.com/robbstacy/StoryTime.git
  ```
  (No Git? A plain zip works too: https://github.com/robbstacy/StoryTime/archive/refs/heads/main.zip — but Git is strongly recommended.)
- [ ] Sign in to GitHub in the browser, so you can pull/push and download release APKs.

That's genuinely all: story editing, docs, and Claude sessions only need Git + a browser. Every push to `main` triggers GitHub Actions to build the APK and publish it at
https://github.com/robbstacy/StoryTime/releases/tag/latest — no local build machine required.

## 2. Optional — only if Argo should build/run the app locally

Skip this section if Argo is weak; the phone installs from the release link regardless.

- [ ] **Android Studio** — https://developer.android.com/studio (bundles the Android SDK and JDK). Wants ~8 GB RAM and ~15 GB disk; the emulator wants more. On a modest laptop, skip the emulator and Run ▶ straight to the phone over USB.
- [ ] Open the cloned repo root in Android Studio → let Gradle sync → Run ▶.
- [ ] Command-line alternative (no IDE, lighter): install JDK 17 + Android SDK command-line tools, then `./gradlew assembleDebug`.

## 3. Phone quick reference

- Install/update the app: open https://github.com/robbstacy/StoryTime/releases/tag/latest on the phone → download `app-debug.apk` → tap → install (updates go over the top).
- Back up recordings: in-app, Voices screen → 💾 Back up (save the .zip to Google Drive so a dead phone can't take the recordings with it).

## 4. Other projects

- [ ] **PuroVida**: after rescuing the source (step 0), from its folder:
  ```bash
  git init
  git add . && git commit -m "PuroVida cigar journal"
  git remote add origin https://github.com/robbstacy/PuroVida.git
  git branch -M main
  git push -u origin main
  ```
- [ ] MedCounter and other repos: just `git clone` them as needed.
