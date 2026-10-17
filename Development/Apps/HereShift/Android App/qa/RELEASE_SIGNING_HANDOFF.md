# HereShift release signing handoff
Status: unsigned release APK and AAB built in GitHub Actions; no production signing key supplied or used.
Version: 0.5.0-rc1 / versionCode 7.
Application ID: com.peostectica.hereshift.

## Owner-controlled signing
1. Choose an existing production upload keystore if this app has ever been published. Reusing the established signing identity is necessary for updates.
2. Otherwise create a new upload keystore on an owner-controlled machine using Android Studio's Generate Signed Bundle / APK workflow; store the keystore and passwords in separate secure backups.
3. Never send private signing keys or passwords through chat, GitHub source, issue comments, or build artifacts.
4. Verify the release AAB's package ID and version; sign using the authorized owner-controlled process.
5. Record only public certificate fingerprints and signed artifact SHA-256 in the release evidence document.
6. Upload the signed AAB to an internal Play Console test track after owner authorization; resolve Play policy and prelaunch report issues before production.
7. Obtain physical device tests, accessibility/security/legal reviews, independent DA, and five auditor acceptances before production rollout.

## Artifact source
GitHub Actions HereShift release candidate workflow: https://github.com/joejames11172-pixel/Corporate-App/actions/runs/39069498869
Artifact is unsigned and cannot be presented as a production-installable APK.
