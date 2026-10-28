# Releasing Tube Joy

## Files that never go into git
| File | What | Back it up? |
|---|---|---|
| `keystore.properties` | upload key passwords | **Yes**, together with the .jks |
| `keys/sortopia-upload.jks` | upload key (alias `sortopia-upload`) | **Yes**: losing it means asking Google for an upload-key reset |
| `admob.properties` | real AdMob ids (template: `admob.properties.example`) | optional |

Upload key fingerprints (for reference): SHA-1 `C9:30:64:7D:BF:91:31:18:09:69:6F:51:05:A0:8B:3B:09:23:8A:09`.

## AdMob (do this once, before the first bundle)
1. AdMob → Apps → **Add app** → Android → "Is the app listed on a supported app store?" **No** → name **Tube Joy**.
2. In that app: Ad units → create **Banner**, **Interstitial**, **Rewarded** (reward 1 × "Reward").
3. Put the app id and the three unit ids in `admob.properties`.
4. After the game is live on Play: open **this same app** in AdMob → App settings → **Add store** → Google Play.
   Never use "Add app" again for Tube Joy (that is how BlockFit ended up with a duplicate).

## Privacy policy hosting (Firebase, project quickscan-pro-shamol)
```
cd store
firebase hosting:sites:create tubejoy-bahalabs --project quickscan-pro-shamol
firebase deploy --only hosting --project quickscan-pro-shamol
```
Live at https://tubejoy-bahalabs.web.app/privacy.html (also serves `app-ads.txt`).

## Build the Play bundle
```
gradlew :engine:test :app:lintDebug :app:bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`. The build stops if `admob.properties` is missing.

Before every upload: raise `versionCode` (and `versionName`) in `app/build.gradle.kts`.

## Test on a phone
- Debug: `gradlew :app:installDebug` (package `com.bahalabs.tubejoy.debug`, always test ads).
- Open a level directly (debug only): `adb shell am start -n com.bahalabs.tubejoy.debug/com.bahalabs.sortopia.MainActivity --es route game/WATER/42`
- Release check: `gradlew :app:assembleRelease`, install the APK, play a level: R8 problems only show up here.

## Play Console progress (28 Oct 2026, app id 4975721330492027178)
Done: app created; privacy policy, ads, sign-in, target audience (13+), data safety, ad ID, government,
financial, health; store settings (Puzzle + 5 tags, contact); default listing texts, icon, feature
graphic and 7 screenshots in order 01→07; listing saved ("Ready to send for review") with de-DE, fr-FR,
es-ES, pt-BR, ja-JP translations; production countries = all 178; production release 1 draft opened.
Release 1.0.0 saved (owner uploaded the AAB; 3.9 MB download, no errors or warnings); content rating
done with the owner's consent to IARC terms (Everyone / PEGI 3 / USK 0 / 3+). Publishing overview shows
"Submit 17 changes for review", quick checks running. Left: Send for review (owner's OK).
After publish: AdMob → Tube Joy → Add store.

## Store
Listing texts and Play Console answers: `store/listing.md`, `store/play_console_answers.md`.
