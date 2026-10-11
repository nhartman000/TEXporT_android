<p align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="TExporT icon"></p>

<h1 align="center">TExporT for Android</h1>

<p align="center">
Select text in <b>any</b> app → <b>Mark with TExporT</b>.<br>
Collect the passages that matter from Claude, ChatGPT, Grok, Gemini or your browser,<br>
and export a handoff the next AI can't quietly lose.
</p>

<p align="center">Android 8.0+ · no permissions · works alongside the <a href="https://github.com/nhartman000/Texport-browser-extension">TExporT browser extension</a> and <a href="https://github.com/nhartman000/TEXporT">TExporT for Linux</a></p>

---

## Contents
- [Why an app?](#why-an-app)
- [Install](#install)
- [Mark text](#mark-text)
- [Collections and export](#collections-and-export)
- [Working with the browser extension](#working-with-the-browser-extension)
- [Settings](#settings)
- [Privacy](#privacy)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Project structure](#project-structure)
- [License](#license)

## Why an app?
On a phone, browsers don't let extensions add anything to the long-press menu. That menu belongs to Android, and Android only lets *apps* add items to it. That's how "Ask ChatGPT" and "Ask Grok" get there.

TExporT for Android adds **Mark with TExporT** to that same menu. It works in every app where you can select text, including the official Claude and ChatGPT apps, where no browser extension can run.

## Install
1. Download the latest `TExporT-android-<version>.apk` from [Releases](../../releases). For a build of the latest commit, go to **Actions** → newest run → **TExporT-android-apk**.
2. Open it on your phone. Android asks you to allow installs from your browser or file manager the first time. Allow it, then tap **Install**.
3. Open TExporT once. A short setup screen explains marking and asks whether you also use the browser extension.

A Google Play listing will come later. Until then the APK is signed with a development key, so updates install over it as long as they come from this repo.

## Mark text
1. In any app, **long-press** and drag the handles over the text you want.
2. In the menu that pops up, tap **Mark with TExporT**. If you don't see it, tap **⋮** (more) first.
3. A sheet slides up showing the text, where it came from (e.g. "From Claude") and its size in words and ≈tokens.
4. Optionally type a **note** saying why it matters, then pick a **collection** or create one with "＋ New collection…".
5. Tap **★ Save marker**. You stay in the app you came from, and the text there is not changed.

**Whole conversations.** Use the app's **Share** button and pick **TExporT**. The same sheet opens. Untick "Mark it" to save the text as context only, unmarked.

## Collections and export
Open TExporT to see your collections, with marker counts and sizes. Tap one to:
- **See every marker** (M1, M2…) with its source app, size and note. Tap a marker to **edit the note**, **delete** it or **copy** it.
- **Share to an AI app:** sends the whole handoff document as text to the Claude app, ChatGPT, Grok, email, anything.
- **Save .md:** saves the handoff as a Markdown file wherever you choose.
- **Rename** or **delete** the collection.

The handoff document is identical to the one the browser extension and the Linux app produce:
- reader instructions
- a register of every marked passage, at the start and repeated at the end
- the content with `[[MARK Mn START]] … [[MARK Mn END]]` tags

A unit test checks that the phone's output matches the extension's byte for byte. Snippets are grouped by source app ("From Claude", "From Firefox"), and each snippet is one fully marked message.

## Working with the browser extension
Turn on **I also use the TExporT browser extension** at first launch, or later in ⋮ → Settings. Then:
- **Phone → computer:** in a collection, tap **Send to browser (.texport.json)**, save the file somewhere you can reach from the computer (Drive, email, USB), then use **Import from Android…** on the extension's Collections page.
- **Computer → phone:** on the extension's export page, use **Send to Android (.texport.json)**. Get the file to the phone, then tap **Import from browser** on TExporT's main screen.

Turn on the matching switch in the extension: Settings → Companion apps → **I also use TExporT for Android**. The file format is documented in the extension's [EXPORT_FORMAT.md](https://github.com/nhartman000/Texport-browser-extension/blob/main/docs/EXPORT_FORMAT.md#collection-files-texportjson).

## Settings
⋮ → **Settings**:
| Setting | What it does |
|---|---|
| I also use the TExporT browser extension | Shows "Send to browser" and "Import from browser" |
| Measure markers in | tokens (≈ characters ÷ 4), words or characters |

⋮ → **Setup guide** shows the first-run screen again.

## Privacy
TExporT for Android requests **no permissions**, not even internet access, so it cannot send anything anywhere. It only ever sees text you explicitly mark or share to it. Everything is stored privately on the phone until you export it. Full policy: [PRIVACY.md](PRIVACY.md).

## Troubleshooting
| Problem | Fix |
|---|---|
| "Mark with TExporT" isn't in the long-press menu | Tap **⋮** in that menu; extra items are often hidden there. Some apps show only Copy and Share in their own custom menu. In that case use **Share → TExporT**. |
| It's still missing everywhere | Open TExporT once after installing. Some phones only list apps that have been opened at least once. |
| Android blocks the install | Allow "Install unknown apps" for the app you opened the APK from (Settings → Apps → that app). |
| Shared text is just a link | Some apps share a URL, not the text. Select and copy the text instead, then use Mark with TExporT, or paste it into a note. |

## Development
**Requirements:** JDK 17, Gradle 8.7+ and the Android SDK (platform 34). There are no third-party libraries; the app uses only the Android platform.

```bash
gradle testDebugUnitTest   # export format = browser extension (fixtures generated from its src/export.js)
gradle assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
```

On every push, GitHub Actions runs the tests, builds the APK and uploads it as an artifact. Each push to main also publishes the APK as the GitHub release for the current `versionName` (creating it on first build of a new version).

To regenerate the test fixtures after a deliberate format change, run the Node snippet in `docs/FIXTURES.md` against the extension repo.

## Project structure
```
app/src/main/
├── AndroidManifest.xml          PROCESS_TEXT + SEND intent filters, no permissions
├── java/com/americanmilestone/texport/
│   ├── MarkActivity.kt          "Mark with TExporT" sheet (selection menu + Share)
│   ├── MainActivity.kt          collections, first-run setup, settings, import
│   ├── CollectionActivity.kt    markers, notes, share/save Markdown, send to browser
│   ├── Exporter.kt              handoff document, a port of the extension's export.js
│   ├── Model.kt                 data model + .texport.json (same shape as the extension)
│   ├── Store.kt                 private JSON storage + settings
│   └── Ui.kt                    small view helpers (screens are built in code)
└── res/                         launcher icons, theme, strings
app/src/test/                    unit tests + fixtures generated by the browser extension
```

## License
Proprietary. © 2026 American Milestone Inc. All rights reserved. Free to install and use; see [LICENSE](LICENSE).
