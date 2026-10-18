# Peostectica Android application release playbook
Version: 2026-10-17; source: HereShift release-candidate experience.
Applies to: every Peostectica Android app (currently HereShift and TapVoyage).
This is a reusable template, not evidence that another app has been implemented or approved.

## Company execution order
1. Read each app's objectives, forbidden rules, mandatory requirements, logs and prior Devil's Advocate findings.
2. Confirm feature inventory and identify unfinished user-facing functionality. Do not replace product completion with a build-version change.
3. Preserve previous source, version metadata, and release artifacts; use a feature/release branch and review.
4. Build debug APK for internal diagnosis, but never call it production.
5. Build **non-debuggable release APK and AAB** via CI; verify file existence, checksums, versionCode, versionName, applicationId, permissions and debuggable=false. A release APK may be unsigned and therefore not installable until signed.
6. Test on an Android emulator and at least one physical device; document navigation, accessibility, permission denial, restart, upgrade, battery/service stop and data retention.
7. Audit licensing, security, privacy disclosures, Android/Play policy, content rating, screenshots, support and privacy URLs.
8. Have independent Devil's Advocate review and resolve all issues; collect five independent auditor acceptances per company policy.
9. Keep signing keys in owner-controlled secure storage; never commit them or export them in logs. Generate signed AAB through authorized signing workflow only.
10. Obtain explicit owner publication authorization; upload to Play Console appropriate test track, verify store processing and install, then advance to production only when accepted.
11. Save verified artifacts and evidence to canonical Google Drive; GitHub Actions is only a build mechanism.
12. If any mandatory gate fails, fix and restart Forbidden → Mandatory → objectives/research → execution → DA checks.

## Automation reusable for each app
- Isolate per-app applicationId, display name, assets, permissions and version.
- Use Java 17, Android SDK 36 (unless updated after research), Gradle pinned dependencies.
- Reproduce original/licensed assets deterministically; verify SHA-256 hashes.
- CI assembleDebug, assembleRelease, bundleRelease and upload artifacts.
- Include emulator smoke tests; assert real UI controls using hierarchy and scroll when controls are below fold.
- Distinguish test-script failure from app defect with screenshots, XML and logcat.
- Require release metadata report: SHA, version, debug flag, package ID, asset inventory and audit evidence.
- No signing secret in GitHub source, issue, artifacts, or transcript.

## Learned from HereShift
- A debug APK compiled while release status remained unproven; do not equate compilation with launch approval.
- Tester feedback should autosave and require explicit consent before sharing.
- Testing shortcuts should be direct and avoid inadvertently activating sensitive services.
- Emulator UI trees only contain visible items; scroll before declaring a lower section missing.
- Keep Play publishing separate from building and from release signing.

## App-specific status (verified 2026-10-17)
- HereShift: native Android app source exists; 0.5.0-rc1 release build pipeline initiated, outcome pending at template publication.
- TapVoyage: Android App folder currently contains only .gitkeep; app implementation, objectives and release pipeline must be established before any production build.
- Other future apps: discover from canonical Google Drive objectives before creating or publishing anything.

## Release acceptance record (copy per app)
App: __
Commit: __
Version/applicationId: __
APK/AAB SHA-256: __
Emulator result: __
Physical device result: __
Security/privacy/accessibility reviews: __
Independent DA report: __
Five auditors: __
Owner approval: __
Google Play track and verified URL: __
Status: BLOCKED until all mandatory fields supported by evidence.
