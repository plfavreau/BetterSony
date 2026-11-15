---
name: release
description: Publish a new BetterSony version as a GitHub release with a signed APK.
---

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`. Commit `chore(release): vX.Y.Z`.
2. `./gradlew assembleRelease`. It must be signed with the release key (`BETTERSONY_*` in `~/.gradle/gradle.properties`), otherwise users can't update over the previous version. Check:
   `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
   SHA-256 digest must be `f4af9350f7eec2c52cfbe3e8d05c485185531b770dde7c03d0033e5a371ee16d`.
3. Push, then:
   ```sh
   cp app/build/outputs/apk/release/app-release.apk BetterSony-X.Y.Z.apk
   gh release create vX.Y.Z BetterSony-X.Y.Z.apk --title "BetterSony X.Y.Z" --notes "<what changed, 3-5 short bullets>"
   ```
4. Install it on a device and open it once before announcing.
