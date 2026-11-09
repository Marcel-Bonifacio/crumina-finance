# Third-party notices

Crumina is free software licensed under the GNU Affero General Public License v3.0;
see [LICENSE](LICENSE). It includes, depends on, or connects to the third-party
components and services listed below. Each of them is used under its own licence or
terms, and Crumina's licence does not change those terms.

The Apache License 2.0, which covers most of the components below, is reproduced in
full in [app/vendor/tesseract/LICENSE](app/vendor/tesseract/LICENSE) and at
https://www.apache.org/licenses/LICENSE-2.0.

Contents:

1. [Web app: files served to the browser](#1-web-app-files-served-to-the-browser)
2. [Android app (APK)](#2-android-app-apk)
3. [Server: npm packages](#3-server-npm-packages)
4. [Fonts](#4-fonts)
5. [External services and data](#5-external-services-and-data)
6. [Trademarks](#6-trademarks)
7. [Pending owner decision: Google ML Kit in the AGPL-3.0 Android app](#7-pending-owner-decision-google-ml-kit-in-the-agpl-30-android-app)

## 1. Web app: files served to the browser

These files are committed in [`app/vendor/tesseract/`](app/vendor/tesseract/) and
served to every browser that scans a receipt. They are unmodified upstream files. Their
licence files and notices sit next to them; the folder's
[README](app/vendor/tesseract/README.md) records versions, checksums and the full
licence texts of the packages bundled in the JavaScript files.

| Component | Version | Licence | Copyright / source |
|---|---|---|---|
| tesseract.js (`tesseract.min.js`, `worker.min.js`) | 5.1.1 | Apache-2.0 | https://github.com/naptha/tesseract.js |
| regenerator-runtime (bundled in both files above) | as bundled by tesseract.js 5.1.1 | MIT | Copyright (c) 2014-present, Facebook, Inc. |
| buffer (bundled in `worker.min.js`) | as bundled by tesseract.js 5.1.1 | MIT | Copyright (c) Feross Aboukhadijeh, and other contributors; parts Copyright Node.js contributors |
| ieee754 (bundled in `worker.min.js`) | as bundled by tesseract.js 5.1.1 | BSD-3-Clause | Copyright 2008 Fair Oaks Labs, Inc. |
| zlib.js (bundled in `worker.min.js`) | as bundled by tesseract.js 5.1.1 | MIT | Copyright (c) 2012 imaya |
| tesseract.js-core (the four `tesseract-core*.wasm.js` files) | 5.1.1 | Apache-2.0 | https://github.com/naptha/tesseract.js-core |
| Tesseract OCR, Balearica fork (compiled into the wasm core) | 5.3.0 | Apache-2.0 | https://github.com/Balearica/tesseract |
| Leptonica (compiled into the wasm core) | 1.83 | BSD-2-Clause style | Copyright (C) 2001-2020 Leptonica |
| libjpeg, Independent JPEG Group (compiled into the wasm core) | 9a | IJG licence | Copyright (C) 1991-2014, Thomas G. Lane, Guido Vollbeding |
| giflib (compiled into the wasm core) | 5.1.4 | MIT | Copyright (c) 1997 Eric S. Raymond |
| libpng (compiled into the wasm core) | 1.6.38 (git) | libpng License v2 | Copyright (c) 1995-2020 The PNG Reference Library Authors, and others |
| libtiff (compiled into the wasm core) | 4.3.0 | libtiff licence | Copyright (c) 1988-1997 Sam Leffler; Copyright (c) 1991-1997 Silicon Graphics, Inc. |
| libwebp (compiled into the wasm core) | 1.2.2 | BSD-3-Clause, plus the WebM patent grant | Copyright (c) 2010, Google Inc. |
| zlib (compiled into the wasm core) | 1.2.12 | Zlib | (C) 1995-2022 Jean-loup Gailly and Mark Adler |
| OpenLibm (compiled into the wasm core) | 0.8.0 | MIT, BSD-2-Clause, ISC and others | The Julia Project, The FreeBSD Project, Stephen L. Moshier, Sun Microsystems |
| English and Indonesian language data (`eng.traineddata.gz`, `ind.traineddata.gz`) | `@tesseract.js-data/eng` and `/ind` 1.0.0, folder `4.0.0` | Apache-2.0 | https://github.com/naptha/tessdata, from https://github.com/tesseract-ocr/tessdata |

This software is based in part on the work of the Independent JPEG Group.

The full copyright and licence notices of the libraries compiled into the wasm core,
taken from the exact submodule commits that tesseract.js-core v5.1.1 builds from, are in
[`app/vendor/tesseract/tesseract-core.wasm.js.LICENSE.txt`](app/vendor/tesseract/tesseract-core.wasm.js.LICENSE.txt).

## 2. Android app (APK)

Dependencies declared in [`android/app/build.gradle.kts`](android/app/build.gradle.kts):

| Library | Version | Licence |
|---|---|---|
| androidx.core:core-ktx | 1.13.1 | Apache-2.0 |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.2 | Apache-2.0 |
| androidx.activity:activity-compose | 1.9.0 | Apache-2.0 |
| Jetpack Compose: ui, ui-graphics, ui-tooling-preview, material3, material-icons-extended (ui-tooling in debug builds only) | set by compose-bom 2024.06.00 | Apache-2.0 |
| androidx.browser:browser | 1.8.0 | Apache-2.0 |
| androidx.security:security-crypto | 1.1.0-alpha06 | Apache-2.0 |
| com.squareup.retrofit2:retrofit and converter-gson | 2.11.0 | Apache-2.0 |
| com.squareup.okhttp3:okhttp | 4.12.0 | Apache-2.0 (see the Public Suffix List note below) |
| com.google.code.gson:gson | 2.10.1 | Apache-2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.8.1 | Apache-2.0 |
| Kotlin standard library | 1.9.24 | Apache-2.0 |
| com.google.mlkit:text-recognition | 16.0.1 | **Proprietary.** Google ML Kit, under the [ML Kit Terms of Service](https://developers.google.com/ml-kit/terms) and the [Google APIs Terms of Service](https://developers.google.com/terms). See section 7. |

These libraries also pull in transitive dependencies, among them other AndroidX
libraries, Okio, JetBrains annotations and Google Tink (all Apache-2.0), and, through
ML Kit, Google Play services, Firebase and Android data-transport client libraries,
which are under Google's own terms. Each library's Maven POM states its licence.

OkHttp includes `publicsuffixes.gz`, which is compiled from the Public Suffix List
(https://publicsuffix.org/list/public_suffix_list.dat). That list is subject to the
Mozilla Public License, v. 2.0 (https://mozilla.org/MPL/2.0/).

The release build keeps the licence and notice files that these libraries ship inside
their jars (see `packaging` in `android/app/build.gradle.kts`). The app does not yet
have an in-app "Open-source licences" screen; adding one (for example with the
AboutLibraries Gradle plugin or Google's oss-licenses plugin) is still to do.

## 3. Server: npm packages

The server code in `app/api` and `app/lib` uses the npm packages below, as resolved in
[`app/package-lock.json`](app/package-lock.json). They are installed from npm at deploy
time (`npm ci`), run only on the server, and are not stored in this repository. A Docker
image built from `selfhost/Dockerfile` contains them in `app/node_modules`, each with its
own licence file; keep those files if you distribute such an image.

Direct dependencies:

| Package | Version | Licence |
|---|---|---|
| pdfjs-dist | 4.10.38 | Apache-2.0 |
| imapflow | 1.4.2 | MIT |
| mailparser | 3.9.11 | MIT |

Transitive dependencies:

| Package | Version | Licence |
|---|---|---|
| @napi-rs/canvas, and the one platform build of it that npm installs (optional dependency of pdfjs-dist) | 0.1.100 | MIT |
| @pinojs/redact | 0.4.0 | MIT |
| @selderee/plugin-htmlparser2 | 0.12.0 | MIT |
| @zone-eu/mailsplit | 5.4.12 | MIT OR EUPL-1.1+ (used under MIT) |
| atomic-sleep | 1.0.0 | MIT |
| deepmerge-ts | 7.1.5 | BSD-3-Clause |
| dom-serializer | 2.0.0 | MIT |
| domelementtype | 2.3.0 | BSD-2-Clause |
| domhandler | 5.0.3 | BSD-2-Clause |
| domutils | 3.2.2 | BSD-2-Clause |
| encoding-japanese | 2.2.0 | MIT |
| entities | 4.5.0 and 7.0.1 | BSD-2-Clause |
| he | 1.2.0 | MIT |
| html-to-text | 10.0.0 | MIT |
| htmlparser2 | 10.1.0 | MIT |
| iconv-lite | 0.7.2 | MIT |
| ip-address | 10.2.0 | MIT |
| leac | 0.7.0 | MIT |
| libbase64 | 1.3.0 | MIT |
| libmime | 5.3.8 | MIT |
| libqp | 2.1.1 | MIT |
| linkify-it | 5.0.1 | MIT |
| nodemailer | 9.0.1 | MIT-0 |
| on-exit-leak-free | 2.1.2 | MIT |
| parseley | 0.13.1 | MIT |
| peberminta | 0.10.0 | MIT |
| pino | 10.3.1 | MIT |
| pino-abstract-transport | 3.0.0 | MIT |
| pino-std-serializers | 7.1.0 | MIT |
| process-warning | 5.0.0 | MIT |
| punycode.js | 2.3.1 | MIT |
| quick-format-unescaped | 4.0.4 | MIT |
| real-require | 0.2.0 and 1.0.0 | MIT |
| safe-stable-stringify | 2.5.0 | MIT |
| safer-buffer | 2.1.2 | MIT |
| selderee | 0.12.0 | MIT |
| smart-buffer | 4.2.0 | MIT |
| socks | 2.8.9 | MIT |
| sonic-boom | 4.2.1 | MIT |
| split2 | 4.2.0 | ISC |
| thread-stream | 4.2.0 | MIT |
| tlds | 1.261.0 | MIT |
| uc.micro | 2.1.0 | MIT |

Licences are taken from the `license` field that each package declares in
`app/package-lock.json`. Update this table when the lockfile changes.

## 4. Fonts

| Font | Licence | Copyright | Where it comes from |
|---|---|---|---|
| Plus Jakarta Sans | SIL Open Font License 1.1 | Copyright 2020 The Plus Jakarta Sans Project Authors (https://github.com/tokotype/PlusJakartaSans) | Loaded at runtime from Google Fonts (`fonts.googleapis.com`, `fonts.gstatic.com`) by the web pages. The font files are not in this repository. If you serve them yourself, include the font's `OFL.txt`. |

The Android app uses the device's system font.

## 5. External services and data

Crumina connects to these services. They are not part of Crumina and are used under the
provider's terms.

- **Google Sign-In (OAuth 2.0 / OpenID Connect) and the Gmail API** (`gmail.readonly`
  scope): [Google APIs Terms of Service](https://developers.google.com/terms) and the
  [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy),
  including its Limited Use requirements.
- **Google Fonts**: Google APIs Terms of Service (see section 4).
- **Google ML Kit** (Android app, on device): [ML Kit Terms of Service](https://developers.google.com/ml-kit/terms).
  Text recognition runs on the device, but ML Kit sends Google device and app
  information, a per-installation identifier, and performance and usage metrics. The
  ML Kit terms make the app developer responsible for telling users about this; the
  [privacy policy](app/privacy.html) does so.
- **Yahoo Finance**: `app/api/yf.js` calls Yahoo's `query1.finance.yahoo.com` search and
  chart endpoints for ticker search and prices. These are not a published, licensed API.
  Quotes and other data belong to Yahoo and its data providers and are subject to
  [Yahoo's terms](https://legal.yahoo.com/us/en/yahoo/terms/otos/index.html). If you run
  a public instance, check that your use is allowed.
- **ExchangeRate-API open access endpoint** (`open.er-api.com`): `app/api/fx.js` fetches
  exchange rates from it. Its [terms](https://www.exchangerate-api.com/docs/free) allow
  caching and personal or commercial currency conversion, require attribution on the
  pages that use the rates with a link to https://www.exchangerate-api.com (the app shows
  "Rates By Exchange Rate API"), and do not allow redistributing the data. Keep the
  attribution if you run your own instance.

## 6. Trademarks

Google, Gmail, Android, Google Play and the Google "G" logo shown on the sign-in buttons
are trademarks of Google LLC. Yahoo is a trademark of Yahoo. Bank and card names are
trademarks of their owners. They are used only to identify the services Crumina works
with; Crumina is not affiliated with or endorsed by any of them.

## 7. Pending owner decision: Google ML Kit in the AGPL-3.0 Android app

**Status: not adopted.** Only the copyright holder, Marcel-Bonifacio, can adopt the
additional permission drafted below. Until that happens, [LICENSE](LICENSE) (plain
AGPL-3.0, with no additional permissions) is the only licence for Crumina's own code,
and nothing in this section grants any permission.

### The issue

- The Android app links `com.google.mlkit:text-recognition:16.0.1`, used in
  `android/app/src/main/java/id/tirtawijata/crumina/data/Ocr.kt`. ML Kit and the Google
  Play services libraries it depends on are proprietary. They are licensed under the ML
  Kit Terms of Service, which forbid reverse engineering or extracting their source, and
  the Google APIs Terms of Service.
- AGPL-3.0 section 6 requires anyone who conveys the APK to provide its Corresponding
  Source, which section 1 defines to include libraries the program is designed to link
  with, apart from System Libraries. ML Kit is bundled inside the APK, so it is not a
  System Library, and its source is not available.
- The copyright holder can still publish the APK, because the AGPL's conditions bind
  licensees, not the licensor. Anyone else who redistributes the APK, or a build of a
  fork (a mirror, an F-Droid packager, a fork's own release), cannot meet section 6 and
  so has no right to convey it. F-Droid would also reject the app as non-free.
- All 27 commits in the repository are by the owner, so the owner alone can still add an
  exception. Once someone else's contribution is merged under plain AGPL-3.0, adding it
  would also need that contributor's agreement.

### Options

- **A. Keep ML Kit and add a linking exception** under AGPL-3.0 section 7, using the
  draft below.
- **B. Replace ML Kit** in `Ocr.kt` with an open-source on-device OCR engine, for
  example Tesseract4Android (Apache-2.0), which can reuse the `eng` and `ind` traineddata
  already in `app/vendor/tesseract`. No exception is then needed, the APK becomes fully
  free software, and the ML Kit metrics disclosure in the privacy policy can be removed.

### Draft additional permission (for option A)

This follows the Free Software Foundation's template for a GPLv3 section 7 linking
exception. The template's optional last sentence, which says the Corresponding Source
shall include the library's source code, is left out because ML Kit's source is not
available.

> **Additional permission under GNU AGPL version 3 section 7**
>
> If you modify this Program, or any covered work, by linking or combining it with the
> Google ML Kit libraries (Maven group `com.google.mlkit`) and the Google Play services,
> Firebase and Android data transport client libraries that they depend on (Maven groups
> `com.google.android.gms`, `com.google.firebase` and `com.google.android.datatransport`),
> or modified versions of those libraries, containing parts covered by the terms of the
> ML Kit Terms of Service or the Google APIs Terms of Service, the licensors of this
> Program grant you additional permission to convey the resulting work.

### How to adopt it

1. Put the text above, with a copyright line naming the copyright holder, in a new
   top-level file such as `LICENSE-EXCEPTION.md`. Do not edit `LICENSE`: the AGPL text
   itself may not be changed.
2. Refer to that file from the License section of `README.md`, and change
   `CONTRIBUTING.md` so that contributions are made under AGPL-3.0 together with this
   additional permission.
3. Optionally mention the exception next to `"license": "AGPL-3.0-only"` in
   `app/package.json`. The web app does not use ML Kit, so this is informational only.
4. Mark this section as adopted, or remove it and point to the new file.
