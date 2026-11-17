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

## Permanent download link

Share https://github.com/TaiThanDen/Timecounter/releases/download/download/timecounter.apk. The `download` release is a moving distribution alias, including beta versions; versioned release assets remain unchanged.

Upload the signed `timecounter-VERSION.apk` and its `SHA256SUMS` before publishing each `v*` release. The `Update permanent APK download` workflow verifies the checksum, copies the APK unchanged to `timecounter.apk`, and updates the alias checksum, `VERSION.txt` and release notes. Updates become available after that workflow succeeds. Source code and version-specific limitations are linked from the alias release notes.

To retry or initialize the alias, run the workflow manually with the published version tag:

```bash
gh workflow run download-link.yml -f source_tag=v1.0.0-beta.3
```

If a future release workflow publishes with `GITHUB_TOKEN`, explicitly dispatch or invoke this sync workflow there: events created by that token do not trigger another workflow automatically. The alias release must exist before syncing. Keep it a prerelease so it does not replace a stable version in GitHub's `latest` selection.
