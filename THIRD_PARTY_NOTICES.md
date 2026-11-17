# Third-party notices and licensing scope

The root MIT license applies only to original project source and documentation. It does not grant rights to third-party fonts, artwork, trademarks, reference assets or quoted material.

## Existing fonts — preserved without replacement

License information was read from the exact bundled font files' OpenType name tables. Their binaries are unchanged.

| Files | Embedded attribution / license |
| --- | --- |
| `assets/fonts/BigShouldersText-Medium.ttf` | Copyright 2019 The Big Shoulders Project Authors; SIL OFL 1.1 |
| `assets/fonts/PTMono-Regular.ttf` | Copyright 2010 ParaType; SIL OFL 1.1, with reserved font names as stated in the bundled notice |
| `assets/fonts/RobotoCondensed-Regular.ttf`, `RobotoCondensed-Medium.ttf` | Copyright 2011 Google Inc.; Apache License 2.0 |
| `res/font/roboto_medium_numbers.ttf` | Copyright 2011 Google Inc.; reference-derived number font. The embedded copyright table does not itself establish the license of any modifications. |

Full font license texts are bundled in `app/src/main/assets/licenses/`. Big Shoulders license source: [official Google Fonts repository](https://github.com/google/fonts/tree/main/ofl/bigshoulderstext). The PT Mono notice is taken directly from its font metadata. Apache-2.0 text is sourced from the [AndroidX license](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt).

## Reference-derived assets

The following assets and reference-style content are intentionally preserved at the owner's request:

- `assets/video/official_vi.png` and `official_wm_new.webp`: original Timemark-style wordmark graphics.
- `assets/verification/anti_code_icon_small.webp` and `res/drawable/watermark_shield.webp`: reference verification graphics.
- `assets/watermarks/templates.json`: reference-derived template definitions.
- The reference-derived number font noted above and research material under `docs/timemark_reference`.

No separate redistribution license for these reference-derived graphics/catalogue/font modifications has been established by this project. They are not relicensed under MIT. Attribution is not permission, and public source access does not establish clearance. Anyone redistributing them must assess the applicable rights separately. Timemark names and marks belong to their respective owners; this project is independent and provides no official verification service.

## Custom app icon

The camera/clock icon was supplied by the repository owner. It is used only for application identification, not as the photo/video watermark. It is excluded from the MIT source-code grant; no separate artwork/trademark rights or independent clearance are asserted.

## Dependencies

AndroidX Activity, CameraX and ExifInterface are Android Open Source Project libraries under Apache-2.0. The APK includes the Apache-2.0 license text. JUnit 4 is a test-only dependency under EPL-1.0. Gradle is Apache-2.0 build tooling and retains its wrapper notice.

Upstream references: [AndroidX](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [JUnit](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt), [Gradle](https://github.com/gradle/gradle/blob/master/LICENSE).
