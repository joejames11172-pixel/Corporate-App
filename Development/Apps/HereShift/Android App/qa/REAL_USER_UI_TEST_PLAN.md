# HereShift — Real-user UI testing (development only)

## Objective
Provide an in-app Development Mode screen for human testers, without enabling mock location or purchases merely by entering test mode.

## Required screen
- Visible build/version and Android version; clearly label DEVELOPMENT.
- Guided checklist: Home/navigation, 40 paired display themes and contrast, 20 audio themes and None, volume/preview, coordinates and validation, privacy screen, Back navigation, persistence after app restart.
- Per-task Pass / Fail / Skip and free-text notes, plus a screen-level 1–5 usability rating.
- Report issue: screen, steps to reproduce, expected vs actual, optional screenshot via Android system picker, optional device/app metadata.
- Review and export report via Android Sharesheet only after explicit confirmation. No silent upload, account registration, location history, billing, or analytics SDK.
- Reset local test data with confirmation. All data stored privately on device until explicitly shared.
- Clear separation of UI-only testing from authorized mock-location service testing; no service starts automatically.

## Acceptance gates
1. Implement screen in native Kotlin application; wire navigation from Home.
2. Build debug APK and run emulator tests for navigation, persistence, report export intent, and data deletion.
3. Run accessibility checks for screen reader labels, touch targets, contrast, font scaling, and orientation.
4. Recruit opt-in human testers; do not claim human results until received. Collect explicit consent before sharing feedback.
5. Five independent auditors required for company production acceptance; debug build remains development-only.

## Current state
Design and acceptance plan only. UI implementation, test execution, and human testing are not yet complete.
