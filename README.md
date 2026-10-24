# ReelVault

ReelVault is an Android app that keeps the short videos you save on TikTok, Instagram, YouTube Shorts,
Facebook, X, Pinterest and Xiaohongshu (RED) in one place. Share a video to ReelVault from any app and
it shows up as a preview card. Tap the card and the video opens in its own app.

Your vault lives on the device and works fully offline. Sign in to back it up, sync between devices
and share collections as links.

<!-- Screenshots: add images to docs/screenshots/ and reference them here. -->
| Home | Item | Quick save | Shared collection |
|---|---|---|---|
| _screenshot_ | _screenshot_ | _screenshot_ | _screenshot_ |

## Features

- **Save from anywhere.** ReelVault appears in every app's share sheet. A small sheet saves the link
  without leaving the app you're in, and lets you drop it into a collection. You can also paste a link,
  and Home offers to save a supported link it finds on your clipboard.
- **Clean links, no duplicates.** Short links (`vm.tiktok.com`, `youtu.be`, `pin.it`, `xhslink.com`,
  `fb.watch`, `t.co`, …) are resolved, tracking parameters are removed, and saving the same video
  twice tells you it's already there.
- **Rich previews.** Thumbnail, title or caption, author and platform badge, fetched per platform
  (see [How previews work](#how-previews-work)). Thumbnails are cached on the device because some
  platforms' image links expire. If nothing can be fetched you get a styled card in the platform's
  colors, never a broken image.
- **Opens in the right app.** The platform's app if it's installed, otherwise a Custom Tab.
- **Collections.** Name, emoji, color and cover; one save can be in several. Reorder them.
- **Search as you type** across titles, captions, authors, tags, notes and links, with platform,
  collection and tag filters.
- **Auto-tagging on the device, free.** Hashtags become tags; editable keyword rules map saves to
  categories (Food, Fitness, Travel, Fashion, Tech, Music, Comedy, DIY, Beauty, Education, Finance);
  optional ML Kit image labeling on thumbnails feeds the same rules. Add, rename and remove tags yourself.
- **Notes, favorites, pins, sorting** (newest, oldest, platform, A–Z), grid or list view, swipe to
  delete with Undo, and multi-select to move, tag or delete in bulk.
- **Sync** through Firebase (Google account, or anonymous and upgradeable to Google later).
- **Share** a single video, or a whole collection as a link to a web page anyone can open. With
  ReelVault installed, the link opens in the app with **Add to my vault**.
- **Export and import** your vault as a JSON file.
- Light, dark or system theme; optional Material You wallpaper colors; respects the system's
  "remove animations" setting.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · MVVM with `data` / `domain` / `ui` layers · Coroutines & Flow ·
Hilt · Navigation Compose (type-safe routes) · Room with FTS4 (single source of truth) · WorkManager ·
OkHttp, Retrofit, kotlinx.serialization, Jsoup · Coil 3 · ML Kit image labeling · Firebase Auth,
Cloud Firestore and Hosting · Credential Manager · JUnit, Robolectric, MockWebServer.

Min SDK 26, target SDK 37. Versions are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Build

Requirements: Android Studio (Narwhal or newer) or JDK 21 and the Android SDK (platform 37).

```bash
git clone https://github.com/wilymanhoward/ReelVault.git
cd ReelVault
./gradlew assembleDebug testDebugUnitTest
```

The app builds and runs **without any Firebase setup**: everything works offline and Settings says
sync isn't set up. Add Firebase (below) for sign-in, sync and collection links.

Optional, in `local.properties` (never committed):

```properties
# Meta app token ("appId|clientToken") for Instagram/Facebook oEmbed. Without it, Open Graph tags are used.
meta.oembedToken=
```

## Firebase setup

1. **Create a project** at [console.firebase.google.com](https://console.firebase.google.com). Its
   project ID becomes your share-link domain: `https://<project-id>.web.app`.
2. **Add an Android app** with package name `io.github.wilymanhoward.reelvault` (or your own, if you
   changed `applicationId`). Add your signing key's **SHA-1** (required for Google sign-in) and
   **SHA-256**. For the debug key:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
3. **Download `google-services.json`** into `app/`. It's gitignored; see
   [`app/google-services.json.example`](app/google-services.json.example) for its shape. The build
   picks it up automatically and reads the share domain from it.
4. **Authentication → Sign-in method:** enable **Google** and **Anonymous**.
5. **Firestore Database → Create database** in production mode.
6. **Put your SHA-256 fingerprints in
   [`web/public/.well-known/assetlinks.json`](web/public/.well-known/assetlinks.json)** (debug and
   release) so Android verifies the share links for your app.
7. **Deploy the rules and the share page** with the [Firebase CLI](https://firebase.google.com/docs/cli):
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase deploy --only firestore,hosting --project <project-id>
   ```
8. Reinstall the app and check the link verification:
   ```bash
   adb shell pm get-app-links io.github.wilymanhoward.reelvault
   ```
   It should say `<project-id>.web.app: verified`.

### Data layout

| Path | Contents | Access ([`firestore.rules`](firestore.rules)) |
|---|---|---|
| `users/{uid}/items/{itemId}` | a save, with its tags and collection ids | owner only |
| `users/{uid}/collections/{id}` | a collection | owner only |
| `shared/{shareId}` | public snapshot of a shared collection | anyone reads; owner writes |

Sync is last-write-wins on each record's `updatedAt`, with soft deletes. A server timestamp is the
pull cursor, so devices with a wrong clock can't hide changes from each other.

## How previews work

Each platform has its own strategy (`NetworkPreviewResolver`), checked against the live endpoints:

| Platform | Strategy |
|---|---|
| TikTok | oEmbed, then Open Graph. TikTok's oEmbed currently returns only the player, so title, author and image come from the page's Open Graph tags. |
| YouTube / Shorts | oEmbed for title and author; thumbnail from `img.youtube.com/vi/{id}/hqdefault.jpg` |
| X | oEmbed at `publish.x.com` for author and text; Open Graph for the image |
| Instagram, Facebook | Open Graph; Meta oEmbed when `meta.oembedToken` is set |
| Pinterest, RED, anything else | Open Graph / Twitter card |

Endpoints change. If previews for a platform stop working, start in
[`NetworkPreviewResolver.kt`](app/src/main/java/io/github/wilymanhoward/reelvault/data/remote/NetworkPreviewResolver.kt).

## Release builds

Create a release keystore (keep it out of the repo; `*.jks` and `keystore.properties` are gitignored),
add its SHA-1 and SHA-256 to the Firebase app, and add the SHA-256 to `assetlinks.json` before
deploying Hosting again. Otherwise Google sign-in and share links only work in debug builds.

## Project layout

```
app/src/main/java/io/github/wilymanhoward/reelvault/
  domain/   models, repository interfaces, link parsing, auto-tagger, save use case
  data/     Room, network previews, thumbnails, sync, sharing, backup, settings
  ui/       Compose screens, theme, shared components
web/public/  Firebase Hosting site for shared collections
```

## Contributing

Issues and pull requests are welcome. CI builds the app and runs the unit tests on every push and
pull request; please run `./gradlew testDebugUnitTest` before opening one.

## License

[MIT](LICENSE). The app's typefaces, [Inter](licenses/Inter-OFL.txt) and
[Source Serif 4](licenses/SourceSerif4-OFL.txt), are under the SIL Open Font License 1.1.
ReelVault isn't affiliated with any of the platforms it saves links from.
