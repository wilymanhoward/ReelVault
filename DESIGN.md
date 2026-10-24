# ReelVault design

## Android app: a personal film library

The user's latest direction is Once (Disposable Camera for Events) on iOS, refined to feel like a native Apple app. Adapt its serif typography, near-black surfaces, white actions and photo-led albums to ReelVault. The app remains Kotlin and Jetpack Compose, with original custom icons and ReelVault branding.

- Ground #101010, surface #191919, raised surface #262625, text and primary actions #F7F6F2, muted text #AAA7A1. Keep the palette independent of wallpaper colors.
- Inter for all interface text and Source Serif 4 for large titles: the closest openly licensed stand-ins for Apple's SF Pro and New York, which can't ship on Android. Sizes follow iOS Dynamic Type at its default (Large Title 34sp, Body 17sp, Footnote 13sp, tab labels 11sp). The serif keeps the personal-album feel; Inter keeps controls native and readable. Support system font scaling.
- Use the app's own geometric vectors in `VaultIcons.kt`. Navigation is a compact inset labeled bar with a muted selected surface. Keep destinations labeled and targets at least 44dp.
- Settings use iOS grouped lists: inset rounded groups, hairline separators, footnote-size headers and explanatory footers. Alerts, fields, menus and snackbars share the Vault shapes.
- Use white rounded primary actions, custom compact switches and muted chips. The full-width Save a video control opens a simple add-link sheet.
- Keep real video thumbnails, titles, authors and collection colors. Two-column portrait cards have restrained captions. Collection covers become image-led album tiles, with serif names and real save counts. Collection details use a photographic header when a cover exists.
- The off-white launcher mark combines a saved bookmark, a play cutout and a library spine on a near-black tile with a gentle top-to-bottom tone, rounded and optically centered inside the adaptive safe zone. Its editable source is `design/reelvault-icon.svg`; adaptive and monochrome vectors ship in Android resources.
- Default new installs to dark mode. Retain explicit light/system choices, accessible focus feedback, reduced motion, error states and the existing save/search/sync behavior. Preserve stored settings and data.

References:

- https://once.film/
- https://apps.apple.com/us/app/once-disposable-camera-event/id6754265953

The official gallery, creation and invitation screens informed the design. Reference photos, logos and event-camera functionality are not copied into ReelVault. The app uses original Kotlin components and vectors.

## Companion website

The website retains the earlier iOS-inspired neutral palette and system type. Keep availability claims accurate: Android, open source, and collection sharing requiring configured sync. The labeled collection example illustrates organization without storing user data. Real public collection pages render the shared snapshot.
