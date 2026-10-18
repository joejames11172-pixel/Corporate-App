# Mobile app deployment standard

Scope: all existing Android applications, shared app templates, and all future mobile applications in Peostectica.

## Required deployment route
1. Build a testable, signed Android APK for device installation; do not label unsigned or debug artifacts as production releases.
2. Store approved APK artifacts in the application's designated Google Drive deployment folder, preserving version, checksum, build identity, and acceptance records.
3. Publish the APK through that application's Peostectica website, using a verified direct HTTPS download link. HereShift's designated website is https://hereshift.peostectica.com. Other apps must use their own approved app-specific domain/subdomain; do not publish all apps under HereShift's domain.
4. Verify the website URL actually downloads the expected APK on a real Android phone, then verify installation and launch. A Google Drive upload alone does not establish public website publication.
5. Apply Founders' Core Procedure: read objectives/mandatory/forbidden/checklists and previous DA reports; run independent review and required acceptance gates; log evidence and only claim delivery when verified.

## Template requirements
Every new mobile-app project and template must include deployment folder, intended app website, APK signing policy, build version, SHA-256, publication procedure, direct-link validation, installation test, and rollback instructions. Keep Google Drive as the canonical artifact store and do not introduce an Alpine persistent database.


## Dynamic document publishing
- For each app, detect approved publishable Google Play documents and APKs from its designated Drive deployment folders; render the current inventory on the app website without manually editing HTML for each file.
- Provide in-browser preview for supported public documents (such as PDF and accessible HTML), plus original-file downloads. Unsupported formats should have download links and clear labels rather than broken previews.
- Show version, last-updated timestamp, document type, and APK checksum where available; refresh inventory when files are added, updated, or removed, with cache invalidation and graceful empty states.
- **Default deny:** never expose internal Play Console records, signing credentials, private audit reports, personally identifying registration details, or unpublished files merely because they exist in Drive. Require explicit publication approval or a designated public subfolder/manifest.
- Keep Google Drive canonical; use least-privilege read-only access for website publishing, not an open public listing of the entire Drive tree.
- Acceptance: verify new document appears, updated document refreshes, removed document disappears, preview/download works anonymously on a mobile browser, and unapproved private documents remain inaccessible.


## Play Store listing parity and document formats
- Each app's public website is a mobile-friendly preview of its eventual Google Play listing: approved app name, icon, screenshots, short/full description, feature highlights, support contact, privacy policy, and release information.
- Render approved public content as accessible responsive HTML; optionally generate PDFs for downloadable/printable documents. Never make users open DOCX to read a public app description.
- Use a single versioned approved content source/manifest in Google Drive to generate website pages and Play submission assets. Show only assets approved for public display; keep internal submission documents and credentials private.
- Verify content parity against the actual Google Play listing after publication. Website updates do not automatically update the Google Play listing; store listing changes must be submitted through Play Console and reviewed separately.
- For dynamic Drive discovery, detect source changes and regenerate/publish approved HTML/PDF outputs, then test mobile viewing, download, accessibility, and cache refresh.

Status: policy recorded; website integration, individual template adoption, and live deployment remain to be verified.
## Mandatory release-blocking deployment gate — ALL apps
This gate applies to every existing and future Peostectica application, every shared template, every release candidate, and every revision, not only HereShift or Android. No app may be reported as delivered, publicly deployed, or ready for CEO installation until ALL applicable checks have evidence:
1. Identify the approved Google Drive release folder and app-specific public website before building; for Android, confirm the intended `deployments/android` folder. Other platforms use their designated platform folder and appropriate installable package.
2. Build and verify a real installable artifact, its signature, version, SHA-256, and test result; never mistake a GitHub ZIP or unsigned candidate for a user-installable APK.
3. Upload the actual release artifact into the designated Drive folder and independently list/read back the exact file there; keep approved source documents and public export assets linked by version.
4. Generate responsive HTML previews and optional PDFs from approved public store-listing content; keep private internal documentation excluded by default.
5. Confirm the website dynamically displays approved documents and offers the actual package via a tested HTTPS download; a repository commit, Drive upload, or placeholder link is NOT publication.
6. Download from the public website on a real supported device, verify checksum, install, launch, and exercise core functions; log screenshots/test evidence and any remaining blockers.
7. Apply the Founders' Core Procedure, independent DA review, required auditor acceptances and CEO approval; on any failure, return to forbidden/mandatory/objectives and repeat verification.
8. Record timestamped proof of each gate and rollback details. If any check is missing, mark release BLOCKED and do not provide a fabricated or untested link.
Each app template must carry this release gate as a required checklist, and automated release workflows should fail closed when any machine-checkable gate is missing.
