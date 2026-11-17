# Contributing

Please use English for issues, pull requests and public documentation. Include Android version, device model, steps to reproduce and expected/actual behavior. Use fictional locations and omit serial numbers, private media and credentials.

Before submitting a change:

```bash
./gradlew clean assembleDebug testDebugUnitTest
git diff --check
```

Keep commits focused and include a meaningful test for behavior changes. Do not commit APKs, build directories, device dumps, signing material or third-party resources without redistribution terms.

The Activity stays portrait. `DeviceOrientation` owns sensor mapping. Preview rotation follows the display; capture rotation follows physical orientation. Check live preview and saved media on a real device in portrait, upside down and both landscape directions when changing rotation or output rendering. Emulator rotation is not a substitute for sensor checks.

Use device wall time by default. Explicit user-selected clock values must remain distinguishable from trusted capture metadata. Photo codes are local identifiers; do not present them as server verification.

Preserve the reference-style watermark, fonts, templates and camera behavior unless the owner explicitly asks to change them. The custom app icon must not be burned into photos or video. Existing lint findings are tracked separately; do not suppress or change runtime behavior merely to claim a clean lint run.
