# Beta validation

## Completed

- Debug compilation and 28 JVM unit tests.
- LG V50S: installation, camera startup and a new real photo capture. The saved JPEG was opened directly and confirmed to contain the original Timemark wordmark/subtitle and reference verification label, rather than the custom launcher icon.
- Photo/video renderers both reference `video/official_vi.png`; launcher uses the separate custom icon.
- Existing production fonts, template definitions, camera behavior and media rendering are preserved.
- Signing material is stored locally and excluded from Git. The release APK is checked with Android build-tools before upload.

## Limits

The complete live-preview/JPEG/video matrix in portrait, upside-down and both landscape orientations has not been repeated for this beta. Hardware behavior varies. The beta label remains until broader testing is complete.

Existing lint findings are not hidden: the full lint task currently reports four annotation/constant errors and additional warnings. The automated release checks use compilation, unit tests, APK signature/manifest inspection and targeted device verification. This beta does not claim a clean lint result.

Raw device dumps, personal test media and signing keys are not release assets. See THIRD_PARTY_NOTICES for rights/provenance limits; a local photo ID and the reference-style verification label do not provide actual verification.
