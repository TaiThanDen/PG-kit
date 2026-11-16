# Timemark system-address formatting

Investigated 2026-11-16 against v10.0.316, base APK SHA-256
`b59c50b6c84ccfa180f8648bc6864a83040b61ff6fcd70eb7e092321042a1a98`.

## Cause of the difference

PG-kit previously used `Address.getAddressLine(0)` unchanged and forced the Android
geocoder locale to `vi_VN`. Timemark constructs `Geocoder(context)` with the default
locale and formats its system address before storing it in a `PlaceItem`. Its
watermark reads `PlaceItem.getAddress()`, rather than reconstructing a watermark
from the small/large position strings shown elsewhere in the app.

The retained original portrait capture contains
`234 Example Street, Example District, Hồ Chí Minh` without a country suffix.
This is a captured example, not a production constant or a fresh same-location test.

## REA evidence

All following bodies were complete Java decompiler output. Decompiled control flow
is static evidence and does not establish the active provider for a particular fix.

- `GlobalLocationService.<init>` creates `new Geocoder(todayApplication)`:
  `ev_0b37e0caca41433f447881d3d93dbb6bba603ff6a9049ea53c634908bcc957da`.
- `GlobalLocationService.createPlaceItemFromSystemAddress` removes a leading
  feature name containing `+`, removes the final comma-delimited component outside
  China (Thailand can use a space delimiter), and removes the returned country
  name. With no address line it combines the small and large position fields:
  `ev_9cdbad1151d70124b77d1cbf8a82054e8ef49a23a580b7af630a21cd8a8c4ecf`.
- `GlobalLocationService.formatSmallPosition` uses feature name, house number and
  street; it omits a Plus Code feature and avoids adding number/street already
  contained in the feature name:
  `ev_e5439a11fea68f3eda164e915612f1d0db85d4b9d6f918102eb0eb18bea89ab9`.
- `GlobalLocationService.formatLargePosition` adds sublocality, locality, admin
  area, postcode and country. Singapore locale places the postcode last; country
  code TW suppresses the country name:
  `ev_11b7b6d3de5fa7ddf45d7879712cb28317369cd38368c705b0bfba6c3fa91aad`.
  The obfuscated country constant is confirmed as `TW` by
  `ev_0f8eed760eac3f24f8f04dd654eb0421d850903e84b8a84a540256f5cd4e6713`;
  the Thailand constant is confirmed as `TH` by
  `ev_c775675e37f6af4f18934fe014c4b76cf615a11433372639f3f16fa8d01d12db`
  (field `com.tom_roush.pdfbox.pdmodel.documentinterchange.taggedpdf.i.A`).
- `com.xhey.xcamera.data.pref.d.k0` selects the searched/current place's address
  for the watermark:
  `ev_4330562a30304d1babbb6919732060c6f2abc1e3e5ac1360b3373b6acb747aef`.
- `GlobalLocationService.forceGetAddressFromRemote` and
  `com.xhey.xcamera.location.a.handlePlaceSuccess` demonstrate a configurable remote
  path and server-provided place lists. This does not prove that the user's current
  address came from that path:
  `ev_3cf3639d9c889314fd84188f6cc80c4f6c98f5b54990a63895d45dd601babc2c`,
  `ev_38f361c27b2e15f3ecb2f0e92d0f671c4d23323ce44fbd71b756c85863c01779`.

Full evidence is ignored under
`reference_app/v10.0.316/location-format/rea-evidence.json`.

## Implementation and verification

`LocationAddressFormatter` now owns the system-address conversion. The controller
uses the default geocoder locale and passes formatted text to its existing
callback. It trims edge whitespace and tolerates missing fields. Manual address
entry, camera orientation, and watermark layout are unchanged.

Six unit tests cover the captured Vietnamese text, a leading Plus Code, China and
Thailand branches, missing-line component fallback, postcode placement, TW country
suppression and empty results. A clean debug build and the existing unit suite
passed with JDK 17.

A fresh paired phone comparison was not established. The screen captured during
inspection showed a camera permission dialog over PG Camera despite the window
manager reporting a Timemark transparent activity, so it is not labeled as a
Timemark address screenshot. No permissions were changed or taps performed.

Formatting now follows the recovered system route. Different geocoder databases,
GPS fixes, selected POIs, cached places or remote results can still produce
different street/ward names. This change does not replicate the remote service.
