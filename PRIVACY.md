# Privacy

Timecounter is a local camera application. This document describes the community beta, not Android services or apps you choose to share files with.

## Permissions

- **Camera:** live preview, taking photos and recording video.
- **Microphone:** audio during video recording. Recording can continue without audio when this permission is denied.
- **Precise/approximate location:** coordinates, altitude and an address label when a location fix and geocoder result are available.
- **Storage on Android 8–9:** saving and reading the app's media. Newer Android versions use MediaStore scoped storage.

## Data on your device

Photos and videos are written to shared media storage. Template settings, custom watermark clock settings and related preferences are stored locally. Android may back up app preferences according to your device/account settings because application backup is enabled.

The app does not operate an account system, analytics service, advertising service or remote capture archive. It does not upload captures to a project server. Android's location/geocoding implementation may contact its own service providers; behavior varies by device and system configuration.

Location can appear in burned-in watermark text, and saved JPEGs may retain GPS/camera metadata. Editing the visible address does not automatically remove embedded GPS metadata. A custom watermark time is not a trusted capture timestamp.

## Sharing and deletion

Sharing passes your selected files to another app via Android's share mechanism. That app's privacy policy then applies. Review visible content and metadata before sharing. Delete captures through the app or your system gallery. Clearing application storage removes local preferences; it does not necessarily delete files in shared media storage.

Do not post private photographs, GPS coordinates, signing credentials or device identifiers in public bug reports. Use fictional examples or redact them first.
