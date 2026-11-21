# QuranApp – Refactored foundation

This update keeps the existing Quran reader, PDF asset, fonts, images and feature screens,
while introducing a clearer application structure:

- `core/`: app-wide constants.
- `data/`: persistence boundary (`AppPreferencesRepository`).
- `domain/`: destination/route contract.
- `presentation/`: app state holder (`AppViewModel`).
- Existing `MainActivity2.kt` … `MainActivity9.kt`: feature UI and platform helpers.

## State persistence
The theme, last-read page (1–604), and favorite pages are persisted in the app's private
preferences. Screen composables receive state and callbacks from the activity/view-model.

## Build and signing
Debug builds use Android's standard debug signing. No release keystore password is embedded
in Gradle. Configure release signing through local, untracked Gradle properties or CI secrets.

## Notes
- The old manifest referenced `PlayerService`, `AdhanAlarmReceiver`, `ReminderReceiver`
  and `BootReceiver`, but those classes were not present in the supplied source. Those stale
  declarations were removed to avoid manifest component resolution failures.
- Audio stream selection now maps each displayed reciter to a separate MP3Quran host path.
  Availability of individual remote streams still depends on the provider and network.
- This source package has not been compiled on an Android SDK in this environment. Run
  `./gradlew :app:assembleDebug` in Android Studio and resolve any environment-specific
  SDK/plugin setup before release.


## Quran text and tafsir providers (4.1)
The PDF renderer and bundled mushaf PDF are removed. The reader now requests Uthmani word text and page/line metadata from the Quran.com Content API, using the 15-line mushaf layout. The user taps a word to open the selected ayah's Tafsir Muyassar through QuranEnc. The app must retain source attribution and the provider's translation version.
