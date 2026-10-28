# Play Console answers: Tube Joy

Everything below matches what the game actually does (checked against the code). Answer exactly
this; if a page asks something not covered, stop and check first.

## Create app
- App name: **Tube Joy: Water Sort Puzzle**
- Default language: English (United States)
- App or game: **Game** · Free or paid: **Free**
- Declarations: accept only after reading (Developer Program Policies, US export laws).

## App content
| Section | Answer |
|---|---|
| Privacy policy | https://tubejoy-bahalabs.web.app/privacy.html |
| App access | All functionality is available without special access (no login) |
| Ads | **Yes, my app contains ads** |
| Content rating | IARC questionnaire, category **"Puzzle / casual game"**. Violence, fear, sexual content, language, controlled substances, gambling, crude humour: **No**. Users can interact or communicate: **No**. Shares location: **No**. Digital purchases: **No**. Expected: Everyone / PEGI 3 |
| Target audience | **13–15, 16–17, 18+** (do **not** pick under 13). "Appeals to children": **No** (ads are limited to PG, the policy says it is not for under-13s) |
| News app | No |
| COVID-19 apps | No |
| Data safety | see below |
| Advertising ID | **Yes**. Purposes: Advertising or marketing; Analytics; Fraud prevention, security and compliance |
| Government apps | No |
| Financial features | My app doesn't provide any financial features |
| Health apps | My app does not have any health features |

## Data safety
All of it comes from the Google Mobile Ads SDK; the game itself sends nothing.

- Collects or shares required data: **Yes**
- Encrypted in transit: **Yes**
- Account creation: **My app does not allow users to create an account**
- Data deletion request URL: not needed (no account); leave optional fields empty

| Data type | Collected | Shared | Ephemeral | Required | Purposes |
|---|---|---|---|---|---|
| Location → Approximate location | ✓ | ✓ | No | Required | Advertising or marketing; Analytics |
| App activity → App interactions | ✓ | ✓ | No | Required | Advertising or marketing; Analytics |
| App info and performance → Diagnostics | ✓ | ✓ | No | Required | Analytics; Fraud prevention, security, and compliance |
| Device or other IDs → Device or other IDs | ✓ | ✓ | No | Required | Advertising or marketing; Analytics; Fraud prevention, security, and compliance |

Nothing else (no name, email, photos, files, contacts, purchases).

## Store settings
- App category: **Puzzle**
- Tags (pick the closest available, max 5): Puzzle, Casual, Brain games / Logic, Relaxing / Offline
- Email: hamidakhan7654@gmail.com
- Website: https://tubejoy-bahalabs.web.app
- External marketing: on

## Main store listing
Texts in `store/listing.md` (English default + de, fr, es, pt-BR, ja translations).
Graphics in `store/graphics/`. AI asset declaration: **Don't label assets** (all art is drawn by
code from the game itself, no AI image generator).

## Production release
- Countries: all
- App bundle: `app/build/outputs/bundle/release/app-release.aab` (version 1, 1.0.0)
- Release name: 1.0.0
- Release notes (en-US): `First release of Tube Joy: water, ball and nut sort puzzles with unlimited levels and daily challenges.`
- Play App Signing: keep the default (Google holds the app signing key; ours is the upload key)
