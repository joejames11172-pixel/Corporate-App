# HereShift — production release acceptance gates

Status: **NOT APPROVED FOR PUBLIC DEPLOYMENT**. Debug build success is not production approval.

## Current implementation
- Native Android Kotlin application, compileSdk/targetSdk 36, minSdk 26.
- Home, coordinate entry and in-app preview, 40 paired display themes, 20 audio themes plus None, Privacy, and opt-in mock-location service with Stop control.
- In-app Development Mode for testers, local feedback, consented share, and reset.
- Version 0.4.2-dev / versionCode 6 is a development version, not a production release.

## Release-blocking acceptance checklist
- [ ] Build and inspect a **release AAB** from the final reviewed commit.
- [ ] Set final production versionCode and versionName and preserve upgrade compatibility.
- [ ] Secure owner-controlled Play App Signing/upload key. Never commit keys or passwords.
- [ ] Run real-phone installation, upgrade, restart, permission denial, and foreground Stop/cleanup tests.
- [ ] Verify display/audio assets, theme selection, audio off, and persistence on real devices.
- [ ] Accessibility checks: TalkBack, touch targets, text scaling, light/dark contrast, orientation.
- [ ] Security/privacy audit: permissions, no network telemetry, data handling, no location history, user-controlled mock simulation.
- [ ] Validate public privacy-policy URL, Play listing, screenshots, content and data-safety disclosures.
- [ ] Confirm Android/Google Play policies for foreground location service and test-provider behavior.
- [ ] Independent Devil's Advocate report; resolve every criticism.
- [ ] Five independent auditor acceptances per company policy.
- [ ] Explicit owner approval for publication and Play Console release authorization.

## Shipping rule
A successful debug APK is suitable only for authorized development testing.
Do not publish, mark production-ready, or claim a full release until all gates have documented evidence.
