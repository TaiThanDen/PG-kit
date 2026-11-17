# Releasing Timecounter

Use JDK 17–21 and Android SDK 35. Increment `versionCode` for every distributed APK update and update `versionName` plus `CHANGELOG.md`.

## Signing

Keep the keystore, alias and passwords in a secure backup outside Git. Losing the release key prevents future in-place updates. Never upload the keystore or password files to GitHub Releases, issues or CI logs.

Set these environment variables in your local shell or CI secret store:

- `TIMECOUNTER_KEYSTORE`: absolute path to the release keystore.
- `TIMECOUNTER_STORE_PASSWORD`: keystore password.
- `TIMECOUNTER_KEY_ALIAS`: signing alias.
- `TIMECOUNTER_KEY_PASSWORD`: private key password.

Then build:

```bash
./gradlew clean assembleDebug testDebugUnitTest assembleRelease
```

Without a signing keystore, Gradle produces an unsigned release artifact. Never distribute that file as an installable release. Verify the actual release APK with the Android SDK build-tools `apksigner verify --verbose --print-certs` command. It must be signed with the release certificate and must not be debuggable. Compare the certificate fingerprint with the previous official release before publishing an update.

## Release checks

Install on a compatible test device without deleting valuable user data. A debug-to-release signature mismatch is expected; do not work around it by publishing with the debug key. Test capture, gallery, optional permissions and video audio. For a stable release, check preview and saved photo/video for all four physical orientations. Record unfinished checks as beta limitations.

Attach the signed APK and a `SHA256SUMS` text file to a GitHub Release for the source tag. Include changes, minimum Android version, install/update instructions, known issues and the signing certificate SHA-256 fingerprint. Use a prerelease for beta builds. Do not attach keys or raw device logs.
