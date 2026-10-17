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

Status: policy recorded; website integration, individual template adoption, and live deployment remain to be verified.